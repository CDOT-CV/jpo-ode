/*******************************************************************************
 * Copyright 2018 572682.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at</p>
 *
 *   <p>http://www.apache.org/licenses/LICENSE-2.0</p>
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.</p>
 ******************************************************************************/

package us.dot.its.jpo.ode.plugin.j2735;

import java.util.ArrayList;
import java.util.List;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class J2735TrailerDataTest {
  @Tested
  J2735TrailerData td;

  @Test
  void testGettersAndSetters() {
    J2735PivotPointDescription connection = new J2735PivotPointDescription();
    td.setConnection(connection);
    Assertions.assertEquals(connection, td.getConnection());
    Integer sspRights = 1;
    td.setDoNotUse(sspRights);
    Assertions.assertEquals(sspRights, td.getDoNotUse());
    List<J2735TrailerUnitDescription> units = new ArrayList<>();
    td.setUnits(units);
    Assertions.assertEquals(units, td.getUnits());
  }
}
