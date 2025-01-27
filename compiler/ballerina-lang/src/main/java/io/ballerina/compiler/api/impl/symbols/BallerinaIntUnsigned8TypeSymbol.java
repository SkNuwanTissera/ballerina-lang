/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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

package io.ballerina.compiler.api.impl.symbols;

import io.ballerina.compiler.api.SymbolTransformer;
import io.ballerina.compiler.api.SymbolVisitor;
import io.ballerina.compiler.api.symbols.IntUnsigned8TypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import org.wso2.ballerinalang.compiler.semantics.model.types.BIntSubType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Names;

import java.util.Optional;

/**
 * Represents the int:Unsigned8 type descriptor.
 *
 * @since 2.0.0
 */
public class BallerinaIntUnsigned8TypeSymbol extends AbstractTypeSymbol implements IntUnsigned8TypeSymbol {

    public BallerinaIntUnsigned8TypeSymbol(CompilerContext context, BIntSubType unsigned8Type) {
        super(context, TypeDescKind.INT_UNSIGNED8, unsigned8Type);
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(Names.STRING_UNSIGNED8);
    }

    @Override
    public String signature() {
        return "int:" + Names.STRING_UNSIGNED8;
    }

    @Override
    public void accept(SymbolVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T apply(SymbolTransformer<T> transformer) {
        return transformer.transform(this);
    }
}
