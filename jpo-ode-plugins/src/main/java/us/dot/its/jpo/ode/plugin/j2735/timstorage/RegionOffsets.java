package us.dot.its.jpo.ode.plugin.j2735.timstorage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import us.dot.its.jpo.ode.plugin.asn1.Asn1Object;

/**
 * The offsets of a region.
 */
@EqualsAndHashCode(callSuper = false)
public class RegionOffsets extends Asn1Object {
  private static final long serialVersionUID = 1L;

  @JsonProperty("xOffset")
  private int offsetX;
  @JsonProperty("yOffset")
  private int offsetY;
  @JsonProperty("zOffset")
  private int offsetZ;

  public int getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }

  public int getOffsetZ() {
    return offsetZ;
  }

  public void setOffsetZ(int offsetZ) {
    this.offsetZ = offsetZ;
  }


}
