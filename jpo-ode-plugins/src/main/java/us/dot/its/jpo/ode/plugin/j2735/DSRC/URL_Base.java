package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import us.dot.its.jpo.ode.plugin.types.IA5String;
import com.fasterxml.jackson.annotation.JsonCreator;

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
public class URL_Base extends IA5String {

	public URL_Base() {
		super(1, 45);
	}

	@JsonCreator
	public URL_Base(String value) {
		this();
		this.value = value;
	}
}