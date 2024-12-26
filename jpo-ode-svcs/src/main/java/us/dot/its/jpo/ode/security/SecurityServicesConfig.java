package us.dot.its.jpo.ode.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up security services related beans.
 *
 * <p>This class provides a Spring-managed bean of type ISecurityServicesClient
 * that is configured using SecurityServicesProperties and ObjectMapper.
 * The client facilitates interaction with external security services for
 * cryptographic operations such as message signing.</p>
 */
@Configuration
public class SecurityServicesConfig {
  @Bean
  public ISecurityServicesClient securityServicesClient(SecurityServicesProperties securityServicesProperties, ObjectMapper mapper) {
    return new SecurityServicesClientImpl(mapper, securityServicesProperties.getSignatureEndpoint());
  }
}
