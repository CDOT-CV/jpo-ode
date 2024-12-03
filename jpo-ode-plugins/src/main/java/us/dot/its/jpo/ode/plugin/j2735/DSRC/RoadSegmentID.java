package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import us.dot.its.jpo.ode.plugin.types.Asn1Integer;
import com.fasterxml.jackson.annotation.JsonCreator;
import us.dot.its.jpo.ode.plugin.serialization.IntegerDeserializer;

/**
 * 
 * <p>
 * This source code was generated by a tool. Manual edits are futile.
 * </p>
 * <p>
 * asn1jvm v1.0-SNAPSHOT
 * </p>
 * <p>
 * ASN.1 source files:
 * </p>
 * 
 * <pre>
 * J2735_201603DA.ASN
 * </pre>
 * 
 */
public class RoadSegmentID extends Asn1Integer {

	public RoadSegmentID() {
		super(0L, 65535L);
	}

	@JsonCreator
	public RoadSegmentID(long value) {
		this();
		this.value = value;
	}

	public static class RoadSegmentIDDeserializer extends IntegerDeserializer<RoadSegmentID> {
		public RoadSegmentIDDeserializer() {
			super(RoadSegmentID.class);
		}

		@Override
		protected RoadSegmentID construct() {
			return new RoadSegmentID();
		}
	}
}