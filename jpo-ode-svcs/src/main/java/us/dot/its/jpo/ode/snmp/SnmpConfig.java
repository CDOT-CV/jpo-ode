package us.dot.its.jpo.ode.snmp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnmpConfig {

  @Bean
  public SnmpSessionFactory snmpSessionFactory() {
    return SnmpSession::new;
  }
}
