package us.dot.its.jpo.ode.coder;

public interface MessagePublisher<T> {
    void publish(String topic, T message);
}
