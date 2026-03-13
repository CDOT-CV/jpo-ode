package us.dot.its.jpo.ode.plugin.j2735.builders;

import tools.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735HumanPropelledType;
import us.dot.its.jpo.ode.plugin.j2735.J2735AnimalPropelledType;
import us.dot.its.jpo.ode.plugin.j2735.J2735MotorizedPropelledType;

import us.dot.its.jpo.ode.plugin.j2735.J2735PropelledInformation;

public class PropelledInformationBuilder {

    private PropelledInformationBuilder() {
        throw new UnsupportedOperationException();
     }

    public static J2735PropelledInformation genericPropelledInformation(JsonNode propelledInformation) {
        J2735PropelledInformation pi = new J2735PropelledInformation();

        JsonNode human = propelledInformation.get("human");
        if (human != null){
			pi.setHuman(J2735HumanPropelledType.valueOf(human.asString().toUpperCase()));
        }

        JsonNode animal = propelledInformation.get("animal");
        if (animal != null){
			pi.setAnimal(J2735AnimalPropelledType.valueOf(animal.asString().toUpperCase()));
        }

        JsonNode motor = propelledInformation.get("motor");
        if (motor != null){
			pi.setMotor(J2735MotorizedPropelledType.valueOf(motor.asString().toUpperCase()));
        }

        return pi;
    }

}
