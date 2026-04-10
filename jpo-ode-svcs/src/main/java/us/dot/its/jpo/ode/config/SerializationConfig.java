package us.dot.its.jpo.ode.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
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
  public JsonMapper jsonMapper() {
    return JsonMapper.builder()
              .changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
              .withCoercionConfig(tools.jackson.databind.type.LogicalType.Enum,
                      cfg -> cfg.setCoercion(
                              tools.jackson.databind.cfg.CoercionInputShape.EmptyString, tools.jackson.databind.cfg.CoercionAction.AsNull))
              .changeDefaultPropertyInclusion(incl ->
                      incl.withValueInclusion(JsonInclude.Include.NON_NULL)
              )
              // Ensure BigDecimals are serialized consistently as numbers, not strings
              .withConfigOverride(BigDecimal.class,
                      cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)))
              .build();
  }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper legacyObjectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.coercionConfigFor(LogicalType.Enum)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        // Ensure BigDecimals are serialized consistently as numbers not strings
        mapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER));
        // Only serialize non-null fields
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
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

    @Bean("simpleLegacyXmlMapper")
    public com.fasterxml.jackson.dataformat.xml.XmlMapper simpleLegacyXmlMapper() {
        return new com.fasterxml.jackson.dataformat.xml.XmlMapper();
    }
}
