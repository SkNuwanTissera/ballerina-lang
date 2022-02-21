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
package org.ballerinalang.test.types.tuples;

import io.ballerina.runtime.api.values.BArray;
import org.ballerinalang.test.BAssertUtil;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.CompileResult;
import org.ballerinalang.test.JvmRunUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Basic Test cases for tuples.
 *
 * @since 0.966.0
 */
public class BasicTupleTest {

    private CompileResult result;
    private CompileResult resultNegative;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/types/tuples/tuple_basic_test.bal");
        resultNegative = BCompileUtil.compile("test-src/types/tuples/tuple_negative_test.bal");
    }

    @Test(description = "Test basics of tuple types")
    public void testTupleTypeBasics() {
        Object returns = JvmRunUtil.invoke(result, "basicTupleTest", new Object[]{});

        Assert.assertEquals(returns.toString(), " test1 expr \n" +
                " test2 \n" +
                " test3 3 \n" +
                " test4 4 \n" +
                " test5 foo test5 \n ");
    }

    @Test(description = "Test Function invocation using tuples")
    public void testFunctionInvocationUsingTuples() {
        Object returns = JvmRunUtil.invoke(result, "testFunctionInvocation", new Object[]{});

        Assert.assertEquals(returns.toString(), "xy5.0z");
    }

    @Test(description = "Test Function Invocation return values using tuples")
    public void testFunctionReturnValue() {
        Object returns = JvmRunUtil.invoke(result, "testFunctionReturnValue", new Object[]{});

        Assert.assertEquals(returns.toString(), "x5.0z");

        BArray returns1 = (BArray) JvmRunUtil.invoke(result, "testFunctionReturnValue2", new Object[]{});
        Assert.assertEquals(returns1.size(), 2);
        Assert.assertEquals(returns1.get(0).toString(), "xz");
        Assert.assertEquals(returns1.get(1).toString(), "5.0");
    }

    @Test(description = "Test Function Invocation return values using tuples")
    public void testIgnoredValue() {
        Object returns = JvmRunUtil.invoke(result, "testIgnoredValue1");

        Assert.assertEquals(returns.toString(), "foo");

        returns = JvmRunUtil.invoke(result, "testIgnoredValue2");

        Assert.assertEquals(returns.toString(), "foo");

        returns = JvmRunUtil.invoke(result, "testIgnoredValue3");

        Assert.assertEquals(returns.toString(), "foo");

        BArray returns1 = (BArray) JvmRunUtil.invoke(result, "testIgnoredValue4");
        Assert.assertEquals(returns1.size(), 2);
        Assert.assertEquals(returns1.get(0).toString(), "foo");
        Assert.assertTrue((Boolean) returns1.get(1));
    }

    @Test(description = "Test index based access of tuple type")
    public void testIndexBasedAccess() {
        BArray returns = (BArray) JvmRunUtil.invoke(result, "testIndexBasedAccess");
        Assert.assertEquals(returns.get(0).toString(), "def");
        Assert.assertEquals(returns.get(1), 4L);
        Assert.assertFalse((Boolean) returns.get(2));
    }

    @Test(description = "Test index based access of tuple type with records")
    public void testIndexBasedAccessOfRecords() {
        BArray returns = (BArray) JvmRunUtil.invoke(result, "testIndexBasedAccessOfRecords");
        Assert.assertEquals(returns.get(0).toString(), "NewFoo");
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertEquals(returns.get(2).toString(), "NewBar");
        Assert.assertEquals(returns.get(3).toString(), "Foo");
        Assert.assertEquals(returns.get(4), 15.5);
    }

    @Test(description = "Test default values for tuple type")
    public void testDefaultValuesInTuples() {
        BArray returns = (BArray) JvmRunUtil.invoke(result, "testDefaultValuesInTuples");
        Assert.assertEquals(returns.get(0).toString(), "");
        Assert.assertEquals(returns.get(1), 0L);
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertEquals(returns.get(3), 0.0);
    }

    @Test(description = "Test tuple to array assignment")
    public void testTupleToArrayAssignment() {
        BArray returns = (BArray) JvmRunUtil.invoke(result, "testTupleToArrayAssignment1", new Object[]{});
        Assert.assertEquals(returns.size(), 3);
        Assert.assertEquals(returns.get(0).toString(), "a");
        Assert.assertEquals(returns.get(1).toString(), "b");
        Assert.assertEquals(returns.get(2).toString(), "c");

        returns = (BArray) JvmRunUtil.invoke(result, "testTupleToArrayAssignment2", new Object[]{});
        Assert.assertEquals(returns.size(), 3);
        Assert.assertEquals(returns.get(0).toString(), "a");
        Assert.assertEquals(returns.get(1).toString(), "b");
        Assert.assertEquals(returns.get(2).toString(), "c");
    }

    @Test(description = "Test tuple to JSON assignment")
    public void testTupleToJSONAssignment() {
        JvmRunUtil.invoke(result, "testTupleToJSONAssignment");
    }

    @Test(description = "Test array to tuple assignment")
    public void testArrayToTupleAssignment() {
        Object returns = JvmRunUtil.invoke(result, "testArrayToTupleAssignment1", new Object[]{});

        Assert.assertEquals(returns.toString(), "[\"a\",\"b\",\"c\"]");

        returns = JvmRunUtil.invoke(result, "testArrayToTupleAssignment2", new Object[]{});

        Assert.assertEquals(returns.toString(), "[\"a\",\"b\",\"c\"]");

        BArray returns1 = (BArray) JvmRunUtil.invoke(result, "testArrayToTupleAssignment3", new Object[]{});
        Assert.assertEquals(returns1.size(), 2);
        Assert.assertEquals(returns1.get(0).toString(), "a");
        Assert.assertEquals(returns1.get(1).toString(), "[\"b\",\"c\"]");

        returns = JvmRunUtil.invoke(result, "testArrayToTupleAssignment4", new Object[]{});

        Assert.assertEquals(returns.toString(), "[\"a\",\"b\",\"c\"]");
    }

    @Test(description = "Test union expected type for list constructor")
    public void testTupleUnionExpectedType() {
        JvmRunUtil.invoke(result, "testTupleUnionExpectedType");
    }

    @Test
    public void testUnionRestDescriptor() {
        JvmRunUtil.invoke(result, "testUnionRestDescriptor");
    }

    @Test
    public void testAnonRecordsInTupleTypeDescriptor() {
        JvmRunUtil.invoke(result, "testAnonRecordsInTupleTypeDescriptor");
    }

    @Test
    public void testTupleWithUnion() {
        JvmRunUtil.invoke(result, "testTupleWithUnion");
    }

    @Test(description = "Test negative scenarios of assigning tuple literals")
    public void testNegativeTupleLiteralAssignments() {
        Assert.assertEquals(resultNegative.getErrorCount(), 40);
        int i = 0;
        BAssertUtil.validateError(
                resultNegative, i++, "tuple and expression size does not match", 18, 32);
        BAssertUtil.validateError(
                resultNegative, i++, "tuple and expression size does not match", 19, 41);
        BAssertUtil.validateError(
                resultNegative, i++, "ambiguous type '([int,boolean,string]|[any,boolean,string])?'", 34, 63);
        BAssertUtil.validateError(
                resultNegative, i, "ambiguous type '([Person,int]|[Employee,int])?'", 38, 47);
    }

    @Test(description = "Test negative scenarios of assigning tuples and arrays")
    public void testNegativeTupleArrayAssignments() {
        int i = 4;
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected 'int[]', found '[string...]'", 43, 15);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected 'string[2]', found '[string...]'", 49, 19);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected 'string[3]', found '[string,string]'", 55, 19);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int...]', found 'string[]'", 61, 18);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,string...]', found '(int|string)[]'", 67, 26);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int]', found 'int[3]'", 73, 20);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int,int]', found 'int[2]'", 79, 25);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int,int...]', found 'int[]'", 91, 28);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int,int]', found 'int[]'", 93, 25);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int,int...]', found 'int[1]'", 96, 28);
        BAssertUtil.validateError(
                resultNegative, i++, "incompatible types: expected '[int,int]', found 'int[1]'", 98, 20);
        BAssertUtil.validateError(
                resultNegative, i, "incompatible types: expected '[int,int]', found 'int[3]'", 101, 20);
    }

    @Test(description = "Test negatives of index based access of tuple type")
    public void testNegativesOfTupleType() {
        int i = 16;
        BAssertUtil.validateError(resultNegative, i++, "tuple and expression size does not match", 114, 38);
        BAssertUtil.validateError(resultNegative, i++, "list index out of range: index: '-1'", 119, 16);
        BAssertUtil.validateError(resultNegative, i++, "list index out of range: index: '3'", 120, 16);
        BAssertUtil.validateError(resultNegative, i++, "incompatible types: expected 'int', found 'string'", 122, 16);
        BAssertUtil.validateError(resultNegative, i++, "incompatible types: expected 'int', found 'string'", 128, 24);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected '(string|boolean|int)', found 'float'", 134, 20);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected 'string', found '(string|boolean|int)'", 135, 16);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected '(string|boolean)', found '(string|boolean|int)'", 136, 24);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected 'int', found 'FiniteOne'", 154, 19);
        BAssertUtil.validateError(resultNegative, i++,
                "invalid list member access expression: value space 'FiniteTwo' out of range", 155, 19);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected 'int', found 'FiniteThree'", 156, 19);
        BAssertUtil.validateError(resultNegative, i++,
                "incompatible types: expected 'int', found 'FiniteFour'", 157, 19);
        BAssertUtil.validateError(resultNegative, i++,
                "invalid list member access expression: value space 'FiniteFive' " +
                        "out of range", 158, 19);
        BAssertUtil.validateError(resultNegative, i, "list index out of range: index: '-1'", 165, 19);
    }

    @Test(description = "Test negative scenarios of assigning to wild card binding pattern")
    public void testNegativeWildCardBindingPatternAssignability() {
        int i = 33;
        BAssertUtil.validateError(
                resultNegative, i++, "a wildcard binding pattern can be used only with a value "
                        + "that belong to type 'any'", 187, 1);
        BAssertUtil.validateError(
                resultNegative, i++, "a wildcard binding pattern can be used only with a value "
                        + "that belong to type 'any'", 190, 5);
        BAssertUtil.validateError(
                resultNegative, i++, "a wildcard binding pattern can be used only with a value "
                        + "that belong to type 'any'", 193, 5);
    }

    @Test(dataProvider = "dataToTestTupleDeclaredWithVar", description = "Test tuple declared with var")
    public void testModuleLevelTupleVarDecl(String functionName) {
        JvmRunUtil.invoke(result, functionName);
    }

    @DataProvider
    public Object[] dataToTestTupleDeclaredWithVar() {
        return new Object[]{
                "testTupleDeclaredWithVar1",
                "testTupleDeclaredWithVar2",
                "testTupleDeclaredWithVar3",
                "testTupleDeclaredWithVar4"
        };
    }

    @Test(description = "Test invalid var declaration with tuples")
    public void testInvalidTupleDeclaredWithVar() {
        int i = 30;
        BAssertUtil.validateError(resultNegative, i++, "invalid list binding pattern; " +
                "member variable count mismatch with member type count", 172, 5);
        BAssertUtil.validateError(resultNegative, i++, "invalid list binding pattern; member variable " +
                "count mismatch with member type count", 173, 5);
        BAssertUtil.validateError(resultNegative, i, "invalid list binding pattern; member variable " +
                "count mismatch with member type count", 174, 5);
    }

    @Test(description = "Test invalid tuple assignments to JSON")
    public void testTupleToJSONAssignmentNegative() {
        int i = 36;
        BAssertUtil.validateError(resultNegative, i++, "incompatible types: expected 'json', " +
                "found '[string,int,xml...]'", 199, 21);
        BAssertUtil.validateError(resultNegative, i++, "incompatible types: '[string,(int|xml),string...]' " +
                "cannot be cast to 'json[]'", 202, 16);
        BAssertUtil.validateError(resultNegative, i, "incompatible types: expected 'json', " +
                "found '[string,(int|xml),string...]'", 203, 16);
    }

    @Test(description = "Test ambiguous tuple assignment to unions of tuple types")
    public void testAmbiguousTupleTupeNegative() {
        int i = 39;
        BAssertUtil.validateError(resultNegative, i, "ambiguous type '([1,\"hello\"]|[1])'", 208, 10);
    }

    @Test
    public void testTupleAsTupleFirstMember() {
        JvmRunUtil.invoke(result, "testTupleAsTupleFirstMember");
    }

    @AfterClass
    public void tearDown() {
        result = null;
        resultNegative = null;
    }
}
