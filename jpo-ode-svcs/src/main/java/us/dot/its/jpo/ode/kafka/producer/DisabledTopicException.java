package us.dot.its.jpo.ode.kafka.producer;

public final class DisabledTopicException extends RuntimeException {

  public DisabledTopicException(String topic) {
    super(String.format("Topic %s is disabled.", topic));
  }
}
