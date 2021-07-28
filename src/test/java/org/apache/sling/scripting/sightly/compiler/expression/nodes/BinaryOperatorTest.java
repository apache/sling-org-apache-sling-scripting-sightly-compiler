/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.testobjects.TestEnum;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class BinaryOperatorTest {

    @RunWith(Parameterized.class)
    public static class StrictEq {
        @Parameters(name = "Comparison: {3}")
        public static Iterable<? extends Object> data() {
            List<Object[]> list = new ArrayList<>();
            list.add(new Object[] {TestEnum.ONE, TestEnum.ONE.name(), true, "enum to string are equal"});
            list.add(new Object[] {TestEnum.ONE, TestEnum.TWO.name(), false, "enum to string not equal"});
            list.add(new Object[] {TestEnum.ONE.name(), TestEnum.ONE, true,  "string to enum are equal"});
            list.add(new Object[] {TestEnum.ONE.name(), TestEnum.TWO, false,  "string to enum not equal"});
            /**
             * SLING-10682 verify compare enum to another enum
             */
            list.add(new Object[] {TestEnum.ONE.name(), TestEnum.ONE, true, "enum to enum are equal"});
            list.add(new Object[] {TestEnum.ONE.name(), TestEnum.TWO, false, "enum to enum not equal"});

            return list;
        }

        private Object left;
        private Object right;
        private boolean expectedOutcome;

        public StrictEq(Object left, Object right, boolean expectedOutcome, String testLabel) {
            super();
            this.left = left;
            this.right = right;
            this.expectedOutcome = expectedOutcome;
        }

        @Test
        public void testStrictEq() {
            assertEquals(expectedOutcome, BinaryOperator.strictEq(left, right));
        }

    }

    public static class StrictEqError {

        /**
         * Expect exception when passed the wrong kind of object
         */
        @Test(expected = SightlyCompilerException.class)
        public void testStrictEqWrongType() {
            BinaryOperator.strictEq(new Object(), new Object());
        }

    }

}
