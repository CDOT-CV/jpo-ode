package us.dot.its.jpo.ode.plugin.j2735.REGION;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import us.dot.its.jpo.ode.plugin.j2735.DSRC.RegionalExtension;

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
@JsonInclude(Include.NON_NULL)
abstract public class Reg_GeometricProjection<TValue> extends RegionalExtension<TValue> {

	public Reg_GeometricProjection(int id, String name) {
		super(id, name);
	}
}