package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import us.dot.its.jpo.ode.plugin.serialization.EnumeratedDeserializer;

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
public class DirectionOfUseDeserializer extends EnumeratedDeserializer<DirectionOfUse> {

	DirectionOfUseDeserializer() {
		super(DirectionOfUse.class);
	}

	@Override
	protected DirectionOfUse[] listEnumValues() {
		return DirectionOfUse.values();
	}
}