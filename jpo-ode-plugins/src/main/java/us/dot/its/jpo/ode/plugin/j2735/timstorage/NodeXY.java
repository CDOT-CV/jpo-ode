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
 * NodeXY.
 */
@EqualsAndHashCode(callSuper = false)
public class NodeXY extends Asn1Object {
  private static final long serialVersionUID = 1L;
  private NodeOffsetPointXY delta;
  private NodeAttributeSetXY attributes;

  public NodeOffsetPointXY getDelta() {
    return delta;
  }

  public void setDelta(NodeOffsetPointXY delta) {
    this.delta = delta;
  }

  public NodeAttributeSetXY getAttributes() {
    return attributes;
  }

  public void setAttributes(NodeAttributeSetXY attributes) {
    this.attributes = attributes;
  }

}
