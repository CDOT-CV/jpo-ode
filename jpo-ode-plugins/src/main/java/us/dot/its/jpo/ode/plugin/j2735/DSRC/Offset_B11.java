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
public class Offset_B11 extends Asn1Integer {

	public Offset_B11() {
		super(-1024L, 1023L);
	}

	@JsonCreator
	public Offset_B11(long value) {
		this();
		this.value = value;
	}

	public static class Offset_B11Deserializer extends IntegerDeserializer<Offset_B11> {
		public Offset_B11Deserializer() {
			super(Offset_B11.class);
		}

		@Override
		protected Offset_B11 construct() {
			return new Offset_B11();
		}
	}
}