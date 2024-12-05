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

package us.dot.its.jpo.ode.plugin.j2735.timstorage;

import lombok.EqualsAndHashCode;
import us.dot.its.jpo.ode.plugin.asn1.Asn1Object;

/**
 * Description.
 */
@EqualsAndHashCode(callSuper = false)
public class Description extends Asn1Object {
  private static final long serialVersionUID = 1L;

  private OffsetSystem path;

  private GeometricProjection geometry;

  private ValidRegion oldRegion;

  public OffsetSystem getPath() {
    return path;
  }

  public void setPath(OffsetSystem path) {
    this.path = path;
  }

  public GeometricProjection getGeometry() {
    return geometry;
  }

  public void setGeometry(GeometricProjection geometry) {
    this.geometry = geometry;
  }

  public ValidRegion getOldRegion() {
    return oldRegion;
  }

  public void setOldRegion(ValidRegion oldRegion) {
    this.oldRegion = oldRegion;
  }

}
