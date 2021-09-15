/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.ballerinalang.compiler.tree.statements;

import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.VariableNode;
import org.ballerinalang.model.tree.statements.VariableDefinitionNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeAnalyzer;
import org.wso2.ballerinalang.compiler.tree.BLangNodeModifier;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangTupleVariable;

/**
 * [int, string] [i, s] = [5, "foo"];.
 *
 * @since 0.985.0
 */
public class BLangTupleVariableDef extends BLangStatement implements VariableDefinitionNode {

    // BLangNodes
    public BLangTupleVariable var;

    @Override
    public BLangTupleVariable getVariable() {
        return var;
    }

    @Override
    public void accept(BLangNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> void accept(BLangNodeAnalyzer<T> analyzer, T props) {
        analyzer.visit(this, props);
    }

    @Override
    public <T, R> R apply(BLangNodeModifier<T, R> modifier, T props) {
        return modifier.modify(this, props);
    }

    @Override
    public void setVariable(VariableNode var) {
        this.var = (BLangTupleVariable) var;
    }

    @Override
    public boolean getIsInFork() {
        return false;
    }

    @Override
    public boolean getIsWorker() {
        return false;
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.VARIABLE_DEF;
    }

    @Override
    public String toString() {
        return this.var.toString();
    }
}
