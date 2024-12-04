package us.dot.its.jpo.ode.kafka.producer;

import java.util.Set;
import us.dot.its.jpo.ode.model.OdeObject;

/**
 * An implementation of {@link AbstractDisabledTopicsProducerInterceptor} for KafkaTemplates
 * responsible for producing data with String keys and {@link OdeObject} values. See
 * {@link AbstractDisabledTopicsProducerInterceptor} for more detail
 */
public class DisabledTopicsOdeObjectProducerInterceptor
    extends AbstractDisabledTopicsProducerInterceptor<String, OdeObject> {

  @SuppressWarnings("checkstyle:missingJavadocMethod")
  public DisabledTopicsOdeObjectProducerInterceptor(Set<String> disabledTopics) {
    super(disabledTopics);
  }
}