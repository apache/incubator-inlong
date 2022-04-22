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

package org.apache.inlong.sort.protocol.transformation.relation;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link UnionNodeRelationShip}
 */
public class UnionNodeRelationTest {

    /**
     * Test serialize for UnionNodeRelationShip
     *
     * @throws JsonProcessingException The exception may throws when execute the method
     */
    @Test
    public void testSerialize() throws JsonProcessingException {
        UnionNodeRelationShip relationShip = new UnionNodeRelationShip(Arrays.asList("1", "2"),
                Collections.singletonList("3"));
        ObjectMapper objectMapper = new ObjectMapper();
        String expected = "{\"type\":\"union\",\"inputs\":[\"1\",\"2\"],\"outputs\":[\"3\"]}";
        assertEquals(expected, objectMapper.writeValueAsString(relationShip));
    }

    /**
     * Test deserialize for UnionNodeRelationShip
     *
     * @throws JsonProcessingException The exception may throws when execute the method
     */
    @Test
    public void testDeserialize() throws JsonProcessingException {
        UnionNodeRelationShip relationShip = new UnionNodeRelationShip(Arrays.asList("1", "2"),
                Collections.singletonList("3"));
        ObjectMapper objectMapper = new ObjectMapper();
        String relationShipStr = "{\"type\":\"union\",\"inputs\":[\"1\",\"2\"],\"outputs\":[\"3\"]}";
        UnionNodeRelationShip expected = objectMapper.readValue(relationShipStr, UnionNodeRelationShip.class);
        assertEquals(expected, relationShip);
    }

}
