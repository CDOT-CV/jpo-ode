package us.dot.its.jpo.ode.traveler;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.Invocation;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

/**
 * Regression tests for the refactor to remove legacy J2735.
 * Tests that depositor can accept the same inputs and outputs are the same before and after,
 * except for a couple xml variants, some corrections, and errors that no longer happen:
 * <ul>
 * <li>Empty elements: encoder XML now writes {@code <advisory/>} instead of
 *     {@code <advisory></advisory>}.</li>
 * <li>Geometry extent: now sent as the enum, e.g. {@code <extent><useFor500meters/></extent>}, and
 *     OdeTimJson now includes extent.</li>
 * <li>Circle units: "1": legacy wrote an empty, invalid {@code <units/>}. It now writes cm2-5, and
 *     OdeTimJson gains units.</li>
 *  <p>Lowercase packetID: it now goes to the encoder in uppercase hex; OdeTimJson was already
 *     identical.</p>
 *  <p>Unsupported regions still get a 400, with new message text. Unknown description values and
 *     shapepoint/regionPoint old-region areas now read "Unsupported region description: …" or
 *     "Unsupported oldRegion area…"; the old message named a deleted class. An unknown content value
 *     still returns 200, as before.
 * <p>Two removed 400s: "Error converting to encodable TravelerInputData." and "Non-compliant
 *     fields in TIM" could no longer be triggered through REST.</p>
 * </ul>
 */
class TimGoldenMasterTest {

  private static final Path GOLDEN = Paths.get("src/test/resources/us/dot/its/jpo/ode/traveler/golden");
  private static final Path EXPECTED = GOLDEN.resolve("expected");
  private static final boolean CAPTURE = Boolean.getBoolean("golden.capture");
  private static final String TIM_JSON_TOPIC = "timJson";
  private static final String ENCODER_INPUT_TOPIC = "encoderInput";

  private static Clock prevClock;

  private record Outputs(String response, String timJson, String encoderXml) {
  }

  @BeforeAll
  static void fixClock() {
    prevClock = DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
  }

  @AfterAll
  static void restoreClock() {
    DateTimeUtils.setClock(prevClock);
  }

  static Stream<String> goldenCases() throws IOException {
    try (Stream<Path> inputs = Files.list(GOLDEN.resolve("inputs"))) {
      return inputs.map(input -> input.getFileName().toString().replace(".json", ""))
          .sorted()
          .toList()
          .stream();
    }
  }

  /**
   * Deposits each request in {@code golden/inputs} and compares the REST response, OdeTimJson
   * message and Asn1EncoderInput message with the files in {@code golden/expected}.
   *
   * <p>Run with {@code -Dgolden.capture=true} to overwrite the expected files with the current
   * outputs instead of comparing (no assertions run). Use it to add a case or record an intended
   * behavior change, then review the {@code git diff} of {@code golden/expected}.</p>
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("goldenCases")
  void timDepositOutputsMatchGoldenFiles(String name) throws IOException {
    String body = Files.readString(GOLDEN.resolve("inputs").resolve(name + ".json"), StandardCharsets.UTF_8);
    Outputs actual = deposit(body);

    Path responseFile = EXPECTED.resolve(name + ".response.txt");
    Path jsonFile = EXPECTED.resolve(name + ".tim.json");
    Path xmlFile = EXPECTED.resolve(name + ".encoder.xml");
    if (CAPTURE) {
      Files.createDirectories(EXPECTED);
      Files.writeString(responseFile, actual.response() + "\n", StandardCharsets.UTF_8);
      Files.writeString(jsonFile, actual.timJson(), StandardCharsets.UTF_8);
      Files.writeString(xmlFile, actual.encoderXml(), StandardCharsets.UTF_8);
      return;
    }

    assertEquals(Files.readString(responseFile, StandardCharsets.UTF_8).trim(), actual.response(),
        "REST response");
    assertTimJson(Files.readString(jsonFile, StandardCharsets.UTF_8), actual.timJson());
    assertEncoderXml(Files.readString(xmlFile, StandardCharsets.UTF_8), actual.encoderXml());
  }

  @SuppressWarnings("unchecked")
  private static Outputs deposit(String body) {
    Asn1CoderTopics asn1CoderTopics = new Asn1CoderTopics();
    asn1CoderTopics.setEncoderInput(ENCODER_INPUT_TOPIC);
    JsonTopics jsonTopics = new JsonTopics();
    jsonTopics.setTim(TIM_JSON_TOPIC);
    KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    TimDepositController controller = new TimDepositController(asn1CoderTopics, jsonTopics,
        new TimIngestTrackerProperties(), new SecurityServicesProperties(), kafkaTemplate,
        new XmlMapper());

    String response;
    try {
      ResponseEntity<String> r = controller.postTim(body);
      response = r.getStatusCode().value() + " " + r.getBody();
    } catch (RuntimeException e) {
      response = "EXCEPTION " + e.getClass().getName();
    }

    String timJson = "";
    String encoderXml = "";
    for (Invocation invocation : mockingDetails(kafkaTemplate).getInvocations()) {
      if (invocation.getMethod().getName().equals("send") && invocation.getArguments().length == 3) {
        String topic = (String) invocation.getArguments()[0];
        String value = normalizeStreamId((String) invocation.getArguments()[2]);
        if (topic.equals(TIM_JSON_TOPIC)) {
          timJson = value;
        } else if (topic.equals(ENCODER_INPUT_TOPIC)) {
          encoderXml = value;
        }
      }
    }
    return new Outputs(response, timJson, encoderXml);
  }

  private static String normalizeStreamId(String value) {
    return value.replaceAll("\"streamId\":\"[^\"]*\"", "\"streamId\":\"STREAM\"")
        .replaceAll("<streamId>[^<]*</streamId>", "<streamId>STREAM</streamId>");
  }

  private static void assertTimJson(String expected, String actual) {
    if (expected.isBlank() || actual.isBlank()) {
      assertEquals(expected.strip(), actual.strip(), "OdeTimJson message");
      return;
    }
    assertJsonEquals(expected, actual);
  }

  private static void assertEncoderXml(String expected, String actual) {
    if (expected.isBlank() || actual.isBlank()) {
      assertEquals(expected.strip(), actual.strip(), "Asn1EncoderInput message");
      return;
    }
    Diff diff = DiffBuilder.compare(expected)
        .withTest(actual)
        .ignoreWhitespace()
        .checkForSimilar()
        .build();
    assertFalse(diff.hasDifferences(), () -> "Asn1EncoderInput message differs: " + diff);
  }
}
