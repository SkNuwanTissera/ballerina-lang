/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerina.semtype;

/**
 * SemType Interface.
 */
interface SemType {

}

/**
 * Complex SemType implementation.
 */
class ComplexSemType implements SemType {
    UniformTypeBitSet all;
    UniformTypeBitSet some;

}

/**
 * UniformTypeBitSet SemType implementation.
 */
class UniformTypeBitSet implements SemType {
    int value;

    public UniformTypeBitSet(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
