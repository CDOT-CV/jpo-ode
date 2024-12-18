package us.dot.its.jpo.ode.security;

/**
 * Interface for interacting with security services that provide cryptographic operations.
 * It defines methods for signing messages with optional validity overrides.
 */
public interface ISecurityServicesClient {

  public String signMessage(String message, int sigValidityOverride);
}
