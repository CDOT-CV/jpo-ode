package us.dot.its.jpo.ode.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

@Slf4j
public class StringToIntegerConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        try {
            return Integer.valueOf(source);
        } catch (NumberFormatException e) {
            // Handle the case where the source is not a valid integer
            log.warn("Invalid integer value: {}", source);
            return null;
        }
    }
}
