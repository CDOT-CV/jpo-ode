package us.dot.its.jpo.ode.http;

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This class provides a configuration for creating and managing
 * a {@link RestTemplate} bean, which is used for making HTTP requests
 * to external services.
 *
 * <p><strong>NOTE:</strong> As of 5.0 the {@link RestTemplate} class is in maintenance mode, with
 * only minor requests for changes and bugs to be accepted going forward. Please,
 * consider using the {@code org.springframework.web.reactive.client.WebClient}
 * which has a more modern API and supports sync, async, and streaming scenarios.
 * Whenever we the time or resources to update our Spring version,
 * we should replace usages of RestTemplate with WebClient.</p>
 */
@Configuration
public class WebClientConfig {

  /**
   * Creates and configures a {@link RestTemplate} bean with a custom
   * {@link JacksonJsonHttpMessageConverter} to use the provided
   * {@link JsonMapper} for JSON serialization and deserialization.
   *
   * @param mapper the {@link JsonMapper} to be used for configuring
   *               JSON message conversion.
   * @return a configured {@link RestTemplate} instance that includes
   *         the custom JSON message converter.
   */
  @Bean
  public RestTemplate restTemplate(JsonMapper mapper) {
    var template = new RestTemplate();
    final var customConverter = new JacksonJsonHttpMessageConverter(mapper);
    // Replace the default Jackson converter in place, so the String, byte[] and resource
    // converters that precede it still take priority for those types.
    template.getMessageConverters().replaceAll(converter ->
            converter instanceof JacksonJsonHttpMessageConverter
              ? customConverter
              : converter);
    return template;
  }
}
