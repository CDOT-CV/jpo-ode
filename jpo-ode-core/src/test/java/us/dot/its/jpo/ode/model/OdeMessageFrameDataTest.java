package us.dot.its.jpo.ode.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the OdeMessageFrameData class.
 */
public class OdeMessageFrameDataTest {

  private static final String SAMPLE_SDSM_FILE = "src/test/resources/json/sample-sdsm.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Test proper serialization and deserialization of SDSM data.

   * @throws IOException if there is an error reading the test file
   */
  @Test
  public void testSdsmSerializationDeserialization() throws IOException {
    // Read the sample JSON file
    String jsonContent = new String(Files.readAllBytes(Paths.get(SAMPLE_SDSM_FILE)));

    // Deserialize JSON to OdeMessageFrameData
    OdeMessageFrameData messageFrame =
        objectMapper.readValue(jsonContent, OdeMessageFrameData.class);

    // Verify metadata
    assertNotNull(messageFrame.getMetadata());
    assertEquals("us.dot.its.jpo.ode.model.OdeMessageFramePayload",
        messageFrame.getMetadata().getPayloadType());

    // Verify payload
    assertNotNull(messageFrame.getPayload());
    assertNotNull(messageFrame.getPayload().getData());

    // Verify basic SDSM structure
    JsonNode data = objectMapper.valueToTree(messageFrame.getPayload().getData());
    assertNotNull(data.get("messageId"));
    assertEquals(41, data.get("messageId").asInt());

    JsonNode sdsm = data.get("value").get("SensorDataSharingMessage");
    assertNotNull(sdsm);
    assertEquals(10, sdsm.get("msgCnt").asInt());
    assertEquals("010C0C0A", sdsm.get("sourceID").asText());
  }

  @Test
  public void testCertificatePresenceUsesOnlyIsCertPresentJsonProperty() throws IOException {
    OdeMessageFrameMetadata metadata = new OdeMessageFrameMetadata();
    metadata.setCertPresent(true);

    JsonNode json = objectMapper.valueToTree(metadata);
    assertTrue(json.get("isCertPresent").asBoolean());
    assertFalse(json.has("certPresent"));

    OdeMessageFrameMetadata deserialized = objectMapper.readValue(
        "{\"isCertPresent\":true}", OdeMessageFrameMetadata.class);
    assertTrue(deserialized.isCertPresent());
  }

  @Test
  public void testCertMetadataSerializationDeserialization() throws IOException {
    String json = "{\"certMetadata\":{\"psid\":32,"
        + "\"generationTime\":\"2026-05-07T18:26:51.000Z\","
        + "\"expiryTime\":\"2026-05-12T10:00:05.000Z\","
        + "\"certificateValidityStart\":\"2026-05-05T09:00:05.000Z\","
        + "\"certificateValidityEnd\":\"2026-05-12T10:00:05.000Z\"}}";

    OdeMessageFrameMetadata metadata = objectMapper.readValue(json, OdeMessageFrameMetadata.class);

    assertEquals(32L, metadata.getCertMetadata().getPsid());
    JsonNode serializedCertMetadata = objectMapper.valueToTree(metadata).get("certMetadata");
    assertEquals(32, serializedCertMetadata.get("psid").asInt());
    assertEquals("2026-05-07T18:26:51.000Z",
        serializedCertMetadata.get("generationTime").asText());
    assertEquals("2026-05-12T10:00:05.000Z",
        serializedCertMetadata.get("expiryTime").asText());
    assertEquals("2026-05-05T09:00:05.000Z",
        serializedCertMetadata.get("certificateValidityStart").asText());
    assertEquals("2026-05-12T10:00:05.000Z",
        serializedCertMetadata.get("certificateValidityEnd").asText());
  }

}
