package us.dot.its.jpo.ode.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
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
   * Configures and returns an {@link ObjectMapper} instance customized for specific serialization
   * and deserialization behavior.
   *
   * @return a customized {@link ObjectMapper} instance supporting specific serialization and
   *         deserialization configurations.
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return JsonMapper.builder()
              .changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
              .withCoercionConfig(LogicalType.Enum,
                      cfg -> cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
              .changeDefaultPropertyInclusion(incl ->
                      incl.withValueInclusion(JsonInclude.Include.NON_NULL)
              )
              // Ensure BigDecimals are serialized consistently as numbers, not strings
              .withConfigOverride(BigDecimal.class,
                      cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)))
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
    return XmlMapper.builder()
            .defaultUseWrapper(true)
            .build();
  }


  @Bean("simpleObjectMapper")
  public ObjectMapper simpleObjectMapper() {
    return new ObjectMapper();
  }

  @Bean("simpleXmlMapper")
  public XmlMapper simpleXmlMapper() {
    return new XmlMapper();
  }
}
