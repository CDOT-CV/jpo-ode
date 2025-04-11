package us.dot.its.jpo.ode.model;

public class OdeSdsmMetadata extends OdeLogMetadata {
    
    public enum SdsmSource {
		RSU, V2X, MMITSS, unknown
	}
    
    private String originIp;
    private SdsmSource sdsmSource;
    
    public OdeSdsmMetadata(OdeMsgPayload payload) {
        super(payload);
    }

    public OdeSdsmMetadata() {
        super();
    }

    public String getOriginIp() {
        return originIp;
    }
 
    public void setOriginIp(String originIp) {
        this.originIp = originIp;
    }

    public SdsmSource getSdsmSource() {
	    return sdsmSource;
	}

    public void setSdsmSource(SdsmSource sdsmSource) {
	    this.sdsmSource = sdsmSource;
	}
}
