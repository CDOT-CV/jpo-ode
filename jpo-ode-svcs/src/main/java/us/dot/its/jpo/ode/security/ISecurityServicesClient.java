package us.dot.its.jpo.ode.security;

import us.dot.its.jpo.ode.security.models.SignatureResultModel;

/**
 * Interface for interacting with security services that provide cryptographic operations.
 * It defines methods for signing messages with optional validity overrides.
 */
public interface ISecurityServicesClient {

  public SignatureResultModel signMessage(String message, int sigValidityOverride);
}
