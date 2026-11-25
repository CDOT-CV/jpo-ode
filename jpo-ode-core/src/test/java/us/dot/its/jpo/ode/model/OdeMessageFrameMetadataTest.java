package us.dot.its.jpo.ode.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import us.dot.its.jpo.ode.util.JsonUtils;

/**
 * Unit tests for the JSON property names of {@link OdeMessageFrameMetadata}.
 *
 * <p>The {@code isCertPresent} field doesn't follow the JavaBeans naming convention, so its
 * Lombok accessors ({@code isCertPresent()} and {@code setCertPresent()}) have the implicit
 * property name {@code certPresent}.  The output schemas use {@code isCertPresent}.  This only
 * works while Lombok copies the field's {@code @JsonProperty} to the accessors, which the root
 * {@code lombok.config} turns on.
 */
class OdeMessageFrameMetadataTest {

  private final ObjectMapper mapper = JsonUtils.baseMapperBuilder().build();

  @Test
  void serializesCertPresentOnlyAsIsCertPresent() {
    OdeMessageFrameMetadata metadata = new OdeMessageFrameMetadata();
    metadata.setCertPresent(true);

    JsonNode json = mapper.valueToTree(metadata);

    assertTrue(json.has("isCertPresent"), "expected isCertPresent in " + json);
    assertTrue(json.get("isCertPresent").asBoolean());
    assertFalse(json.has("certPresent"), "unexpected certPresent in " + json);
  }

  @Test
  void deserializesIsCertPresent() {
    OdeMessageFrameMetadata metadata =
        mapper.readValue("{\"isCertPresent\":true}", OdeMessageFrameMetadata.class);

    assertTrue(metadata.isCertPresent());

    JsonNode json = mapper.valueToTree(metadata);
    assertTrue(json.get("isCertPresent").asBoolean());
    assertFalse(json.has("certPresent"), "unexpected certPresent in " + json);
  }
}
