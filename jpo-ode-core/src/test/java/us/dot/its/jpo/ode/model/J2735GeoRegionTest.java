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
package us.dot.its.jpo.ode.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.plugin.j2735.OdeGeoRegion;
import us.dot.its.jpo.ode.plugin.j2735.OdeGeoRegion.GeoRegionException;
import us.dot.its.jpo.ode.plugin.j2735.OdePosition3D;

/**
 * The class <code>J2735GeoRegionTest</code> contains tests for the class {@link
 * <code>J2735GeoRegion</code>}
 *
 * @pattern JUnit Test Case
 *
 * @generatedBy CodePro at 10/2/16 10:03 AM
 *
 * @author 572682
 *
 * @version $Revision$
 */
public class J2735GeoRegionTest {

   private static final String TEST_GEO_REGION = "42.537903,-83.477903 42.305753,   -82.842753";

   /**
    * Run the conversion costructir test
   * @throws GeoRegionException 
    * @throws Exception 
    */
   @Test
   public void testConstructorString() throws GeoRegionException {
      OdeGeoRegion fixture = new OdeGeoRegion(TEST_GEO_REGION);
      assertEquals(BigDecimal.valueOf(42.537903), fixture.getNwCorner().getLatitude());
      assertEquals(BigDecimal.valueOf(-83.477903), fixture.getNwCorner().getLongitude());
      assertEquals(BigDecimal.valueOf(42.305753), fixture.getSeCorner().getLatitude());
      assertEquals(BigDecimal.valueOf(-82.842753), fixture.getSeCorner().getLongitude());
   }
   
   /**
    * Run the J2735GeoRegion setNwCorner(J2735Position3D) method test
    */
   @Test
   public void testSetNwCorner() {
      OdeGeoRegion fixture = new OdeGeoRegion();
      OdePosition3D nwCorner = new OdePosition3D(BigDecimal.valueOf(42.537903), BigDecimal.valueOf(-83.477903), BigDecimal.valueOf(0.0));
      OdeGeoRegion result = fixture.setNwCorner(nwCorner);
      assertEquals(nwCorner, result.getNwCorner());
   }

   /**
    * Run the J2735GeoRegion setSeCorner(J2735Position3D) method test
    */
   @Test
   public void testSetSeCorner() {
     OdeGeoRegion fixture = new OdeGeoRegion();
     OdePosition3D seCorner = new OdePosition3D(BigDecimal.valueOf(42.537903), BigDecimal.valueOf(-83.477903), BigDecimal.valueOf(0.0));
     OdeGeoRegion result = fixture.setSeCorner(seCorner);
     assertEquals(seCorner, result.getSeCorner());
   }

   /**
    * Run the J2735Position3D getCenterPosition() method test
   * @throws GeoRegionException 
    * @throws Exception 
    */
   @Test
   public void testGetCenterPosition() throws GeoRegionException {
      // add test code here
      OdeGeoRegion fixture = new OdeGeoRegion("42.537903,-83.477903 42.305753,-82.842753");
      OdePosition3D result = fixture.getCenterPosition();
      assertEquals(BigDecimal.valueOf(42.421828000000005), result.getLatitude());
      assertEquals(BigDecimal.valueOf(-83.16032799999999), result.getLongitude());
   }

   /**
    * Run the int hashCode() method test
   * @throws GeoRegionException 
    */
   @Test
   public void testHashCode() throws GeoRegionException {
     OdeGeoRegion fixture = new OdeGeoRegion(TEST_GEO_REGION);
     int result = fixture.hashCode();
     assertEquals(1047387837, result);
   }

   /**
    * Run the boolean equals(Object) method test
   * @throws GeoRegionException 
    */
   @Test
   public void testEquals() throws GeoRegionException {
     OdeGeoRegion fixture1 = new OdeGeoRegion(TEST_GEO_REGION);
     OdeGeoRegion fixture2 = new OdeGeoRegion(TEST_GEO_REGION);
     assertTrue(fixture1.equals(fixture2));
   }
}
