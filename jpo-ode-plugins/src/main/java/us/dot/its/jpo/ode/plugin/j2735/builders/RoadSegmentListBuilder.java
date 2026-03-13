package us.dot.its.jpo.ode.plugin.j2735.builders;

import java.util.Iterator;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735RoadSegmentList;

public class RoadSegmentListBuilder {
    private RoadSegmentListBuilder() {
		throw new UnsupportedOperationException();
	}
	
	public static J2735RoadSegmentList genericRoadSegmentList(JsonNode roadSegmentListNode) {
		J2735RoadSegmentList roadSegmentList = new J2735RoadSegmentList();

		JsonNode roadSegment = roadSegmentListNode.get("RoadSegment");
		if (roadSegment != null && roadSegment.isArray()) {
			Iterator<JsonNode> elements = roadSegment.values().iterator();

			while (elements.hasNext()) {
				roadSegmentList.getRoadSegList()
                    .add(RoadSegmentBuilder.genericRoadSegment(elements.next()));
			}
		} else if (roadSegment != null) {
			roadSegmentList.getRoadSegList()
				.add(RoadSegmentBuilder.genericRoadSegment(roadSegment));
		}

		return roadSegmentList;
	}
}
