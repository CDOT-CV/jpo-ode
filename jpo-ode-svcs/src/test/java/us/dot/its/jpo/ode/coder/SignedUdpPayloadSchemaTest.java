package us.dot.its.jpo.ode.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.SignedDataMetadata;
import us.dot.its.jpo.ode.util.JsonUtils;

/**
 * Decoded JSON from signed XER XML payloads and validates against the output schema.
 *
 * <p>The fixtures are ACM decoder XML for those payloads. This test checks the optional
 * {@code certMetadata} object the ODE publishes and validates that JSON against the output schema.
 */
class SignedUdpPayloadSchemaTest {

  private static final DateTimeFormatter ISO_MILLIS = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
      .withZone(ZoneOffset.UTC);

  private static final ObjectMapper SCHEMA_MAPPER = new ObjectMapper();

  @ParameterizedTest
  @MethodSource("signedUdpPayloads")
  void signedUdpPayloadPublishesCertMetadataAndMatchesSchema(SignedPayloadCase testCase)
      throws IOException {
    String xml = readResource("/us/dot/its/jpo/ode/coder/signed-udp/" + testCase.fixture());
    OdeMessageFrameData data =
        OdeMessageFrameDataCreatorHelper.createOdeMessageFrameData(xml, new XmlMapper());

    SignedDataMetadata certMetadata = data.getMetadata().getCertMetadata();
    assertEquals(testCase.psid(), certMetadata.getPsid());
    assertEquals(Instant.parse(testCase.generationTime()), certMetadata.getGenerationTime());
    assertInstant(testCase.expiryTime(), certMetadata.getExpiryTime());
    assertEquals(Instant.parse(testCase.certificateValidityStart()),
        certMetadata.getCertificateValidityStart());
    assertEquals(Instant.parse(testCase.certificateValidityEnd()),
        certMetadata.getCertificateValidityEnd());
    assertTrue(data.getMetadata().isCertPresent());

    JSONObject json = new JSONObject(JsonUtils.toJson(data, false));
    JSONObject publishedCertMetadata = json.getJSONObject("metadata").getJSONObject("certMetadata");
    assertEquals(testCase.psid(), publishedCertMetadata.getLong("psid"));
    assertEquals(ISO_MILLIS.format(certMetadata.getGenerationTime()),
        publishedCertMetadata.getString("generationTime"));
    assertOptionalTimestamp("expiryTime", certMetadata.getExpiryTime(), publishedCertMetadata);
    assertEquals(ISO_MILLIS.format(certMetadata.getCertificateValidityStart()),
        publishedCertMetadata.getString("certificateValidityStart"));
    assertEquals(ISO_MILLIS.format(certMetadata.getCertificateValidityEnd()),
        publishedCertMetadata.getString("certificateValidityEnd"));
    assertEquals(10, json.getJSONObject("metadata").getInt("schemaVersion"));

    assertSchemaValid(testCase.schema(), json.toString());
  }

  private static Stream<SignedPayloadCase> signedUdpPayloads() {
    return Stream.of(
        new SignedPayloadCase("bsm.xml", "/schemas/schema-bsm.json", 32L,
            "2026-05-20T18:08:24.000Z", "2026-05-26T10:00:05.000Z",
            "2026-05-19T09:00:05.000Z", "2026-05-26T10:00:05.000Z"),
        new SignedPayloadCase("map.xml", "/schemas/schema-map.json", 2113687L,
            "2026-05-13T18:12:19.927643Z", "2026-05-13T19:12:19.927643Z",
            "2026-05-08T13:49:31.000Z", "2026-05-15T14:49:31.000Z"),
        new SignedPayloadCase("spat.xml", "/schemas/schema-spat.json", 130L,
            "2026-05-13T17:33:38.753169Z", null,
            "2026-05-08T14:25:19.000Z", "2026-05-15T15:25:19.000Z"),
        new SignedPayloadCase("psm.xml", "/schemas/schema-psm.json", 39L,
            "2026-05-20T17:45:00.909055Z", null,
            "2026-05-14T19:42:18.000Z", "2026-05-21T20:42:18.000Z"),
        new SignedPayloadCase("tim.xml", "/schemas/schema-tim.json", 131L,
            "2026-06-08T23:59:14.142998Z", "2026-06-09T03:59:14.142998Z",
            "2026-06-04T19:42:18.000Z", "2026-06-11T20:42:18.000Z"));
  }

  private static void assertInstant(String expected, Instant actual) {
    if (expected == null) {
      assertNull(actual);
    } else {
      assertEquals(Instant.parse(expected), actual);
    }
  }

  private static void assertOptionalTimestamp(String field, Instant value, JSONObject json) {
    if (value == null) {
      assertFalse(json.has(field));
    } else {
      assertEquals(ISO_MILLIS.format(value), json.getString(field));
    }
  }

  private static void assertSchemaValid(String schemaResource, String json) throws IOException {
    JsonNode schemaNode = SCHEMA_MAPPER.readTree(readResource(schemaResource));
    String dialect = schemaNode.path("$schema").asText("");
    SpecVersion.VersionFlag version = dialect.contains("2020-12")
        ? SpecVersion.VersionFlag.V202012
        : SpecVersion.VersionFlag.V7;
    JsonSchema schema = JsonSchemaFactory.getInstance(version).getSchema(schemaNode);
    JsonNode instance = SCHEMA_MAPPER.readTree(json);
    Set<ValidationMessage> errors = schema.validate(instance);
    assertTrue(errors.isEmpty(), errors::toString);
  }

  private static String readResource(String resource) throws IOException {
    try (InputStream input = SignedUdpPayloadSchemaTest.class.getResourceAsStream(resource)) {
      if (input == null) {
        throw new IOException("Resource not found: " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record SignedPayloadCase(String fixture, String schema, long psid, String generationTime,
      String expiryTime, String certificateValidityStart, String certificateValidityEnd) {
  }
}
