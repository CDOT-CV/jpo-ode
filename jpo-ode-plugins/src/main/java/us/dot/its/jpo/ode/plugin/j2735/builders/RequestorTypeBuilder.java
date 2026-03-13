package us.dot.its.jpo.ode.plugin.j2735.builders;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735RequestorType;
import us.dot.its.jpo.ode.plugin.j2735.J2735BasicVehicleRole;
import us.dot.its.jpo.ode.plugin.j2735.J2735RequestSubRole;
import us.dot.its.jpo.ode.plugin.j2735.J2735RequestImportanceLevel;
import us.dot.its.jpo.ode.plugin.j2735.J2735VehicleType;

public class RequestorTypeBuilder {
    private RequestorTypeBuilder() {
		throw new UnsupportedOperationException();
	}

    public static J2735RequestorType genericRequestorType(JsonNode typeData) {
        J2735RequestorType requestorType = new J2735RequestorType();

        JsonNode role = typeData.get("role");
		if(role != null)
		{
            J2735BasicVehicleRole enumRole = J2735BasicVehicleRole.valueOf(role.propertyNames().iterator().next());
			requestorType.setRole(enumRole);
		}

        JsonNode subrole = typeData.get("subrole");
		if(subrole != null)
		{
            J2735RequestSubRole enumSubrole = J2735RequestSubRole.valueOf(subrole.propertyNames().iterator().next());
			requestorType.setSubrole(enumSubrole);
		}

        JsonNode request = typeData.get("request");
		if(request != null)
		{
            J2735RequestImportanceLevel enumRequest = J2735RequestImportanceLevel.valueOf(request.propertyNames().iterator().next());
			requestorType.setRequest(enumRequest);
		}

        JsonNode iso3883 = typeData.get("iso3883");
		if(iso3883 != null)
		{
			requestorType.setIso3883(iso3883.asInt());
		}

        JsonNode hpmsType = typeData.get("hpmsType");
		if(hpmsType != null)
		{
            J2735VehicleType enumVehicleType = J2735VehicleType.valueOf(hpmsType.propertyNames().iterator().next());
			requestorType.setHpmsType(enumVehicleType);
		}

        return requestorType;
    }
}
