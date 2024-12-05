package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import lombok.Getter;
import us.dot.its.jpo.ode.plugin.types.Asn1Enumerated;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * 
 *******************************************************************************
 *
 * This source file was generated by a tool. Beware manual edits might be
 * overwritten in future releases. asn1jvm v1.0-SNAPSHOT
 *
 *******************************************************************************
 * Copyright 2024 USDOT
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************
 * 
 */
@Getter
@JsonSerialize(using = TravelerInfoTypeSerializer.class)
@JsonDeserialize(using = TravelerInfoTypeDeserializer.class)
public enum TravelerInfoType implements Asn1Enumerated {
	UNKNOWN(0, "unknown"), ADVISORY(1, "advisory"), ROADSIGNAGE(2, "roadSignage"), COMMERCIALSIGNAGE(3,
			"commercialSignage");

	private final int index;
	private final String name;

	public boolean hasExtensionMarker() {
		return false;
	}

	private TravelerInfoType(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public int maxIndex() {
		return 3;
	}
}