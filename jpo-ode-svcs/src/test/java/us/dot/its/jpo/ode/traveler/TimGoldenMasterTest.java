package us.dot.its.jpo.ode.traveler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
 * Tests that depositor outputs are the same before and after, except for a couple xml variants,
 * some corrections, and errors that no longer happen:
 * <ul>
 * <li>Empty elements: encoder XML now writes <advisory/> instead of <advisory></advisory>.</li>
 * <li>Geometry extent: now sent as the enum, e.g. <extent><useFor500meters/></extent>, and
 *     OdeTimJson now includes extent.</li>
 * <li>Circle units: "1": legacy wrote an empty, invalid <units/>. It now writes cm2-5, and
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
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  @SuppressWarnings("unchecked")
  void timDepositOutputsMatchGoldenFiles() throws IOException {
    boolean capture = Boolean.getBoolean("golden.capture");
    Path expectedDir = GOLDEN.resolve("expected");
    Files.createDirectories(expectedDir);
    List<String> failures = new ArrayList<>();
    Clock prevClock = DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    try (Stream<Path> inputs = Files.list(GOLDEN.resolve("inputs"))) {
      for (Path input : inputs.sorted().toList()) {
        String name = input.getFileName().toString().replace(".json", "");
        String body = Files.readString(input, StandardCharsets.UTF_8);

        Asn1CoderTopics asn1CoderTopics = new Asn1CoderTopics();
        asn1CoderTopics.setEncoderInput("encoderInput");
        JsonTopics jsonTopics = new JsonTopics();
        jsonTopics.setTim("timJson");
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
            String value = normalize((String) invocation.getArguments()[2]);
            if (topic.equals("timJson")) {
              timJson = value;
            } else if (topic.equals("encoderInput")) {
              encoderXml = value;
            }
          }
        }

        Path responseFile = expectedDir.resolve(name + ".response.txt");
        Path jsonFile = expectedDir.resolve(name + ".tim.json");
        Path xmlFile = expectedDir.resolve(name + ".encoder.xml");
        if (capture) {
          Files.writeString(responseFile, response + "\n", StandardCharsets.UTF_8);
          Files.writeString(jsonFile, timJson, StandardCharsets.UTF_8);
          Files.writeString(xmlFile, encoderXml, StandardCharsets.UTF_8);
          continue;
        }
        String expectedResponse = Files.readString(responseFile, StandardCharsets.UTF_8).trim();
        if (!expectedResponse.equals(response)) {
          failures.add(name + " response: expected <" + expectedResponse + "> but was <" + response + ">");
        }
        String expectedJson = Files.readString(jsonFile, StandardCharsets.UTF_8);
        if (!sameJson(expectedJson, timJson)) {
          failures.add(name + " tim json differs:\n  expected " + expectedJson + "\n  actual   " + timJson);
        }
        String expectedXml = Files.readString(xmlFile, StandardCharsets.UTF_8);
        String xmlDiff = xmlDifference(expectedXml, encoderXml);
        if (xmlDiff != null) {
          failures.add(name + " encoder xml differs: " + xmlDiff);
        }
      }
    } finally {
      DateTimeUtils.setClock(prevClock);
    }
    Assertions.assertTrue(failures.isEmpty(), String.join("\n", failures));
  }

  private static String normalize(String value) {
    return value.replaceAll("\"streamId\":\"[^\"]*\"", "\"streamId\":\"STREAM\"")
        .replaceAll("<streamId>[^<]*</streamId>", "<streamId>STREAM</streamId>");
  }

  private static boolean sameJson(String expected, String actual) throws IOException {
    if (expected.isEmpty() || actual.isEmpty()) {
      return expected.equals(actual);
    }
    return MAPPER.readTree(expected).equals(MAPPER.readTree(actual));
  }

  private static String xmlDifference(String expected, String actual) {
    if (expected.isEmpty() || actual.isEmpty()) {
      return expected.equals(actual) ? null : "expected <" + expected + "> but was <" + actual + ">";
    }
    Diff diff = DiffBuilder.compare(expected).withTest(actual).ignoreWhitespace().checkForSimilar().build();
    return diff.hasDifferences() ? diff.toString() + "\n  actual " + actual : null;
  }
}
