package us.dot.its.jpo.ode.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.dataformat.xml.XmlMapper;
import java.math.BigDecimal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
    return JsonMapper.builderWithJackson2Defaults()
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        // Ignore JSON properties our models don't declare instead of failing. Input comes from
        // external sources (e.g. jpo-security-svcs responses, Kafka messages from other services)
        // that may include extra fields, now or after they add them. Failing would turn that
        // into an error, e.g. breaking TIM signing. Matches xmlMapper() below.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
        .withCoercionConfig(LogicalType.Enum,
            cfg -> cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
        // Ensure BigDecimals are serialized consistently as numbers not strings
        .withConfigOverride(BigDecimal.class,
            o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)))
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
    var builder = XmlMapper.builderWithJackson2Defaults();
    builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    builder.defaultUseWrapper(true);
    builder.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    return builder.build();
  }


  @Bean("simpleObjectMapper")
  public ObjectMapper simpleObjectMapper() {
    return JsonMapper.builderWithJackson2Defaults()
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build();
  }

  @Bean("simpleXmlMapper")
  public XmlMapper simpleXmlMapper() {
    return XmlMapper.builderWithJackson2Defaults()
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build();
  }
}
