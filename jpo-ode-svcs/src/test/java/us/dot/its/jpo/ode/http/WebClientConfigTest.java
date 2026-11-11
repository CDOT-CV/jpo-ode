package us.dot.its.jpo.ode.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.security.models.SignatureRequestModel;
import us.dot.its.jpo.ode.security.models.SignatureResultModel;

/**
 * Verifies that the {@link RestTemplate} from {@link WebClientConfig} serializes JSON with the
 * application's configured mapper (from {@link SerializationConfig}) rather than the default
 * Jackson converter that {@code new RestTemplate()} registers on its own, while still accepting
 * responses that contain properties we don't model.
 */
@SpringBootTest(classes = {SerializationConfig.class, WebClientConfig.class})
class WebClientConfigTest {

  private static final String URL = "http://localhost/test";

  @Autowired
  private RestTemplate restTemplate;

  @Test
  void defaultJsonConverterIsReplacedNotSupplemented() {
    var jsonConverters = restTemplate.getMessageConverters().stream()
        .filter(JacksonJsonHttpMessageConverter.class::isInstance)
        .toList();
    HttpMessageConverter<?> selected = restTemplate.getMessageConverters().stream()
        .filter(c -> c.canWrite(SignatureRequestModel.class, MediaType.APPLICATION_JSON))
        .findFirst()
        .orElseThrow();

    assertThat(jsonConverters).hasSize(1);
    assertThat(selected).isSameAs(jsonConverters.getFirst());
  }

  @Test
  void responseWithUnknownPropertiesIsAccepted() {
    var mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(ExpectedCount.once(), requestTo(URL))
        .andRespond(withSuccess(
            "{\"messageSigned\":\"abc\",\"messageExpiry\":1,\"notInModel\":\"x\"}",
            MediaType.APPLICATION_JSON));

    var response = restTemplate.postForEntity(URL, new SignatureRequestModel(),
        SignatureResultModel.class);

    mockServer.verify();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessageSigned()).isEqualTo("abc");
    assertThat(response.getBody().getMessageExpiry()).isEqualTo(1L);
  }

  @Test
  void requestBodyIsSerializedWithConfiguredObjectMapper() {
    // The configured mapper uses NON_NULL inclusion, so the null message must be omitted.
    // Jackson's default mapper would write "message":null.
    var request = new SignatureRequestModel();
    request.setSigValidityOverride(5);

    var mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(ExpectedCount.once(), requestTo(URL))
        .andExpect(content().string("{\"sigValidityOverride\":5}"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    restTemplate.postForEntity(URL, request, String.class);

    mockServer.verify();
  }
}
