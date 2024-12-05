package us.dot.its.jpo.ode.plugin.j2735.AddGrpC;

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
@JsonSerialize(using = AltitudeConfidenceSerializer.class)
@JsonDeserialize(using = AltitudeConfidenceDeserializer.class)
public enum AltitudeConfidence implements Asn1Enumerated {
	ALT_000_01(0, "alt-000-01"), ALT_000_02(1, "alt-000-02"), ALT_000_05(2, "alt-000-05"), ALT_000_10(3,
			"alt-000-10"), ALT_000_20(4, "alt-000-20"), ALT_000_50(5, "alt-000-50"), ALT_001_00(6,
					"alt-001-00"), ALT_002_00(7, "alt-002-00"), ALT_005_00(8, "alt-005-00"), ALT_010_00(9,
							"alt-010-00"), ALT_020_00(10, "alt-020-00"), ALT_050_00(11, "alt-050-00"), ALT_100_00(12,
									"alt-100-00"), ALT_200_00(13,
											"alt-200-00"), OUTOFRANGE(14, "outOfRange"), UNAVAILABLE(15, "unavailable");

	private final int index;
	private final String name;

	public boolean hasExtensionMarker() {
		return false;
	}

	private AltitudeConfidence(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public int maxIndex() {
		return 15;
	}
}