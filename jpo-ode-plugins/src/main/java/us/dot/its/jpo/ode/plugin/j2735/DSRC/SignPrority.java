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
public class SignPrority extends Asn1Integer {

	public SignPrority() {
		super(0L, 7L);
	}

	@JsonCreator
	public SignPrority(long value) {
		this();
		this.value = value;
	}

	public static class SignProrityDeserializer extends IntegerDeserializer<SignPrority> {
		public SignProrityDeserializer() {
			super(SignPrority.class);
		}

		@Override
		protected SignPrority construct() {
			return new SignPrority();
		}
	}
}