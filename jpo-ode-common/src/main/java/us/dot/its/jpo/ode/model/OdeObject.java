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

import java.io.Serial;
import java.io.Serializable;
import tools.jackson.core.JacksonException;

import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

public class OdeObject implements Serializable {
   @Serial
   private static final long serialVersionUID = 7514526408925039533L;

    /**
     * Json utils is deprecated. Please use the object/json mapper from the Spring application context instead.
     */
   @Deprecated(forRemoval = true)
   public String toJson() {
      return JsonUtils.toJson(this, false);
   }

    /**
     * Json utils is deprecated. Please use the object/json mapper from the Spring application context instead.
     */
   @Deprecated(forRemoval = true)
   public String toJson(boolean verbose) {
      return JsonUtils.toJson(this, verbose);
   }

   public String toXml() throws XmlUtilsException, JacksonException {
      return XmlUtils.toXmlStatic(this);
   }

   @Override
   public String toString() {
      return this.toJson(false);
   }

}
