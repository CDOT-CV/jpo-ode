/*==============================================================================
 *
 * This source file was generated by a tool.
 * Beware manual edits might be overwritten in future releases.
 * asn1jvm v1.0-SNAPSHOT
 *
 *------------------------------------------------------------------------------
 * Copyright 2024 USDOT
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
 *============================================================================*/

package us.dot.its.jpo.ode.plugin.j2735.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import us.dot.its.jpo.ode.plugin.types.Asn1Sequence;

abstract public class RegionalExtension<TValue> extends Asn1Sequence {

	@JsonIgnore
	final protected RegionId regionId;
	@JsonIgnore
	final protected String name;
	private TValue regExtValue;
	public final static String INFORMATION_OBJECT_CLASS = "REG_EXT_ID_AND_TYPE";

	public RegionId getRegionId() {
		return regionId;
	}

	public String getName() {
		return name;
	}

	@JsonProperty("regionId")
	public String getIdString() {
		return regionId.toString();
	}

	public TValue getRegExtValue() {
		return regExtValue;
	}

	public void setRegExtValue(TValue regExtValue) {
		this.regExtValue = regExtValue;
	}

	public RegionalExtension(int id, String name) {
		super(true);
		var theId = new RegionId();
		theId.setValue(id);
		this.regionId = theId;
		this.name = name;
	}
}