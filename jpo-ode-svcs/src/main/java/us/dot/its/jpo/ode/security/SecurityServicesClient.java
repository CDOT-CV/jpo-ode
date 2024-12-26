package us.dot.its.jpo.ode.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import us.dot.its.jpo.ode.security.configuration.SecurityServicesProperties;
import us.dot.its.jpo.ode.security.models.SignatureRequestModel;
import us.dot.its.jpo.ode.security.models.SignatureResultModel;

/**
 * Implementation of the ISecurityServicesClient interface for interacting with security
 * services that provide cryptographic operations such as message signing.
 */
@Slf4j
@Service
public class SecurityServicesClient implements ISecurityServicesClient {

  private final String signatureUri;
  private final RestTemplate restTemplate;

  public SecurityServicesClient(RestTemplate restTemplate, SecurityServicesProperties securityServicesProperties) {
    this.restTemplate = restTemplate;
    this.signatureUri = securityServicesProperties.getSignatureEndpoint();
  }

  @Override
  public SignatureResultModel signMessage(String message, int sigValidityOverride) {
    log.info("Sending data to security services module at {} with validity override {} to be signed", signatureUri, sigValidityOverride);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    var requestBody = new SignatureRequestModel();
    requestBody.setMessage(message);
    requestBody.setSigValidityOverride(sigValidityOverride);

    HttpEntity<SignatureRequestModel> entity = new HttpEntity<>(requestBody, headers);

    log.debug("Data to be signed: {}", entity);
    ResponseEntity<SignatureResultModel> respEntity = restTemplate.postForEntity(signatureUri, entity, SignatureResultModel.class);
    log.debug("Security services module response: {}", respEntity);

    return respEntity.getBody();
  }
}
