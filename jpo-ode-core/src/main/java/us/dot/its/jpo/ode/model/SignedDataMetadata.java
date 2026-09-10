package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Processed timing metadata extracted from an IEEE 1609.2 signed-data envelope.
 *
 * <p>The {@link Date} values map directly to MongoDB BSON Date values. They are emitted on Kafka
 * as UTC ISO-8601 timestamps and retain MongoDB's millisecond precision.</p>
 */
@Data
@NoArgsConstructor
public class SignedDataMetadata {
  private Long psid;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Date generationTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Date expiryTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Date certificateValidityStart;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
  private Date certificateValidityEnd;
}
