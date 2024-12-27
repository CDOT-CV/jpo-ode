package us.dot.its.jpo.ode.security.models;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of a message signing operation provided by a security
 * service. This model encapsulates the signed message and its expiration
 * information.
 *
 * <p>The signing operation is performed by the security service and the result
 * is returned in this structure. The {@code Result} inner class contains specific
 * details about the signed message.</p>
 */
@Data
@NoArgsConstructor
public class SignatureResultModel {
  private Result result = new Result();

  /**
   * Represents the result of a cryptographic operation that includes a signed message
   * and its associated expiration information.
   *
   * <p>This class is typically used to encapsulate the output of a message signing
   * process performed by a security service. The signed message is stored in the
   * {@code messageSigned} field, while the expiration time for the message, expressed
   * in seconds, is stored in the {@code messageExpiry} field.</p>
   */
  @Data
  @NoArgsConstructor
  public static class Result {
    private String messageSigned;
    // messageExpiry is in seconds
    private Long messageExpiry;
  }
}
