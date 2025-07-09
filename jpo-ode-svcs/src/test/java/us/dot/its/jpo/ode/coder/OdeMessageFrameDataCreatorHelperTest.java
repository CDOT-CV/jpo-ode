package us.dot.its.jpo.ode.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.ode.util.XmlUtils;

/**
 * Unit tests for OdeMessageFrameDataCreatorHelper.
 */
@Slf4j
public class OdeMessageFrameDataCreatorHelperTest {
  private XmlMapper xmlMapper;

  @BeforeEach
  void setUp() {
    xmlMapper = new XmlMapper();
  }

  @Test
  void testDeserializeTimXml2Json() throws JsonMappingException, JsonProcessingException {
    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml");
    String timMFString = XmlUtils.findXmlContentString(baseTestData, "MessageFrame");
    TravelerInformationMessageFrame actualMessageFrame = xmlMapper.readValue(timMFString, TravelerInformationMessageFrame.class);
    assertEquals(-188,
        actualMessageFrame.getValue()
            .getDataFrames().get(0)
            .getRegions().get(1)
            .getDescription().getPath()
            .getOffset().getLl()
            .getNodes().get(0)
            .getAttributes().getDWidth()
            .getValue()
    );
    assertEquals(5, 
        actualMessageFrame.getValue()
            .getDataFrames().get(0)
            .getRegions().get(1)
            .getDescription().getPath()
            .getOffset().getLl()
            .getNodes().get(0)
            .getAttributes().getDisabled().size()
    );
  }

  private String loadFromResource(String resourcePath) {
    String baseTestData;
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new FileNotFoundException("Resource not found: " + resourcePath);
      }
      baseTestData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load test data", e);
    }
    return baseTestData;
  }
}
