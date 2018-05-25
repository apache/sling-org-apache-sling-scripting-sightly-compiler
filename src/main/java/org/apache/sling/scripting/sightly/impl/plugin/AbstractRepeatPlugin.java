/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.sightly.impl.plugin;

import java.util.HashMap;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperator;

public abstract class AbstractRepeatPlugin extends AbstractPlugin {

    protected static final String INDEX = "index";
    protected static final String COUNT = "count";
    protected static final String FIRST = "first";
    protected static final String MIDDLE = "middle";
    protected static final String LAST = "last";
    protected static final String ODD = "odd";
    protected static final String EVEN = "even";
    protected static final String BEGIN = "begin";
    protected static final String STEP = "step";
    protected static final String END = "end";

    protected MapLiteral buildStatusObj(String indexVar, String sizeVar) {
        HashMap<String, ExpressionNode> obj = new HashMap<>();
        Identifier indexId = new Identifier(indexVar);
        BinaryOperation firstExpr = new BinaryOperation(BinaryOperator.EQ, indexId, NumericConstant.ZERO);
        BinaryOperation lastExpr = new BinaryOperation(
                BinaryOperator.EQ,
                indexId,
                new BinaryOperation(BinaryOperator.SUB, new Identifier(sizeVar), NumericConstant.ONE));
        obj.put(INDEX, indexId);
        obj.put(COUNT, new BinaryOperation(BinaryOperator.ADD, indexId, NumericConstant.ONE));
        obj.put(FIRST, firstExpr);
        obj.put(MIDDLE, new UnaryOperation(
                UnaryOperator.NOT,
                new BinaryOperation(BinaryOperator.OR, firstExpr, lastExpr)));
        obj.put(LAST, lastExpr);
        obj.put(ODD, parityCheck(indexId, NumericConstant.ZERO));
        obj.put(EVEN, parityCheck(indexId, NumericConstant.ONE));
        return new MapLiteral(obj);
    }

    private ExpressionNode parityCheck(ExpressionNode numericExpression, NumericConstant expected) {
        return new BinaryOperation(
                BinaryOperator.EQ,
                new BinaryOperation(BinaryOperator.REM, numericExpression, NumericConstant.TWO),
                expected);
    }
}
