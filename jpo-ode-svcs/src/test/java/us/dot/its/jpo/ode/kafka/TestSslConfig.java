package us.dot.its.jpo.ode.kafka;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import static org.mockito.Mockito.mock;

/**
 * Test configuration class that provides a mock SslBundles bean for testing purposes. This
 * configuration can be imported by any test that needs SslBundles but doesn't want to configure
 * actual SSL certificates.
 */
@TestConfiguration
public class TestSslConfig {

  @Bean
  public SslBundles sslBundles() {
    // Return a mock SslBundles for testing purposes
    // This provides an empty SSL configuration that won't interfere with tests
    return mock(SslBundles.class);
  }
}
