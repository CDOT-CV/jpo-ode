package us.dot.its.jpo.ode.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityServicesConfig {
  @Bean
  public ISecurityServicesClient securityServicesClient(SecurityServicesProperties securityServicesProperties) {
    return new SecurityServicesClientImpl(securityServicesProperties.getSignatureEndpoint());
  }
}
