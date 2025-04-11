package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.*;

public class OdeSdsmData extends OdeData {

	private static final long serialVersionUID = 4944935387116447760L;

	public OdeSdsmData() {
		super();
	}

	public OdeSdsmData(OdeMsgMetadata metadata, OdeMsgPayload payload) {
		super(metadata, payload);
	}

	@Override
	@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = OdeSdsmMetadata.class)
	public void setMetadata(OdeMsgMetadata metadata) {
		super.setMetadata(metadata);
	}

	@Override
	@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = OdeSdsmPayload.class)
	public void setPayload(OdeMsgPayload payload) {
		super.setPayload(payload);
	}
}
