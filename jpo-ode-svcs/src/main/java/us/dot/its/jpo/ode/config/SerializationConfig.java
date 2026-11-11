package us.dot.its.jpo.ode.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.XmlUtils;

/**
 * Configuration class for customizing serialization settings using JSON and XML serialization.
 */
@Configuration
public class SerializationConfig {

  /**
   * Configures and returns a {@link JsonMapper} instance customized for specific serialization
   * and deserialization behavior.
   *
   * <p>The return type must be {@link JsonMapper} rather than {@link ObjectMapper}: Spring Boot's
   * {@code JacksonAutoConfiguration} registers its own {@code @Primary} {@code JsonMapper} unless a
   * bean of type {@code JsonMapper} already exists, and it decides that from the declared return
   * type of this method.
   *
   * @return a customized {@link JsonMapper} instance supporting specific serialization and
   *         deserialization configurations.
   */
  @Bean
  @Primary
  public JsonMapper objectMapper() {
    // Shared settings (Jackson 2 defaults, field visibility, ignore unknown properties, enum and
    // BigDecimal handling)
    return JsonUtils.baseMapperBuilder()
        // Only serialize non-null fields
        .changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(Include.NON_NULL).withContentInclusion(Include.NON_NULL))
        .build();
  }

  /**
   * Configures and returns an {@link XmlMapper} instance with customized serialization
   * and deserialization behavior for XML processing.
   *
   * @return a customized {@link XmlMapper} instance with specific configurations,
   *         including disabled failure on unknown properties and default use of wrappers.
   */
  @Bean
  public XmlMapper xmlMapper() {
    // Same settings as XmlUtils' mapper, so all ODE XML is produced consistently
    return XmlUtils.baseXmlMapperBuilder().build();
  }

  @Bean("simpleXmlMapper")
  public XmlMapper simpleXmlMapper() {
    return XmlMapper.builderWithJackson2Defaults()
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build();
  }
}
