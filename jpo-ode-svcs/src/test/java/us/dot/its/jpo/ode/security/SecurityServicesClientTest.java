package us.dot.its.jpo.ode.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.http.WebClientConfig;
import us.dot.its.jpo.ode.security.configuration.SecurityServicesProperties;
import us.dot.its.jpo.ode.security.models.SignatureRequestModel;
import us.dot.its.jpo.ode.security.models.SignatureResultModel;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {
        SerializationConfig.class,
        SecurityServicesClient.class,
        SecurityServicesProperties.class,
        WebClientConfig.class,
    }
)
@EnableConfigurationProperties
class SecurityServicesClientTest {

  @Autowired
  private RestTemplate restTemplate;
  @Autowired
  private SecurityServicesClient securityServicesClient;

  private MockRestServiceServer mockServer;
  private final Clock clock = Clock.fixed(Instant.parse("2024-12-26T23:53:21.120Z"), ZoneId.of("UTC"));
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void beforeEach() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void testSignMessage_WithMockServerSuccessfulResponse() throws JsonProcessingException {
    // Arrange
    String message = "TestMessage";
    SignatureResultModel expectedResult = new SignatureResultModel();
    expectedResult.getResult().setMessageSigned("signed message<%s>".formatted(message));
    expectedResult.getResult().setMessageExpiry(clock.instant().getEpochSecond());

    SignatureRequestModel signatureRequestModel = new SignatureRequestModel();
    signatureRequestModel.setMessage(message);
    var expiryTimeInSeconds = (int) clock.instant().plusSeconds(3600).getEpochSecond();
    signatureRequestModel.setSigValidityOverride(expiryTimeInSeconds);

    mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8090/sign"))
        .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResult), MediaType.APPLICATION_JSON));

    SignatureResultModel result = securityServicesClient.signMessage(message, expiryTimeInSeconds);
    assertEquals(expectedResult, result);
  }
//
//  @Test
//  void testSignMessage_EmptyMessage() {
//    // Arrange
//    RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
//    String signatureUri = "http://example.com/signature";
//    String message = "";
//    int sigValidityOverride = 10;
//    String mockResponse = "{\"result\":\"mockedResponseEmptyMessage\"}";
//
//    SecurityServicesClient client = new SecurityServicesClient(restTemplate, signatureUri);
//    SignatureResultModel expectedResult = new SignatureResultModel();
//
//    ResponseEntity<String> responseEntity = ResponseEntity.ok(mockResponse);
//
//    when(mockRestTemplate.postForEntity(eq(signatureUri), any(HttpEntity.class), eq(String.class)))
//        .thenReturn(responseEntity);
//    when(mapper.convertValue(mockResponse, SignatureResultModel.class))
//        .thenReturn(expectedResult);
//
//    // Act
//    SignatureResultModel result = client.signMessage(message, sigValidityOverride);
//
//    // Assert
//    assertNotNull(result);
//    assertEquals(expectedResult, result);
//  }
//
//  @Test
//  void testSignMessage_InvalidSignatureUri() {
//    // Arrange
//    RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
//    String signatureUri = "http://invaliduri/signature";
//    String message = "TestMessage";
//    int sigValidityOverride = 10;
//
//    SecurityServicesClient client = new SecurityServicesClient(restTemplate, signatureUri);
//
//    when(mockRestTemplate.postForEntity(eq(signatureUri), any(HttpEntity.class), eq(String.class)))
//        .thenThrow(new RuntimeException("Invalid URI"));
//
//    // Act & Assert
//    try {
//      client.signMessage(message, sigValidityOverride);
//    } catch (RuntimeException e) {
//      assertEquals("Invalid URI", e.getMessage());
//    }
//  }
//
//  @Test
//  void testSignMessage_NullResponseBody() {
//    // Arrange
//    RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
//    String signatureUri = "http://example.com/signature";
//    String message = "TestMessage";
//    int sigValidityOverride = 10;
//
//    SecurityServicesClient client = new SecurityServicesClient(restTemplate, signatureUri);
//
//    ResponseEntity<String> responseEntity = ResponseEntity.ok(null);
//
//    when(mockRestTemplate.postForEntity(eq(signatureUri), any(HttpEntity.class), eq(String.class)))
//        .thenReturn(responseEntity);
//
//    // Act & Assert
//    try {
//      client.signMessage(message, sigValidityOverride);
//    } catch (NullPointerException e) {
//      assertNotNull(e.getMessage());
//    }
//  }

}