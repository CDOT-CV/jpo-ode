package us.dot.its.jpo.ode.plugin.j2735.builders;

import java.util.Iterator;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735SpeedLimitList;

public class SpeedLimitListBuilder {
    private SpeedLimitListBuilder() {
		throw new UnsupportedOperationException();
	}

    public static J2735SpeedLimitList genericSpeedLimitList(JsonNode speedLimits) {
		J2735SpeedLimitList genericSpeedLimitList = new J2735SpeedLimitList();

		JsonNode regulatorySpeedLimit = speedLimits.get("RegulatorySpeedLimit");
		if (regulatorySpeedLimit != null && regulatorySpeedLimit.isArray()) {
			Iterator<JsonNode> elements = regulatorySpeedLimit.values().iterator();

			while (elements.hasNext()) {
				genericSpeedLimitList.getSpeedLimits()
					.add(RegulatorySpeedLimitBuilder.genericRegulatorySpeedLimit(elements.next()));
			}
		} else if (regulatorySpeedLimit != null) {
			genericSpeedLimitList.getSpeedLimits()
				.add(RegulatorySpeedLimitBuilder.genericRegulatorySpeedLimit(regulatorySpeedLimit));

		}
		return genericSpeedLimitList;
	}
}
