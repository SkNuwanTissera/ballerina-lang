/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.test.types.table;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.CompileResult;
import org.ballerinalang.test.JvmRunUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Class to test assigning table value to assignable types.
 *
 * @since 2.0.0
 */
public class TableValueAssignTest {
    private CompileResult result;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/types/table/table_value_assign_test.bal");
    }

    @Test(dataProvider = "dataToTestAssignTableValue", description = "Test assigning table value with types")
    public void testAssignTableValue(String functionName) {
        JvmRunUtil.invoke(result, functionName);
    }

    @DataProvider
    public Object[] dataToTestAssignTableValue() {
        return new Object[]{
                "testAssignKeyedTableValueToAnydata",
                "testAssignKeyedTableValueToAny",
                "testAssignKeyedTableValueToVar",
                "testAssignKeyedTableValueToTableType",
                "testAssignKeylessTableValueToAnydata",
                "testAssignKeylessTableValueToAny",
                "testAssignKeylessTableValueToVar",
                "testAssignKeylessTableValueToTableType"
        };
    }
}
