/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.transform.process.function;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Function;

import java.math.BigDecimal;

/**
 * SqrtFunction
 * description: sqrt(numeric)--returns the square root of numeric
 */
public class SqrtFunction implements ValueParser {

    private ValueParser number;

    /**
     * Constructor
     * @param expr
     */
    public SqrtFunction(Function expr) {
        number = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
    }

    /**
     * parse
     * @param sourceData
     * @param rowIndex
     * @return
     */
    @Override
    public Object parse(SourceData sourceData, int rowIndex) {
        Object numberObj = number.parse(sourceData, rowIndex);
        BigDecimal numberValue = OperatorTools.parseBigDecimal(numberObj);
        return Math.sqrt(numberValue.doubleValue());
    }
}
