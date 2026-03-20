package us.dot.its.jpo.ode.plugin.j2735.builders;

import java.util.Iterator;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735IntersectionStateList;

@Deprecated(forRemoval = true)
public class IntersectionStateListBuilder {
	private IntersectionStateListBuilder() {
		throw new UnsupportedOperationException();
	}

	public static J2735IntersectionStateList genericIntersectionStateList(JsonNode intersections) {
		J2735IntersectionStateList genericIntersectionStateList = new J2735IntersectionStateList();
		if (intersections.isArray()) {
			Iterator<JsonNode> elements = intersections.values().iterator();
			while (elements.hasNext()) {
				genericIntersectionStateList.getIntersectionStatelist()
						.add(IntersectionStateBuilder.genericIntersectionState(elements.next()));
			}
		} else {
			genericIntersectionStateList.getIntersectionStatelist()
					.add(IntersectionStateBuilder.genericIntersectionState(intersections));

		}
		return genericIntersectionStateList;
	}
}
