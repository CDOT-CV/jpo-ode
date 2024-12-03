package us.dot.its.jpo.ode.plugin.j2735.AddGrpC;

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
public class AltitudeValue extends Asn1Integer {

	public AltitudeValue() {
		super(-100000L, 800001L);
	}

	@JsonCreator
	public AltitudeValue(long value) {
		this();
		this.value = value;
	}

	public static class AltitudeValueDeserializer extends IntegerDeserializer<AltitudeValue> {
		public AltitudeValueDeserializer() {
			super(AltitudeValue.class);
		}

		@Override
		protected AltitudeValue construct() {
			return new AltitudeValue();
		}
	}
}