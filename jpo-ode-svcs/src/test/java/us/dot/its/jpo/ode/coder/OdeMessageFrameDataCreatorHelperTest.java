package us.dot.its.jpo.ode.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import us.dot.its.jpo.ode.util.JsonUtils;

class OdeMessageFrameDataCreatorHelperTest {

  @ParameterizedTest
  @CsvSource({
      "microseconds, 1500000, 2004-01-01T00:00:01.500Z",
      "milliseconds, 1500, 2004-01-01T00:00:01.500Z",
      "seconds, 90, 2004-01-01T00:01:30Z",
      "minutes, 90, 2004-01-01T01:30:00Z",
      "hours, 169, 2004-01-08T01:00:00Z",
      "sixtyHours, 2, 2004-01-06T00:00:00Z",
      "years, 0, 2004-01-01T00:00:00Z",
      "years, 1, 2004-12-31T05:49:12Z",
      "years, 4, 2007-12-31T23:16:48Z"
  })
  void convertsCertificateDuration(String unit, long duration, String expectedEnd) throws IOException {
    String xml;
    try (var input = getClass().getResourceAsStream(
        "/us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml")) {
      if (input == null) {
        throw new IOException("Decoder TIM fixture not found");
      }
      xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    xml = xml.replace("</metadata>",
        "<signatureValidityPeriod><start>0</start><duration><" + unit + ">"
            + duration + "</" + unit + "></duration></signatureValidityPeriod></metadata>");

    var data = OdeMessageFrameDataCreatorHelper.createOdeMessageFrameData(xml, new XmlMapper());

    assertEquals(Instant.parse("2004-01-01T00:00:00Z"),
        data.getMetadata().getCertMetadata().getCertificateValidityStart());
    assertEquals(Instant.parse(expectedEnd),
        data.getMetadata().getCertMetadata().getCertificateValidityEnd());
  }

  @Test
  void convertsSparseCertificateMetadataToJson() throws IOException {
    String xml;
    try (var input = getClass().getResourceAsStream(
        "/us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml")) {
      if (input == null) {
        throw new IOException("Decoder TIM fixture not found");
      }
      xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    xml = xml.replace("</metadata>",
        "<signatureValidityPeriod><start>428058000</start>"
            + "<duration><hours>1</hours></duration></signatureValidityPeriod></metadata>");

    var data = OdeMessageFrameDataCreatorHelper.createOdeMessageFrameData(xml, new XmlMapper());
    var certMetadata = data.getMetadata().getCertMetadata();

    assertNull(certMetadata.getGenerationTime());
    assertNull(certMetadata.getExpiryTime());
    assertEquals(Instant.parse("2017-07-25T09:00:00Z"),
        certMetadata.getCertificateValidityStart());
    assertEquals(Instant.parse("2017-07-25T10:00:00Z"),
        certMetadata.getCertificateValidityEnd());

    JSONObject jsonMetadata = new JSONObject(JsonUtils.toJson(data, false))
        .getJSONObject("metadata").getJSONObject("certMetadata");
    assertFalse(jsonMetadata.has("generationTime"));
    assertFalse(jsonMetadata.has("expiryTime"));
    assertEquals("2017-07-25T09:00:00.000Z",
        jsonMetadata.getString("certificateValidityStart"));
    assertEquals("2017-07-25T10:00:00.000Z",
        jsonMetadata.getString("certificateValidityEnd"));
  }
}
