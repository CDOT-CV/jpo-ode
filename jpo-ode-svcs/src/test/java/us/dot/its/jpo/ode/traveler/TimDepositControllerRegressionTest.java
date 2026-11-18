package us.dot.its.jpo.ode.traveler;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

/**
 * Regression test for the TIM depositor REST endpoint, {@code POST /tim}.
 *
 * <h2>What it checks</h2>
 * <p>Each case is a request body in {@code tim-deposit-regression/requests/<case>.json}. The test
 * passes the body to {@link TimDepositController#postTim}, called directly (no HTTP server, no
 * Spring context), with a mocked {@link KafkaTemplate} that records what is published. It then
 * compares the three things the depositor produces with files in
 * {@code tim-deposit-regression/expected/}:</p>
 * <ol>
 *   <li>{@code <case>.response.txt}: the HTTP status code and body returned by {@code postTim},
 *       e.g. {@code 200 {"success":"true"}}. If {@code postTim} throws, it holds
 *       {@code EXCEPTION <exception class>}.</li>
 *   <li>{@code <case>.OdeTimJson.json}: the message published to the OdeTimJson topic, compared as
 *       JSON.</li>
 *   <li>{@code <case>.Asn1EncoderInput.xml}: the message published to the Asn1EncoderInput topic,
 *       the input to the ASN.1 encoder, compared as XML ignoring whitespace.</li>
 * </ol>
 * <p>An empty expected file means nothing is published to that topic, as for a rejected request.
 * The clock is fixed and the random {@code streamId} is replaced with {@code STREAM}, so the
 * outputs are repeatable.</p>
 *
 * <h2>Where the expected files come from</h2>
 * <p>They were recorded from the legacy implementation on {@code cdot/develop}, before the legacy
 * J2735 POJOs were removed, and re-recorded only for the intended differences listed below. A
 * passing run shows that the depositor accepts the same requests and produces the same outputs as
 * before the refactor.</p>
 *
 * <p>The case name prefix says where the request came from:</p>
 * <ul>
 *   <li>{@code controller_}: request bodies from {@code TimDepositControllerTest}, named after its
 *       test methods.</li>
 *   <li>{@code converter_}: request bodies from the deleted
 *       {@code TravelerMessageFromHumanToAsnConverterTest}.</li>
 *   <li>{@code data_}: sample requests from {@code data/TIM_Message_Testing_Files}.</li>
 *   <li>{@code resource_}: other request files under {@code src/test/resources}.</li>
 *   <li>{@code crafted_}: written for this test. Each varies one thing, such as a field value, an
 *       enum given by name or by number, or a node offset at a size boundary.</li>
 * </ul>
 *
 * <h2>Intended differences from cdot/develop</h2>
 * <ul>
 *   <li>Empty elements: the encoder XML now writes {@code <advisory/>} instead of
 *       {@code <advisory></advisory>}.</li>
 *   <li>Geometry extent: now sent as the enum, e.g. {@code <extent><useFor500meters/></extent>},
 *       and OdeTimJson now includes the extent.</li>
 *   <li>Circle units {@code "1"}: legacy wrote an empty, invalid {@code <units/>}. It now writes
 *       {@code cm2-5}, and OdeTimJson now includes the units.</li>
 *   <li>Lowercase packetID: now sent to the encoder as uppercase hex. OdeTimJson was already the
 *       same.</li>
 *   <li>Unsupported regions still get a 400, with new message text. An unknown description, or a
 *       shapepoint or regionPoint oldRegion area, now reads "Unsupported region description: ..."
 *       or "Unsupported oldRegion area..."; the old message named a deleted class. An unknown
 *       content value still returns 200.</li>
 *   <li>Two 400 responses no longer exist, because nothing sent through REST could trigger them:
 *       "Error converting to encodable TravelerInputData." and "Non-compliant fields in TIM".</li>
 * </ul>
 *
 * <h2>Recording expected files</h2>
 * <p>Run with {@code -DtimDepositRegression.record=true} to write the current outputs to
 * {@code expected/} instead of comparing them. Use it to add a case (add the request file first) or
 * to record an intended behavior change, then review the {@code git diff} of {@code expected/}.</p>
 * <pre>
 * mvn -pl jpo-ode-svcs test -Dtest=TimDepositControllerRegressionTest -DtimDepositRegression.record=true
 * </pre>
 */
@DisplayName("POST /tim: responses and published Kafka messages match the recorded outputs")
class TimDepositControllerRegressionTest {

  private static final String RECORD_PROPERTY = "timDepositRegression.record";

  private static final Path CASES =
      Paths.get("src/test/resources/us/dot/its/jpo/ode/traveler/tim-deposit-regression");
  private static final Path REQUESTS = CASES.resolve("requests");
  private static final Path EXPECTED = CASES.resolve("expected");

  private static final String ODE_TIM_JSON_TOPIC = "topic.OdeTimJson";
  private static final String ASN1_ENCODER_INPUT_TOPIC = "topic.Asn1EncoderInput";

  /** What the depositor produced for one request. A topic with no message is "". */
  private record DepositResult(String response, String odeTimJsonMessage,
                               String asn1EncoderInputMessage) {
  }

  /** Each case is deposited once and shared by the three comparison tests. */
  private static final Map<String, DepositResult> RESULTS = new HashMap<>();

  private static Clock prevClock;

  @BeforeAll
  static void fixClock() {
    prevClock = DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
  }

  @AfterAll
  static void restoreClock() {
    DateTimeUtils.setClock(prevClock);
  }

  /** Case names: the request file names without {@code .json}, sorted. */
  static Stream<String> caseNames() throws IOException {
    try (Stream<Path> requests = Files.list(REQUESTS)) {
      return requests.map(request -> request.getFileName().toString().replace(".json", ""))
          .sorted()
          .toList()
          .stream();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("caseNames")
  @DisabledIfSystemProperty(named = RECORD_PROPERTY, matches = "true")
  @DisplayName("returns the recorded HTTP status and body")
  void postTimReturnsRecordedResponse(String caseName) throws IOException {
    String expected = readExpected(caseName, ".response.txt").trim();
    assertEquals(expected, depositResult(caseName).response(), "HTTP response from postTim");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("caseNames")
  @DisabledIfSystemProperty(named = RECORD_PROPERTY, matches = "true")
  @DisplayName("publishes the recorded message to the OdeTimJson topic")
  void postTimPublishesRecordedOdeTimJsonMessage(String caseName) throws IOException {
    String expected = readExpected(caseName, ".OdeTimJson.json");
    String actual = depositResult(caseName).odeTimJsonMessage();
    if (expected.isBlank() || actual.isBlank()) {
      assertEquals(expected.strip(), actual.strip(), "OdeTimJson message (empty = not published)");
      return;
    }
    assertJsonEquals(expected, actual);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("caseNames")
  @DisabledIfSystemProperty(named = RECORD_PROPERTY, matches = "true")
  @DisplayName("publishes the recorded message to the Asn1EncoderInput topic")
  void postTimPublishesRecordedAsn1EncoderInputMessage(String caseName) throws IOException {
    String expected = readExpected(caseName, ".Asn1EncoderInput.xml");
    String actual = depositResult(caseName).asn1EncoderInputMessage();
    if (expected.isBlank() || actual.isBlank()) {
      assertEquals(expected.strip(), actual.strip(),
          "Asn1EncoderInput message (empty = not published)");
      return;
    }
    Diff diff = DiffBuilder.compare(expected)
        .withTest(actual)
        .ignoreWhitespace()
        .checkForSimilar()
        .build();
    assertFalse(diff.hasDifferences(), () -> "Asn1EncoderInput message differs: " + diff);
  }

  /** Not a test: writes the current outputs to {@code expected/}. See the class Javadoc. */
  @ParameterizedTest(name = "{0}")
  @MethodSource("caseNames")
  @EnabledIfSystemProperty(named = RECORD_PROPERTY, matches = "true")
  @DisplayName("records the current outputs as the expected files")
  void recordExpectedFiles(String caseName) throws IOException {
    DepositResult result = depositResult(caseName);
    Files.createDirectories(EXPECTED);
    Files.writeString(EXPECTED.resolve(caseName + ".response.txt"), result.response() + "\n",
        StandardCharsets.UTF_8);
    Files.writeString(EXPECTED.resolve(caseName + ".OdeTimJson.json"), result.odeTimJsonMessage(),
        StandardCharsets.UTF_8);
    Files.writeString(EXPECTED.resolve(caseName + ".Asn1EncoderInput.xml"),
        result.asn1EncoderInputMessage(), StandardCharsets.UTF_8);
    assertAll("recorded files read back unchanged",
        () -> assertEquals(result.response() + "\n", readExpected(caseName, ".response.txt")),
        () -> assertEquals(result.odeTimJsonMessage(), readExpected(caseName, ".OdeTimJson.json")),
        () -> assertEquals(result.asn1EncoderInputMessage(),
            readExpected(caseName, ".Asn1EncoderInput.xml")));
  }

  private static String readExpected(String caseName, String suffix) throws IOException {
    return Files.readString(EXPECTED.resolve(caseName + suffix), StandardCharsets.UTF_8);
  }

  private static DepositResult depositResult(String caseName) throws IOException {
    DepositResult result = RESULTS.get(caseName);
    if (result == null) {
      String requestBody = Files.readString(REQUESTS.resolve(caseName + ".json"), StandardCharsets.UTF_8);
      result = deposit(requestBody);
      RESULTS.put(caseName, result);
    }
    return result;
  }

  /** Posts the request body to a new controller and collects what it returns and publishes. */
  @SuppressWarnings("unchecked")
  private static DepositResult deposit(String requestBody) {
    Asn1CoderTopics asn1CoderTopics = new Asn1CoderTopics();
    asn1CoderTopics.setEncoderInput(ASN1_ENCODER_INPUT_TOPIC);
    JsonTopics jsonTopics = new JsonTopics();
    jsonTopics.setTim(ODE_TIM_JSON_TOPIC);
    KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    TimDepositController controller = new TimDepositController(asn1CoderTopics, jsonTopics,
        new TimIngestTrackerProperties(), new SecurityServicesProperties(), kafkaTemplate,
        new XmlMapper());

    String response;
    try {
      ResponseEntity<String> entity = controller.postTim(requestBody);
      response = entity.getStatusCode().value() + " " + entity.getBody();
    } catch (RuntimeException e) {
      response = "EXCEPTION " + e.getClass().getName();
    }
    return new DepositResult(response,
        messagePublishedTo(kafkaTemplate, ODE_TIM_JSON_TOPIC),
        messagePublishedTo(kafkaTemplate, ASN1_ENCODER_INPUT_TOPIC));
  }

  /**
   * The value of the last {@code kafkaTemplate.send(topic, key, value)} call for the topic, with the
   * random streamId replaced; "" if nothing was sent to it.
   */
  private static String messagePublishedTo(KafkaTemplate<String, String> kafkaTemplate, String topic) {
    return mockingDetails(kafkaTemplate).getInvocations().stream()
        .filter(call -> call.getMethod().getName().equals("send")
            && call.getArguments().length == 3
            && topic.equals(call.getArgument(0)))
        .map(call -> (String) call.getArgument(2))
        .reduce((earlier, later) -> later)
        .map(TimDepositControllerRegressionTest::replaceStreamId)
        .orElse("");
  }

  private static String replaceStreamId(String message) {
    return message.replaceAll("\"streamId\":\"[^\"]*\"", "\"streamId\":\"STREAM\"")
        .replaceAll("<streamId>[^<]*</streamId>", "<streamId>STREAM</streamId>");
  }
}
