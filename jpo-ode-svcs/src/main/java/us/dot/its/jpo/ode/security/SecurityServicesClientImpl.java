package us.dot.its.jpo.ode.security;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the ISecurityServicesClient interface for interacting with security
 * services that provide cryptographic operations such as message signing.
 *
 */
@Slf4j
public class SecurityServicesClientImpl implements ISecurityServicesClient {

  private final String signatureUri;

  public SecurityServicesClientImpl(String signatureUri) {
    this.signatureUri = signatureUri;
  }

  @Override
  public String signMessage(String message, int sigValidityOverride) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> map = new HashMap<>();
    map.put("message", message);
    map.put("sigValidityOverride", Integer.toString(sigValidityOverride));

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);
    RestTemplate template = new RestTemplate();

    log.info("Sending data to security services module with validity override at {} to be signed",
        signatureUri);
    log.debug("Data to be signed: {}", entity);

    ResponseEntity<String> respEntity = template.postForEntity(signatureUri, entity, String.class);

    log.debug("Security services module response: {}", respEntity);

    return respEntity.getBody();
  }
}
