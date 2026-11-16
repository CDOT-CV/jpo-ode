package us.dot.its.jpo.ode.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.SignedDataMetadata;
import us.dot.its.jpo.ode.util.JsonUtils;

class OdeMessageFrameDataCreatorHelperTest {

  private static final Instant IEEE_1609_2_EPOCH = Instant.parse("2004-01-01T00:00:00Z");

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
    var certMetadata = certificateValidity(0, unit, duration);

    assertEquals(IEEE_1609_2_EPOCH, certMetadata.getCertificateValidityStart());
    assertEquals(Instant.parse(expectedEnd), certMetadata.getCertificateValidityEnd());
  }

  @ParameterizedTest(name = "{0} microseconds ends at {1}")
  @CsvSource({
      "500, 2004-01-01T00:00:00.000500Z",
      "999, 2004-01-01T00:00:00.000999Z",
      "1500, 2004-01-01T00:00:00.001500Z",
      "65535, 2004-01-01T00:00:00.065535Z"
  })
  void preservesMicrosecondCertificateDuration(long microseconds, String expectedEnd) throws IOException {
    var certMetadata = certificateValidity(0, "microseconds", microseconds);

    assertEquals(IEEE_1609_2_EPOCH, certMetadata.getCertificateValidityStart());
    assertEquals(Instant.parse(expectedEnd), certMetadata.getCertificateValidityEnd());
  }

  @ParameterizedTest(name = "Time64 {0} is {1}")
  @CsvSource({
      "500, 2004-01-01T00:00:00.000500Z",
      "1500, 2004-01-01T00:00:00.001500Z",
      "428169792505460, 2017-07-26T16:03:12.505460Z"
  })
  void preservesTime64Microseconds(long time64Microseconds, String expected) throws IOException {
    var certMetadata = frameWithMetadata(
        "<signedDataHeaderInfo><generationTime>" + time64Microseconds
            + "</generationTime></signedDataHeaderInfo>")
        .getMetadata().getCertMetadata();

    assertEquals(Instant.parse(expected), certMetadata.getGenerationTime());
  }

  @Test
  void time64SampleKeepsMicrosecondsAndIgnoresLeapSeconds() throws IOException {
    var data = frameWithMetadata(
        "<signedDataHeaderInfo><generationTime>428169792505460</generationTime>"
            + "</signedDataHeaderInfo>");
    var certMetadata = data.getMetadata().getCertMetadata();

    assertEquals(Instant.parse("2017-07-26T16:03:12.505460Z"), certMetadata.getGenerationTime());
    assertNotEquals(Instant.parse("2017-07-26T16:03:07.505460Z"), certMetadata.getGenerationTime());

    JSONObject jsonMetadata = new JSONObject(JsonUtils.toJson(data, false))
        .getJSONObject("metadata").getJSONObject("certMetadata");
    assertEquals("2017-07-26T16:03:12.505Z", jsonMetadata.getString("generationTime"));
  }

  @Test
  void blankAndNonNumericTimingValuesStayUnset() throws IOException {
    var blankGenerationTime = frameWithMetadata(
        "<signedDataHeaderInfo><generationTime></generationTime></signedDataHeaderInfo>")
        .getMetadata().getCertMetadata();
    assertNull(blankGenerationTime.getGenerationTime());
    assertNull(blankGenerationTime.getExpiryTime());

    var nonNumericStart = certificatePeriod(
        "<start>not-a-time</start><duration><hours>1</hours></duration>");
    assertNull(nonNumericStart.getCertificateValidityStart());
    assertNull(nonNumericStart.getCertificateValidityEnd());
  }

  @Test
  void emptyMicrosecondsArmFallsThroughToHours() throws IOException {
    var certMetadata = certificatePeriod(
        "<start>0</start><duration><microseconds></microseconds><hours>169</hours></duration>");

    assertEquals(IEEE_1609_2_EPOCH, certMetadata.getCertificateValidityStart());
    assertEquals(Instant.parse("2004-01-08T01:00:00Z"), certMetadata.getCertificateValidityEnd());
  }

  @Test
  void unrecognizedDurationUnitLeavesValidityEndUnset() throws IOException {
    var certMetadata = certificatePeriod(
        "<start>0</start><duration><weeks>1</weeks></duration>");

    assertEquals(IEEE_1609_2_EPOCH, certMetadata.getCertificateValidityStart());
    assertNull(certMetadata.getCertificateValidityEnd());
  }

  @Test
  void convertsSparseCertificateMetadataToJson() throws IOException {
    var data = frameWithMetadata(
        "<signatureValidityPeriod><start>428058000</start>"
            + "<duration><hours>1</hours></duration></signatureValidityPeriod>");
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

  private SignedDataMetadata certificateValidity(long startSeconds, String unit, long duration)
      throws IOException {
    return certificatePeriod("<start>" + startSeconds + "</start><duration><" + unit + ">"
        + duration + "</" + unit + "></duration>");
  }

  private SignedDataMetadata certificatePeriod(String validityContents) throws IOException {
    return frameWithMetadata(
        "<signatureValidityPeriod>" + validityContents + "</signatureValidityPeriod>")
        .getMetadata().getCertMetadata();
  }

  private OdeMessageFrameData frameWithMetadata(String insertedMetadata)
      throws IOException {
    String xml;
    try (var input = getClass().getResourceAsStream(
        "/us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml")) {
      if (input == null) {
        throw new IOException("Decoder TIM fixture not found");
      }
      xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    xml = xml.replace("</metadata>", insertedMetadata + "</metadata>");
    return OdeMessageFrameDataCreatorHelper.createOdeMessageFrameData(xml, new XmlMapper());
  }
}
