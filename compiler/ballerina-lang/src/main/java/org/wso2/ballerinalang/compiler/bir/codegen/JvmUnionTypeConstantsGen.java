/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ballerinalang.compiler.bir.codegen;

import org.ballerinalang.model.elements.PackageID;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.wso2.ballerinalang.compiler.bir.codegen.split.JvmCreateTypeGen;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

/**
 * Generates Jvm class for the ballerina union types as constants for a given module.
 *
 * @since 2.0.0
 */
public class JvmUnionTypeConstantsGen {

    private final Map<BUnionType, String> unionTypeVarMap;
    private final String unionVarConstantsClass;
    private int constantIndex = 0;
    private JvmCreateTypeGen jvmCreateTypeGen;
    private ClassWriter cw;
    private MethodVisitor mv;
    private int methodCount;
    private final List<String> funcNames;
    private final Queue<TypeNamePair> queue;

    /**
     * Stack keeps track of recursion in union types. The method creation is performed only if recursion is completed.
     */
    public JvmUnionTypeConstantsGen(PackageID packageID) {
        unionTypeVarMap = new ConcurrentSkipListMap<>(this::checkUnionEqualityInInts);
        unionVarConstantsClass = JvmCodeGenUtil.getModuleLevelClassName(
                packageID, JvmConstants.BUNION_TYPE_CONSTANT_CLASS_NAME);
        generateUnionTypeConstantsClassInit();
        visitUnionTypeInitMethod();
        funcNames = new ArrayList<>();
        queue = new LinkedList<>();
    }

    private int checkUnionEqualityInInts(BUnionType o1, BUnionType o2) {
        if (checkUnionsEquality(o1, o2)) {
            return 0;
        }
        return -1;
    }

    private boolean checkUnionsEquality(BUnionType o1, BUnionType o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1.getMemberTypes().size() != o2.getMemberTypes().size() || !o1.toString().equals(o2.toString())) {
            return false;
        }
        for (BType type : o1.getMemberTypes()) {
            if (!o2.getMemberTypes().contains(type)) {
                return false;
            }
        }
        return o1.flags == o2.flags;
    }

    public synchronized void setJvmCreateTypeGen(JvmCreateTypeGen jvmCreateTypeGen) {
        this.jvmCreateTypeGen = jvmCreateTypeGen;
    }

    public synchronized String add(BUnionType type) {
        return unionTypeVarMap.computeIfAbsent(type, str -> generateBUnionInits(type));
    }

    private void generateUnionTypeConstantsClassInit() {
        cw = new BallerinaClassWriter(COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, unionVarConstantsClass, null, JvmConstants.OBJECT, null);

        MethodVisitor methodVisitor = cw.visitMethod(ACC_PRIVATE, JvmConstants.JVM_INIT_METHOD, "()V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, JvmConstants.OBJECT, JvmConstants.JVM_INIT_METHOD, "()V", false);
        genMethodReturn(methodVisitor);
    }

    private void visitUnionTypeInitMethod() {
        mv = cw.visitMethod(ACC_STATIC, JvmConstants.B_UNION_TYPE_INIT_METHOD_PREFIX + methodCount++,
                            "()V", null, null);
    }

    private String generateBUnionInits(BUnionType type) {
        String varName = JvmConstants.UNION_TYPE_VAR_PREFIX + constantIndex++;
        visitBUnionField(varName);
        createBunionType(mv, type, varName);
        // Queue is used here to avoid recursive calls to the genPopulateMethod. This can happen when a union
        // contains a union inside it.
        queue.add(new TypeNamePair(type, varName));
        if (queue.size() == 1) {
            genPopulateMethod(type, varName);
            queue.remove();
            while (!queue.isEmpty()) {
                TypeNamePair typeNamePair = queue.remove();
                genPopulateMethod(typeNamePair.type, typeNamePair.varName);
            }
        }
        return varName;
    }

    private void genPopulateMethod(BUnionType type, String varName) {
        String methodName = String.format("$populate%s", varName);
        funcNames.add(methodName);
        MethodVisitor methodVisitor = cw.visitMethod(ACC_STATIC, methodName, "()V", null, null);
        methodVisitor.visitCode();
        generateGetBUnionType(methodVisitor, varName);
        jvmCreateTypeGen.populateUnion(cw, methodVisitor, type, unionVarConstantsClass, varName);
        genMethodReturn(methodVisitor);
    }

    private void createBunionType(MethodVisitor mv, BUnionType unionType, String varName) {
        jvmCreateTypeGen.createUnionType(mv, unionType);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, unionVarConstantsClass, varName,
                          String.format("L%s;", JvmConstants.UNION_TYPE_IMPL));
    }

    private void visitBUnionField(String varName) {
        FieldVisitor fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, varName,
                                        String.format("L%s;", JvmConstants.UNION_TYPE_IMPL), null, null);
        fv.visitEnd();
    }

    public void generateGetBUnionType(MethodVisitor mv, String varName) {
        mv.visitFieldInsn(GETSTATIC, unionVarConstantsClass, varName,
                          String.format("L%s;", JvmConstants.UNION_TYPE_IMPL));
    }

    public synchronized void generateClass(Map<String, byte[]> jarEntries) {
        genMethodReturn(mv);
        visitUnionTypeInitMethod();
        for (String funcName : funcNames) {
            mv.visitMethodInsn(INVOKESTATIC, unionVarConstantsClass, funcName, "()V", false);
        }
        genMethodReturn(mv);
        generateStaticInitializer(cw);
        cw.visitEnd();
        jarEntries.put(unionVarConstantsClass + ".class", cw.toByteArray());
    }

    private void generateStaticInitializer(ClassWriter cw) {
        MethodVisitor methodVisitor = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        for (int i = 0; i < methodCount; i++) {
            methodVisitor.visitMethodInsn(INVOKESTATIC, unionVarConstantsClass,
                                          JvmConstants.B_UNION_TYPE_INIT_METHOD_PREFIX + i,
                                          "()V", false);
        }
        genMethodReturn(methodVisitor);
    }

    private void genMethodReturn(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private static class TypeNamePair {
        BUnionType type;
        String varName;

        private TypeNamePair(BUnionType type, String varName) {
            this.type = type;
            this.varName = varName;
        }
    }

}
