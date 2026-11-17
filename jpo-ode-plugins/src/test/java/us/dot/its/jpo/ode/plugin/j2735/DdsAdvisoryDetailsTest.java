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
package us.dot.its.jpo.ode.plugin.j2735;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisoryDetails.AdvisoryBroadcastType;
import us.dot.its.jpo.ode.util.CodecUtils;

public class DdsAdvisoryDetailsTest {

   DdsAdvisoryDetails testDdsAdvisoryDetails = new DdsAdvisoryDetails();

   @Test
   public void testSettersGetters() {
      testDdsAdvisoryDetails.setAsdmID("testAsdmID");
      testDdsAdvisoryDetails.setAsdmType(1);
      testDdsAdvisoryDetails.setDistType(CodecUtils.toHex(DdsAdvisorySituationData.IP));
      testDdsAdvisoryDetails.setStartTime(new J2735DFullTime());
      testDdsAdvisoryDetails.setStopTime(new J2735DFullTime());
      testDdsAdvisoryDetails.setAdvisoryMessageBytes("testAdvisoryMessageBytes");
      testDdsAdvisoryDetails.setAdvisoryMessage("001F8084");
      
      assertEquals("testAsdmID", testDdsAdvisoryDetails.getAsdmID());
      assertEquals(1, testDdsAdvisoryDetails.getAsdmType());
      assertEquals("02", testDdsAdvisoryDetails.getDistType());
      assertNotNull(testDdsAdvisoryDetails.getStartTime());
      assertNotNull(testDdsAdvisoryDetails.getStopTime());
      assertEquals("testAdvisoryMessageBytes", testDdsAdvisoryDetails.getAdvisoryMessageBytes());
      assertNotNull(testDdsAdvisoryDetails.getAdvisoryMessage());
   }

   @Test
   public void testHashCodeAndEquals() {
      String distType = CodecUtils.toHex(DdsAdvisorySituationData.RSU);
      String advisoryMessage = "001F8084";

      DdsAdvisoryDetails ddsad1 = new DdsAdvisoryDetails("asdmID", AdvisoryBroadcastType.tim, distType, new J2735DFullTime(),
            new J2735DFullTime(), advisoryMessage);
      DdsAdvisoryDetails ddsad2 = new DdsAdvisoryDetails("asdmID", AdvisoryBroadcastType.tim, distType, new J2735DFullTime(),
            new J2735DFullTime(), advisoryMessage);
      DdsAdvisoryDetails ddsad3 = new DdsAdvisoryDetails("asdmID", AdvisoryBroadcastType.map, distType, new J2735DFullTime(),
            new J2735DFullTime(), advisoryMessage);

      assertEquals(ddsad1.hashCode(), ddsad2.hashCode(), "Expected identical hashcodes");
      assertNotEquals(ddsad2.hashCode(), ddsad3.hashCode(), "Expected different hashcodes");
      
      assertTrue(ddsad1.equals(ddsad2), "Expected objects to be equal");
      assertFalse(ddsad2.equals(ddsad3), "Expected objects to not be equal");
   }

}
