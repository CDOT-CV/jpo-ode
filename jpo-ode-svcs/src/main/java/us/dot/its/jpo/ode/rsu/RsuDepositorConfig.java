package us.dot.its.jpo.ode.rsu;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;

@Configuration
public class RsuDepositorConfig {

  @Bean
  public RsuDepositor rsuDepositor(RsuProperties rsuProperties, SecurityServicesProperties securityServicesProps) {
    var rsuDepositor = new RsuDepositor(rsuProperties, securityServicesProps.getIsRsuSigningEnabled());
    rsuDepositor.start();
    return rsuDepositor;
  }
}
