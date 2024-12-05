package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import us.dot.its.jpo.ode.plugin.types.Asn1Choice;
import lombok.Getter;
import lombok.Setter;
import us.dot.its.jpo.ode.plugin.annotations.Asn1Property;
import com.fasterxml.jackson.annotation.JsonProperty;
import us.dot.its.jpo.ode.plugin.j2735.REGION.Reg_NodeOffsetPointLL;
import java.util.Optional;
import java.util.List;
import us.dot.its.jpo.ode.plugin.types.Asn1Type;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
@Setter
@JsonInclude(Include.NON_NULL)
public class NodeOffsetPointLL extends Asn1Choice {

	@Asn1Property(tag = 0, name = "node-LL1")
	@JsonProperty("node-LL1")
	private Node_LL_24B node_LL1;
	@Asn1Property(tag = 1, name = "node-LL2")
	@JsonProperty("node-LL2")
	private Node_LL_28B node_LL2;
	@Asn1Property(tag = 2, name = "node-LL3")
	@JsonProperty("node-LL3")
	private Node_LL_32B node_LL3;
	@Asn1Property(tag = 3, name = "node-LL4")
	@JsonProperty("node-LL4")
	private Node_LL_36B node_LL4;
	@Asn1Property(tag = 4, name = "node-LL5")
	@JsonProperty("node-LL5")
	private Node_LL_44B node_LL5;
	@Asn1Property(tag = 5, name = "node-LL6")
	@JsonProperty("node-LL6")
	private Node_LL_48B node_LL6;
	@Asn1Property(tag = 6, name = "node-LatLon")
	@JsonProperty("node-LatLon")
	private Node_LLmD_64b node_LatLon;
	@Asn1Property(tag = 7)
	private Reg_NodeOffsetPointLL regional;

	NodeOffsetPointLL() {
		super(false);
	}

	@Override
	protected List<Optional<Asn1Type>> listTypes() {
		return null;
	}
}