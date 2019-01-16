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
package org.apache.sling.scripting.sightly.impl.plugin;

import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.Loop;
import org.apache.sling.scripting.sightly.compiler.commands.OutText;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;

public class RepeatPlugin extends AbstractRepeatPlugin {

    private static final OutText NEW_LINE = new OutText("\n");

    public RepeatPlugin() {
        name = "repeat";
        priority = 130;
    }

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            private String listVariable = compilerContext.generateVariable("collectionVar");
            private String collectionSizeVar = compilerContext.generateVariable("size");
            private String collectionNotEmpty = compilerContext.generateVariable("notEmpty");
            private String beginVariable = compilerContext.generateVariable(BEGIN);
            private String stepVariable = compilerContext.generateVariable(STEP);
            private String endVariable = compilerContext.generateVariable(END);
            private String validStartStepEnd = compilerContext.generateVariable("validStartStepEnd");

            private boolean beginAtIndexZero = false;
            private boolean stepOne = false;

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                stream.write(new VariableBinding.Start(listVariable, expression.getRoot()));
                stream.write(new VariableBinding.Start(collectionSizeVar,
                        new UnaryOperation(UnaryOperator.LENGTH, new Identifier(listVariable))));
                stream.write(new VariableBinding.Start(collectionNotEmpty, new BinaryOperation(BinaryOperator.GT, new Identifier
                        (collectionSizeVar), NumericConstant.ZERO)));
                stream.write(new Conditional.Start(collectionNotEmpty, true));
                Map<String, ExpressionNode> options = expression.getOptions();
                if (options.containsKey(BEGIN)) {
                    stream.write(new VariableBinding.Start(beginVariable, expression.getOptions().get(BEGIN)));
                } else {
                    beginAtIndexZero = true;
                    stream.write(new VariableBinding.Start(beginVariable, NumericConstant.ZERO));
                }
                if (options.containsKey(STEP)) {
                    stream.write(new VariableBinding.Start(stepVariable, expression.getOptions().get(STEP)));
                } else {
                    stepOne = true;
                    stream.write(new VariableBinding.Start(stepVariable, NumericConstant.ONE));
                }
                if (options.containsKey(END)) {
                    stream.write(new VariableBinding.Start(endVariable, expression.getOptions().get(END)));
                } else {
                    stream.write(new VariableBinding.Start(endVariable, new Identifier(collectionSizeVar)));
                }
                stream.write(new VariableBinding.Start(validStartStepEnd,
                                new BinaryOperation(BinaryOperator.AND,
                                        new BinaryOperation(BinaryOperator.AND,
                                                new BinaryOperation(BinaryOperator.LT, new Identifier(beginVariable), new Identifier(collectionSizeVar)),
                                                new BinaryOperation(
                                                        BinaryOperator.AND,
                                                        new BinaryOperation(BinaryOperator.GEQ, new Identifier(beginVariable), NumericConstant.ZERO),
                                                        new BinaryOperation(BinaryOperator.GT, new Identifier(stepVariable), NumericConstant.ZERO)
                                                )
                                        ),
                                        new BinaryOperation(BinaryOperator.GT, new Identifier(endVariable), NumericConstant.ZERO)
                                )
                        )
                );
                stream.write(new Conditional.Start(validStartStepEnd, true));
                String itemVariable = decodeItemVariable();
                String loopStatusVar = Syntax.itemLoopStatusVariable(itemVariable);
                String indexVariable = compilerContext.generateVariable("index");
                stream.write(new Loop.Start(listVariable, itemVariable, indexVariable));
                stream.write(new VariableBinding.Start(loopStatusVar, buildStatusObj(indexVariable, collectionSizeVar)));
                String stepConditionVariable = compilerContext.generateVariable("stepCondition");
                stream.write(new VariableBinding.Start(stepConditionVariable,
                                beginAtIndexZero && stepOne ? new NumericConstant(0) :
                                new BinaryOperation(
                                        BinaryOperator.REM,
                                        new BinaryOperation(
                                                BinaryOperator.SUB,
                                                new Identifier(indexVariable),
                                                new Identifier(beginVariable)
                                        ),
                                        new Identifier(stepVariable))
                        )
                );
                String loopTraversalVariable = compilerContext.generateVariable("traversal");
                stream.write(new VariableBinding.Start(loopTraversalVariable,
                                new BinaryOperation(
                                        BinaryOperator.AND,
                                        new BinaryOperation(
                                                BinaryOperator.AND,
                                                new BinaryOperation(BinaryOperator.GEQ, new Identifier(indexVariable), new Identifier(
                                                        beginVariable)),
                                                new BinaryOperation(BinaryOperator.LEQ, new Identifier(indexVariable), new Identifier(endVariable))
                                        ),
                                        new BinaryOperation(BinaryOperator.EQ, new Identifier(stepConditionVariable), NumericConstant.ZERO)
                                )
                        )
                );
                stream.write(new Conditional.Start(loopTraversalVariable, true));

            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                stream.write(NEW_LINE);
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.write(Conditional.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(Loop.END);
                stream.write(Conditional.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(Conditional.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
            }


            private String decodeItemVariable() {
                String[] args = callInfo.getArguments();
                if (args.length > 0) {
                    return args[0];
                }
                return Syntax.DEFAULT_LIST_ITEM_VAR_NAME;
            }

            private ExpressionNode parityCheck(ExpressionNode numericExpression, NumericConstant expected) {
                return new BinaryOperation(
                        BinaryOperator.EQ,
                        new BinaryOperation(BinaryOperator.REM, numericExpression, NumericConstant.TWO),
                        expected);
            }
        };
    }
}
