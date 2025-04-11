package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.*;

import us.dot.its.jpo.asn.j2735.r2024.SensorDataSharingMessage.SensorDataSharingMessage;

public class OdeSdsmPayload extends OdeMsgPayload<SensorDataSharingMessage> {

	private static final long serialVersionUID = 7061315628111448390L;

	public OdeSdsmPayload() {
	  this(new SensorDataSharingMessage());
	}
  
	@JsonCreator
	public OdeSdsmPayload(@JsonProperty("data") SensorDataSharingMessage sdsm) {
	  super(sdsm);
	  this.setData(sdsm);
	}
}
