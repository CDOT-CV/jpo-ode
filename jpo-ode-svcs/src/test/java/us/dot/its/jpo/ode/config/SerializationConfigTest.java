package us.dot.its.jpo.ode.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.SignedDataMetadata;

class SerializationConfigTest {

  private static final Instant GENERATION_TIME = Instant.parse("2026-05-07T18:26:51.120Z");
  private static final Instant EXPIRY_TIME = Instant.parse("2026-05-12T10:00:05.000Z");
  private static final Instant VALIDITY_START = Instant.parse("2026-05-05T09:00:05.001Z");
  private static final Instant VALIDITY_END = Instant.parse("2026-05-12T10:00:05.002Z");

  @Test
  void bothSpringXmlMappersRoundTripCertificateMetadata() throws Exception {
    SerializationConfig config = new SerializationConfig();
    SignedDataMetadata populated = populatedMetadata();
    SignedDataMetadata sparse = new SignedDataMetadata();
    sparse.setPsid(32L);
    sparse.setGenerationTime(GENERATION_TIME);

    assertMetadataEquals(populated, roundTrip(config.xmlMapper(), populated));
    assertMetadataEquals(populated, roundTrip(config.simpleXmlMapper(), populated));
    assertMetadataEquals(sparse, roundTrip(config.xmlMapper(), sparse));
    assertMetadataEquals(sparse, roundTrip(config.simpleXmlMapper(), sparse));
  }

  private static SignedDataMetadata roundTrip(XmlMapper mapper, SignedDataMetadata value)
      throws Exception {
    return mapper.readValue(mapper.writeValueAsString(value), SignedDataMetadata.class);
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
