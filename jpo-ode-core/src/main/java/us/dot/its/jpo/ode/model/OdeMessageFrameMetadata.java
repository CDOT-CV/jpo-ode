package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import us.dot.its.jpo.ode.plugin.ServiceRequest;

/**
 * Represents the metadata of a message frame.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OdeMessageFrameMetadata extends OdeLogMetadata {

  /**
   * Enum representing the source of a message frame.
   */
  public enum Source {
    RSU, V2X, MMITSS, EV, RV, SAT, SNMP, NA, UNKNOWN
  }

  private Source source;
  private String originIp;

  // Only used for messages created through the TIM deposit endpoint
  private ServiceRequest request;

  // otherwise it will deserialize as "certPresent"
  @JsonProperty("isCertPresent")
  private boolean isCertPresent;

  public OdeMessageFrameMetadata(OdeMsgPayload<?> payload) {
    super(payload);
  }

  public OdeMessageFrameMetadata(Source source) {
    this.source = source;
  }
}
