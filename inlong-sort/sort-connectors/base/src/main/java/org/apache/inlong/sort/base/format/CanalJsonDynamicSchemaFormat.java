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

package org.apache.inlong.sort.base.format;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import static org.apache.inlong.sort.base.Constants.DATA;
import static org.apache.inlong.sort.base.Constants.PK_NAMES;

/**
 * Canal json dynamic format
 */
public class CanalJsonDynamicSchemaFormat extends JsonDynamicSchemaFormat {

    private static final String IDENTIFIER = "canal-json";

    private static final CanalJsonDynamicSchemaFormat FORMAT = new CanalJsonDynamicSchemaFormat();

    private CanalJsonDynamicSchemaFormat() {

    }

    @SuppressWarnings("rawtypes")
    public static AbstractDynamicSchemaFormat getInstance() {
        return FORMAT;
    }

    @Override
    public JsonNode getPhysicalData(JsonNode root) {
        JsonNode physicalData = root.get(DATA);
        if (physicalData != null) {
            return root.get(DATA).get(0);
        }
        return null;
    }

    @Override
    public List<String> extractPrimaryKeyNames(JsonNode data) {
        JsonNode pkNamesNode = data.get(PK_NAMES);
        List<String> pkNames = new ArrayList<>();
        if (pkNamesNode != null && pkNamesNode.isArray()) {
            for (int i = 0; i < pkNamesNode.size(); i++) {
                pkNames.add(pkNamesNode.get(i).asText());
            }
        }
        return pkNames;
    }

    /**
     * Get the identifier of this dynamic schema format
     *
     * @return The identifier of this dynamic schema format
     */
    @Override
    public String identifier() {
        return IDENTIFIER;
    }
}
