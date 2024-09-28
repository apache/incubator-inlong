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
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import com.alibaba.fastjson.JSON;
import net.sf.jsqlparser.expression.Function;
/**
 * JsonQuoteFunction
 * description: JSON_QUOTE(string)--Quotes a string as a JSON value by wrapping it with double quote characters,
 *              escaping interior quote and special characters (’"’, ‘', ‘/’, ‘b’, ‘f’, ’n’, ‘r’, ’t’), and returning
 *              the result as a string. If the argument is NULL, the function returns NULL.
 * for example: json_quote('Hello, World!')--return "Hello, World!"
 *              json_quote('Complex string with / and \\')--return "Complex string with / and \\"
 */
@TransformFunction(names = {"json_quote"})
public class JsonQuoteFunction implements ValueParser {

    private ValueParser jsonParser;

    public JsonQuoteFunction(Function expr) {
        jsonParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        if (jsonParser == null) {
            return null;
        }
        String jsonString = OperatorTools.parseString(jsonParser.parse(sourceData, rowIndex, context));
        if (jsonString == null) {
            return null;
        }
        return JSON.toJSONString(jsonString);
    }
}
