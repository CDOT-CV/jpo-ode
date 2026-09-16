package us.dot.its.jpo.ode.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.SignedDataMetadata;

class XmlUtilsCertificateMetadataTest {

  private static final Instant GENERATION_TIME = Instant.parse("2026-05-07T18:26:51.120Z");
  private static final Instant EXPIRY_TIME = Instant.parse("2026-05-12T10:00:05.000Z");
  private static final Instant VALIDITY_START = Instant.parse("2026-05-05T09:00:05.001Z");
  private static final Instant VALIDITY_END = Instant.parse("2026-05-12T10:00:05.002Z");

  @Test
  void roundTripsPopulatedCertificateMetadataThroughXmlUtils() throws Exception {
    SignedDataMetadata original = populatedMetadata();
    XmlUtils xmlUtils = new XmlUtils();

    assertMetadataEquals(original,
        (SignedDataMetadata) xmlUtils.fromXml(xmlUtils.toXml(original), SignedDataMetadata.class));
    assertMetadataEquals(original,
        (SignedDataMetadata) XmlUtils.fromXmlS(XmlUtils.toXmlStatic(original),
            SignedDataMetadata.class));
  }

  @Test
  void roundTripsAbsentOptionalCertificateTimestampsThroughXmlUtils() throws Exception {
    SignedDataMetadata original = new SignedDataMetadata();
    original.setPsid(32L);
    original.setGenerationTime(GENERATION_TIME);
    XmlUtils xmlUtils = new XmlUtils();

    SignedDataMetadata instanceResult = (SignedDataMetadata) xmlUtils.fromXml(
        xmlUtils.toXml(original), SignedDataMetadata.class);
    SignedDataMetadata staticResult = (SignedDataMetadata) XmlUtils.fromXmlS(
        XmlUtils.toXmlStatic(original), SignedDataMetadata.class);

    assertMetadataEquals(original, instanceResult);
    assertMetadataEquals(original, staticResult);
  }

  private static SignedDataMetadata populatedMetadata() {
    SignedDataMetadata metadata = new SignedDataMetadata();
    metadata.setPsid(32L);
    metadata.setGenerationTime(GENERATION_TIME);
    metadata.setExpiryTime(EXPIRY_TIME);
    metadata.setCertificateValidityStart(VALIDITY_START);
    metadata.setCertificateValidityEnd(VALIDITY_END);
    return metadata;
  }

  private static void assertMetadataEquals(SignedDataMetadata expected,
      SignedDataMetadata actual) {
    assertEquals(expected.getPsid(), actual.getPsid());
    assertEquals(expected.getGenerationTime(), actual.getGenerationTime());
    assertEquals(expected.getExpiryTime(), actual.getExpiryTime());
    assertEquals(expected.getCertificateValidityStart(), actual.getCertificateValidityStart());
    assertEquals(expected.getCertificateValidityEnd(), actual.getCertificateValidityEnd());
  }
}
