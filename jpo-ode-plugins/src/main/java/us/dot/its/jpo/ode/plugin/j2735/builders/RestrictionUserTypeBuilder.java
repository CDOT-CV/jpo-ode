package us.dot.its.jpo.ode.plugin.j2735.builders;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735RestrictionUserType;
import us.dot.its.jpo.ode.plugin.j2735.J2735RestrictionAppliesTo;

public class RestrictionUserTypeBuilder {
    private RestrictionUserTypeBuilder() {
		throw new UnsupportedOperationException();
	}
	
	public static J2735RestrictionUserType genericRestrictionUserType(JsonNode restrictionUserTypeNode) {
		J2735RestrictionUserType restrictionUserType = new J2735RestrictionUserType();

		JsonNode basicType = restrictionUserTypeNode.get("basicType");
		if(basicType != null)
		{
            restrictionUserType.setBasicType(J2735RestrictionAppliesTo.valueOf(basicType.propertyNames().iterator().next()));
        }

		return restrictionUserType;
	}
}
