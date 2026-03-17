/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;

public class JsonUtilsTest {

    private static final String OVDF = 
            "{\"className\":\"com.bah.ode.model.OdeVehicleDataFlat\",\"serialId\":\"10817812-036b-4d7b-867b-ae0bc62a2b3e.0\",\"receivedAt\":\"2015-07-22T19:21:16.413+0000\",\"groupId\":\"4130008F\",\"accelLong\":0.34,\"accelVert\":0.00,\"accellYaw\":8.42,\"heading\":65.9500,\"speed\":8.12,\"sizeLength\":500,\"sizeWidth\":200,\"latitude\":42.3296667,\"longitude\":-83.0445390,\"elevation\":156.9,\"tempId\":\"C4290123\",\"year\":2015,\"month\":5,\"day\":13,\"hour\":15,\"minute\":52,\"second\":45.500,\"dateTime\":\"2015-06-13T19:52:45.500+0000\"}";
    private static final String ODM =
            "{\"metadata\":{\"payloadType\":\"veh\",\"version\":1},\"payload\":" + OVDF + "}";
      
    class A {
        int i;
        float f;
        double d;
        String s;

        public A() {
        }

        public A(int i, float f, double d, String s) {
            super();
            this.i = i;
            this.f = f;
            this.d = d;
            this.s = s;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            long temp;
            temp = Double.doubleToLongBits(d);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + Float.floatToIntBits(f);
            result = prime * result + i;
            result = prime * result + ((s == null) ? 0 : s.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            A other = (A) obj;
            if (Double.doubleToLongBits(d) != Double.doubleToLongBits(other.d))
                return false;
            if (Float.floatToIntBits(f) != Float.floatToIntBits(other.f))
                return false;
            if (i != other.i)
                return false;
            if (s == null) {
                if (other.s != null)
                    return false;
            } else if (!s.equals(other.s))
                return false;
            return true;
        }

        private JsonUtilsTest getOuterType() {
            return JsonUtilsTest.this;
        }

      
    }

    private final A a = new A(11, 2.2f, 33333.33333333333d, "hello");

    private final A a2 = new A(11, 2.2f, 33333.33333333333d, null);

    @Test
    public void testToJson() {
        String aj = JsonUtils.toJson(a, false);
        String aJsonString = "{\"d\":33333.33333333333,\"f\":2.2,\"i\":11,\"s\":\"hello\"}";
        Assertions.assertEquals(aJsonString, aj);
        String aj2Compact = JsonUtils.toJson(a2, false);
        String a2Compact = "{\"d\":33333.33333333333,\"f\":2.2,\"i\":11}";
        Assertions.assertEquals(a2Compact, aj2Compact);
        String aj2Verbose = JsonUtils.toJson(a2, true);
        String a2Verbose = "{\"d\":33333.33333333333,\"f\":2.2,\"i\":11,\"s\":null}";
        Assertions.assertEquals(a2Verbose, aj2Verbose);
    }

    @Test
    public void testNewJson() {
        String j = JsonUtils.newJson("key", "value");
        Assertions.assertEquals("{\"key\":\"value\"}", j);
    }

    @Test
    public void testNewObjectNode() {
        ObjectNode j = JsonUtils.newObjectNode("key", "value");
        Assertions.assertEquals("{\"key\":\"value\"}", j.toString());
    }

    @Test
    public void testAddNode() {
        ObjectNode j = JsonUtils.newObjectNode("key", "value");
        ObjectNode j2 = JsonUtils.addNode(j, "key2", "value2");
        Assertions.assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", j.toString());
        Assertions.assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", j2.toString());
    }

    @Test
    public void testGetJsonNode() {
        Object v = JsonUtils.getJsonNode("{\"key\":\"value\"}", "key");
        Assertions.assertEquals("\"value\"", v.toString());
    }

    @Test
    public void testNewNode() {
        Assertions.assertEquals("{}", JsonUtils.newNode().toString());
    }

    @Test
    public void testToObjectNode() throws JsonUtilsException {
        ObjectNode on = JsonUtils.toObjectNode("{\"key\":\"value\"}");
        Assertions.assertEquals("{\"key\":\"value\"}", on.toString());
        ObjectNode ovdf = JsonUtils.toObjectNode(OVDF);
        String expectedOvdf = "{\"className\":\"com.bah.ode.model.OdeVehicleDataFlat\",\"serialId\":\"10817812-036b-4d7b-867b-ae0bc62a2b3e.0\",\"receivedAt\":\"2015-07-22T19:21:16.413+0000\",\"groupId\":\"4130008F\",\"accelLong\":0.34,\"accelVert\":0.0,\"accellYaw\":8.42,\"heading\":65.95,\"speed\":8.12,\"sizeLength\":500,\"sizeWidth\":200,\"latitude\":42.3296667,\"longitude\":-83.044539,\"elevation\":156.9,\"tempId\":\"C4290123\",\"year\":2015,\"month\":5,\"day\":13,\"hour\":15,\"minute\":52,\"second\":45.5,\"dateTime\":\"2015-06-13T19:52:45.500+0000\"}";
        Assertions.assertEquals(expectedOvdf, ovdf.toString());
        JsonUtils.addNode(ovdf, "avgSpeed", "2.22");
        Assertions.assertEquals("{\"className\":\"com.bah.ode.model.OdeVehicleDataFlat\",\"serialId\":\"10817812-036b-4d7b-867b-ae0bc62a2b3e.0\",\"receivedAt\":\"2015-07-22T19:21:16.413+0000\",\"groupId\":\"4130008F\",\"accelLong\":0.34,\"accelVert\":0.0,\"accellYaw\":8.42,\"heading\":65.95,\"speed\":8.12,\"sizeLength\":500,\"sizeWidth\":200,\"latitude\":42.3296667,\"longitude\":-83.044539,\"elevation\":156.9,\"tempId\":\"C4290123\",\"year\":2015,\"month\":5,\"day\":13,\"hour\":15,\"minute\":52,\"second\":45.5,\"dateTime\":\"2015-06-13T19:52:45.500+0000\",\"avgSpeed\":\"2.22\"}", ovdf.toString());
    }

    @Test
    public void testIsValid() throws IOException {
        Assertions.assertTrue(JsonUtils.isValid("{\"key\":\"value\"}"));
        Assertions.assertFalse(JsonUtils.isValid("{\"key\":value}"));
    }

    @Test
    public void testJsonNodeToHashMap() {
        ObjectNode on = JsonUtils.newObjectNode("key", "value");
        HashMap<String, JsonNode> hm = JsonUtils.jsonNodeToHashMap(on);
        Assertions.assertEquals("value", hm.get("key").asString());

        JsonNode jsonNode = JsonUtils.getJsonNode(ODM, "payload");
        HashMap<String, JsonNode> hashMap = JsonUtils.jsonNodeToHashMap(jsonNode);
        Iterator<Entry<String, JsonNode>> fieldsIter = jsonNode.properties().iterator();

        while (fieldsIter.hasNext()) {
            Entry<String, JsonNode> field = fieldsIter.next();
            JsonNode node = hashMap.get(field.getKey());
            Assertions.assertNotNull(node);
            Assertions.assertEquals(node.asString(), field.getValue().asString());
        }

        for (Entry<String, JsonNode> entry : hashMap.entrySet()) {
            JsonNode node = jsonNode.get(entry.getKey());
            Assertions.assertNotNull(node);
            Assertions.assertEquals(node.asString(), entry.getValue().asString());
        }
    }

    @Test
    public void testGetJson() {
        Assertions.assertEquals("10817812-036b-4d7b-867b-ae0bc62a2b3e.0", JsonUtils.getJsonNode(OVDF, "serialId").asString());
    }

    @Test
    public void testPutObject() {
        ObjectNode dm = JsonUtils.newNode();
        dm.putObject("metadata");
        dm.putObject("payload").setAll(JsonUtils.newObjectNode("key1", "value1"));
        Assertions.assertEquals("{\"metadata\":{},\"payload\":{\"key1\":\"value1\"}}", dm.toString());
    }

}
