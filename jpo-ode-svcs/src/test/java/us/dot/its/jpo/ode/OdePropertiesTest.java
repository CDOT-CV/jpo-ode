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
package us.dot.its.jpo.ode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import mockit.Capturing;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import us.dot.its.jpo.ode.util.CommonUtils;

class OdePropertiesTest {

      @Tested
      OdeProperties testOdeProperties;
      @Injectable
      Environment mockEnv;
      @Injectable
      BuildProperties mockBuildProperties;

      @Capturing
      CommonUtils capturingCommonUtils;

      @Test
      void testInit() {
            new Expectations() {
                  {
                  }
            };
            try {
                  new OdeProperties();
            } catch (Exception e) {
                  fail("Unexpected exception: " + e);
            }
      }

      @Test
      void testSettersAndGetters() {

            String testPluginsLocations = "testpluginsLocations123456";
            String testCaCertPath = "testCaCertPath";
            String testSelfCertPath = "testSelfCertPath";
            String testSelfPrivateKeyReconstructionFilePath = "testSelfPrivateKeyReconstructionFilePath";
            String testSelfSigningPrivateKeyFilePath = "testSelfSigningPrivateKeyFilePath";

            boolean testVerboseJson = true;
            int testRsuSrmSlots = 22;

            String testSecuritySvcsSignatureUri = "testSecuritySvcsSignatureUri";
            String testRsuUsername = "testRsuUsername";
            String testRsuPassword = "testRsuPassword";

            testOdeProperties.setHostIP("test-host");
            testOdeProperties.setEnv(mockEnv);
            testOdeProperties.setEnvironment(mockEnv);
            testOdeProperties.setPluginsLocations(testPluginsLocations);
            testOdeProperties.setCaCertPath(testCaCertPath);
            testOdeProperties.setSelfCertPath(testSelfCertPath);
            testOdeProperties.setSelfPrivateKeyReconstructionFilePath(testSelfPrivateKeyReconstructionFilePath);
            testOdeProperties.setSelfSigningPrivateKeyFilePath(testSelfSigningPrivateKeyFilePath);
            testOdeProperties.setVerboseJson(testVerboseJson);
            testOdeProperties.setRsuSrmSlots(testRsuSrmSlots);

            testOdeProperties.setSecuritySvcsSignatureUri(testSecuritySvcsSignatureUri);
            testOdeProperties.setRsuUsername(testRsuUsername);
            testOdeProperties.setRsuPassword(testRsuPassword);

            assertEquals("test-host", testOdeProperties.getHostIP());
            assertEquals("Incorrect testEnv", mockEnv, testOdeProperties.getEnv());
            assertEquals("Incorrect testpluginsLocations", testPluginsLocations,
                        testOdeProperties.getPluginsLocations());
            assertEquals("Incorrect testCaCertPath", testCaCertPath, testOdeProperties.getCaCertPath());
            assertEquals("Incorrect testSelfCertPath", testSelfCertPath, testOdeProperties.getSelfCertPath());
            assertEquals("Incorrect testSelfPrivateKeyReconstructionFilePath", testSelfPrivateKeyReconstructionFilePath,
                        testOdeProperties.getSelfPrivateKeyReconstructionFilePath());
            assertEquals("Incorrect testSelfSigningPrivateKeyFilePath", testSelfSigningPrivateKeyFilePath,
                        testOdeProperties.getSelfSigningPrivateKeyFilePath());

            assertEquals("Incorrect testRsuSrmSlots", testRsuSrmSlots, testOdeProperties.getRsuSrmSlots());

            assertEquals("Incorrect testSecuritySvcsSignatureUri", testSecuritySvcsSignatureUri,
                        testOdeProperties.getSecuritySvcsSignatureUri());
            assertEquals("Incorrect testRsuUsername", testRsuUsername, testOdeProperties.getRsuUsername());
            assertEquals("Incorrect RsuPassword", testRsuPassword, testOdeProperties.getRsuPassword());

            testOdeProperties.getProperty("testProperty");
            testOdeProperties.getProperty("testProperty", 5);
            testOdeProperties.getProperty("testProperty", "testDefaultValue");
      }
}
