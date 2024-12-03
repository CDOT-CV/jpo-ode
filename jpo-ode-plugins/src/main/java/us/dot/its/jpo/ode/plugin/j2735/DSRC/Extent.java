package us.dot.its.jpo.ode.plugin.j2735.DSRC;

import us.dot.its.jpo.ode.plugin.types.Asn1Enumerated;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
@JsonSerialize(using = ExtentSerializer.class)
@JsonDeserialize(using = ExtentDeserializer.class)
public enum Extent implements Asn1Enumerated {
	useInstantlyOnly(0, "useInstantlyOnly"), useFor3meters(1, "useFor3meters"), useFor10meters(2,
			"useFor10meters"), useFor50meters(3, "useFor50meters"), useFor100meters(4,
					"useFor100meters"), useFor500meters(5, "useFor500meters"), useFor1000meters(6,
							"useFor1000meters"), useFor5000meters(7, "useFor5000meters"), useFor10000meters(8,
									"useFor10000meters"), useFor50000meters(9, "useFor50000meters"), useFor100000meters(
											10, "useFor100000meters"), useFor500000meters(11,
													"useFor500000meters"), useFor1000000meters(12,
															"useFor1000000meters"), useFor5000000meters(13,
																	"useFor5000000meters"), useFor10000000meters(14,
																			"useFor10000000meters"), forever(15,
																					"forever");

	private final int index;
	private final String name;

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public boolean hasExtensionMarker() {
		return false;
	}

	private Extent(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public int maxIndex() {
		return 15;
	}
}