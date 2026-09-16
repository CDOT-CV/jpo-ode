package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Processed timing metadata extracted from an IEEE 1609.2 signed-data envelope.
 *
 * <p>The {@link Instant} values are database-agnostic UTC timestamps. They are emitted on Kafka
 * as ISO-8601 timestamps with millisecond precision.</p>
 */
@Data
@NoArgsConstructor
public class SignedDataMetadata {
  private Long psid;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant generationTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant expiryTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant certificateValidityStart;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Instant certificateValidityEnd;
}
