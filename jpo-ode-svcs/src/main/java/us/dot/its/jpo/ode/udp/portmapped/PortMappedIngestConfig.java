package us.dot.its.jpo.ode.udp.portmapped;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ode.port-mapped-ingest")
public class PortMappedIngestConfig {

  private List<PortMappedIngestSource> sources;

  @Data
  public static class PortMappedIngestSource {
    private String intersectionName;
    private String intersectionId;
    private String originIp;

    private int port;

    private String type;
  }
}
