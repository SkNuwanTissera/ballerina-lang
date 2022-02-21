/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.test.types.future;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.CompileResult;
import org.ballerinalang.test.JvmRunUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.ballerinalang.test.BAssertUtil.validateError;

/**
 * This class contains future type related test cases.
 */
public class FuturePositiveTests {

    private CompileResult result;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/types/future/future_positive.bal");
    }

    @Test
    public void testBasicTypes() {
        JvmRunUtil.invoke(result, "testBasicTypes");
    }

    @Test
    public void testBasicTypesWithoutFutureConstraint() {
        JvmRunUtil.invoke(result, "testBasicTypesWithoutFutureConstraint");
    }

    @Test
    public void testRefTypes() {
        JvmRunUtil.invoke(result, "testRefTypes");
    }

    @Test
    public void testRefTypesWithoutFutureConstraint() {
        JvmRunUtil.invoke(result, "testRefTypesWithoutFutureConstraint");
    }

    @Test
    public void testArrayTypes() {
        JvmRunUtil.invoke(result, "testArrayTypes");
    }

    @Test
    public void testArrayTypesWithoutFutureConstraint() {
        JvmRunUtil.invoke(result, "testArrayTypesWithoutFutureConstraint");
    }

    @Test
    public void testRecordTypes() {
        JvmRunUtil.invoke(result, "testRecordTypes");
    }

    @Test
    public void testRecordTypesWithoutFutureConstraint() {
        JvmRunUtil.invoke(result, "testRecordTypesWithoutFutureConstraint");
    }

    @Test
    public void testObjectTypes() {
        JvmRunUtil.invoke(result, "testObjectTypes");
    }

    @Test
    public void testObjectTypesWithoutFutureConstraint() {
        JvmRunUtil.invoke(result, "testObjectTypesWithoutFutureConstraint");
    }

    @Test
    public void testCustomErrorFuture() {
        JvmRunUtil.invoke(result, "testCustomErrorFuture");
    }

    @Test
    public void testCustomErrorFutureWithoutConstraint() {
        JvmRunUtil.invoke(result, "testCustomErrorFutureWithoutConstraint");
    }

    @Test
    public void testFutureTyping() {
        JvmRunUtil.invoke(result, "testFutureTyping");
    }

    @Test
    public void testNegatives() {
        CompileResult errors = BCompileUtil.compile("test-src/types/future/future_negative_compilation.bal");
        int index = 0;
        validateError(errors, index++, "incompatible types: expected 'future<Bar>', found 'future<Foo>'", 18, 22);
        validateError(errors, index++,
                      "incompatible types: expected 'future<(int|string|error)>', found 'future<(Foo|Bar)>'", 19, 35);
        validateError(errors, index++, "incompatible types: expected 'future<Foo>', found 'future<Bar>'", 21, 22);
        validateError(errors, index++,
                      "incompatible types: expected 'future<(int|string)>', found 'future<(int|string|error)>'",
                      22, 29);
        validateError(errors, index++, "incompatible types: expected 'future<Bar>', found 'future<(any|error)>'",
                      25, 22);
        Assert.assertEquals(errors.getErrorCount(), index);
    }

    @AfterClass
    public void tearDown() {
        result = null;
    }
}
