package us.dot.its.jpo.ode.plugin.j2735.timstorage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import us.dot.its.jpo.ode.plugin.asn1.Asn1Object;

/**
 * The units of distance used in the message.
 */
@EqualsAndHashCode(callSuper = false)
public class DistanceUnits extends Asn1Object {
  private static final long serialVersionUID = 1L;

  /**
   * Enumeration of distance units.
   */
  public enum DistanceUnitsEnum {
    centimeter, // (0),
    cm2_5,      // (1), -- Steps of 2.5 centimeters
    decimeter,  // (2),
    meter,      // (3),
    kilometer,  // (4),
    foot,       // (5), -- US foot, 0.3048 meters exactly
    yard,       // (6), -- three US feet
    mile        // (7) -- US mile (5280 US feet)
  }

  private String centimeter;  // (0),
  @JsonProperty("cm2-5")
  private String cm2dot5;       // (1), -- Steps of 2.5 centimeters
  private String decimeter;   // (2),
  private String meter;       // (3),
  private String kilometer;   // (4),
  private String foot;        // (5), -- US foot, 0.3048 meters exactly
  private String yard;        // (6), -- three US feet
  private String mile;        // (7) -- US mile (5280 US feet)

  public String getCentimeter() {
    return centimeter;
  }

  public void setCentimeter(String centimeter) {
    this.centimeter = centimeter;
  }

  public String getCm2dot5() {
    return cm2dot5;
  }

  public void setCm2dot5(String cm2dot5) {
    this.cm2dot5 = cm2dot5;
  }

  public String getDecimeter() {
    return decimeter;
  }

  public void setDecimeter(String decimeter) {
    this.decimeter = decimeter;
  }

  public String getMeter() {
    return meter;
  }

  public void setMeter(String meter) {
    this.meter = meter;
  }

  public String getKilometer() {
    return kilometer;
  }

  public void setKilometer(String kilometer) {
    this.kilometer = kilometer;
  }

  public String getFoot() {
    return foot;
  }

  public void setFoot(String foot) {
    this.foot = foot;
  }

  public String getYard() {
    return yard;
  }

  public void setYard(String yard) {
    this.yard = yard;
  }

  public String getMile() {
    return mile;
  }

  public void setMile(String mile) {
    this.mile = mile;
  }
}
