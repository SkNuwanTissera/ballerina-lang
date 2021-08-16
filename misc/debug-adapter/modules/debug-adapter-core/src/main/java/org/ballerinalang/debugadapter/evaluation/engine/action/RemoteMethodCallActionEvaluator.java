/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.debugadapter.evaluation.engine.action;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import org.ballerinalang.debugadapter.SuspendedContext;
import org.ballerinalang.debugadapter.evaluation.BExpressionValue;
import org.ballerinalang.debugadapter.evaluation.EvaluationException;
import org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind;
import org.ballerinalang.debugadapter.evaluation.engine.ClassDefinitionResolver;
import org.ballerinalang.debugadapter.evaluation.engine.Evaluator;
import org.ballerinalang.debugadapter.evaluation.engine.SymbolBasedArgProcessor;
import org.ballerinalang.debugadapter.evaluation.engine.expression.MethodCallExpressionEvaluator;
import org.ballerinalang.debugadapter.evaluation.engine.invokable.GeneratedInstanceMethod;
import org.ballerinalang.debugadapter.variable.BVariable;
import org.ballerinalang.debugadapter.variable.BVariableType;
import org.ballerinalang.debugadapter.variable.VariableFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluator implementation for remote method call invocation actions.
 *
 * @since 2.0.0
 */
public class RemoteMethodCallActionEvaluator extends MethodCallExpressionEvaluator {

    private final RemoteMethodCallActionNode syntaxNode;
    private final String methodName;
    private final Evaluator objectExpressionEvaluator;
    private final List<Map.Entry<String, Evaluator>> argEvaluators;

    public RemoteMethodCallActionEvaluator(SuspendedContext context, RemoteMethodCallActionNode remoteMethodActionNode,
                                           Evaluator expression, List<Map.Entry<String, Evaluator>> argEvaluators) {
        super(context, null, expression, argEvaluators);
        this.syntaxNode = remoteMethodActionNode;
        this.objectExpressionEvaluator = expression;
        this.argEvaluators = argEvaluators;
        this.methodName = syntaxNode.methodName().toSourceCode().trim();
    }

    @Override
    public BExpressionValue evaluate() throws EvaluationException {
        try {
            // Calls a remote method of a client object. This works the same as a method call expression, except that
            // it is used for a client object method with the remote qualifier.
            BExpressionValue result = objectExpressionEvaluator.evaluate();
            BVariable resultVar = VariableFactory.getVariable(context, result.getJdiValue());

            // If the expression result is an object, try invoking as an object method invocation.
            if (result.getType() != BVariableType.OBJECT) {
                throw new EvaluationException(String.format(EvaluationExceptionKind.CUSTOM_ERROR.getString(),
                        "invalid remote method call: expected a client object, but found 'other'"));
            }

            Value invocationResult = invokeRemoteMethod(resultVar);
            return new BExpressionValue(context, invocationResult);
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(String.format(EvaluationExceptionKind.INTERNAL_ERROR.getString(),
                    syntaxNode.toSourceCode().trim()));
        }
    }

    private Value invokeRemoteMethod(BVariable resultVar) throws EvaluationException {

        ClassDefinitionResolver classDefResolver = new ClassDefinitionResolver(context);
        String className = resultVar.getDapVariable().getValue();
        Optional<ClassSymbol> classDef = classDefResolver.findBalClassDefWithinModule(className);
        if (classDef.isEmpty()) {
            // Resolves the JNI signature to see if the the object/class is defined with a dependency module.
            String signature = resultVar.getJvmValue().type().signature();
            if (!signature.startsWith(QUALIFIED_TYPE_SIGNATURE_PREFIX)) {
                throw new EvaluationException(String.format(EvaluationExceptionKind.CLASS_NOT_FOUND.getString(),
                        className));
            }

            String[] signatureParts = signature.substring(1).split(JNI_SIGNATURE_SEPARATOR);
            if (signatureParts.length < 2) {
                throw new EvaluationException(String.format(EvaluationExceptionKind.CLASS_NOT_FOUND.getString(),
                        className));
            }
            String orgName = signatureParts[0];
            String packageName = signatureParts[1];
            classDef = classDefResolver.findBalClassDefWithinDependencies(orgName, packageName, className);
        }

        if (classDef.isEmpty()) {
            throw new EvaluationException(String.format(EvaluationExceptionKind.CLASS_NOT_FOUND.getString(),
                    className));
        }

        Optional<MethodSymbol> objectMethodDef = findObjectMethodInClass(classDef.get(), methodName);
        if (objectMethodDef.isEmpty()) {
            throw new EvaluationException(String.format(EvaluationExceptionKind.REMOTE_METHOD_NOT_FOUND.getString(),
                    syntaxNode.methodName().toString().trim(), className));
        }

        GeneratedInstanceMethod objectMethod = getRemoteMethodByName(resultVar, objectMethodDef.get());
        List<Value> orderedArgsList = new SymbolBasedArgProcessor(context, methodName, objectMethod.getJDIMethodRef(),
                objectMethodDef.get()).process(argEvaluators);
        objectMethod.setArgValues(orderedArgsList);
        return objectMethod.invokeSafely();
    }

    private GeneratedInstanceMethod getRemoteMethodByName(BVariable objectVar, MethodSymbol methodDefinition)
            throws EvaluationException {
        try {
            ReferenceType objectRef = ((ObjectReference) objectVar.getJvmValue()).referenceType();
            int argsCountInDefinition = methodDefinition.typeDescriptor().params().get().size() +
                    (methodDefinition.typeDescriptor().restParam().isPresent() ? 1 : 0);

            List<Method> methods = objectRef.methodsByName(methodName);
            for (Method method : methods) {
                // total number of parameters defined in the runtime method will be n + 1, due to the 'strand' variable.
                int expectedArgsCountInRuntime = argsCountInDefinition + 1;
                if (method.argumentTypes().size() == expectedArgsCountInRuntime) {
                    return new GeneratedInstanceMethod(context, objectVar.getJvmValue(), methods.get(0));
                }
            }
            throw new EvaluationException(String.format(EvaluationExceptionKind.REMOTE_METHOD_NOT_FOUND.getString(),
                    syntaxNode.methodName().toString().trim(), objectVar.computeValue()));
        } catch (ClassNotLoadedException e) {
            throw new EvaluationException(String.format(EvaluationExceptionKind.REMOTE_METHOD_NOT_FOUND.getString(),
                    syntaxNode.methodName().toString().trim(), objectVar.computeValue()));
        }
    }
}
