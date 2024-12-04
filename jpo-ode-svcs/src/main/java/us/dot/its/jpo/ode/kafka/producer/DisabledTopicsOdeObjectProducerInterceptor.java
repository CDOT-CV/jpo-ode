package us.dot.its.jpo.ode.kafka.producer;

import java.util.Set;
import us.dot.its.jpo.ode.model.OdeObject;

public class DisabledTopicsOdeObjectProducerInterceptor
    extends AbstractDisabledTopicsProducerInterceptor<String, OdeObject> {

  public DisabledTopicsOdeObjectProducerInterceptor(Set<String> disabledTopics) {
    super(disabledTopics);
  }
}