package us.dot.its.jpo.ode.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        data.getMetadata().getCertMetadata().getCertificateValidityStart().toInstant());
    assertEquals(Instant.parse(expectedEnd),
        data.getMetadata().getCertMetadata().getCertificateValidityEnd().toInstant());
  }
}
