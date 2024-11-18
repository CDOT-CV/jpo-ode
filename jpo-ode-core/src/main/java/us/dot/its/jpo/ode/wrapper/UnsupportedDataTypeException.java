package us.dot.its.jpo.ode.wrapper;

/**
 * UnsupportedDataTypeException is used to indicate when we've consumed a message on a queue that we don't support
 *  in the specified code path. It's used mainly to indicate to the MessageProcessor that we should acknowledge
 *  receipt and processing of the message on the kafak topic.
 */
public class UnsupportedDataTypeException extends RuntimeException {
    public UnsupportedDataTypeException(String msg) {
        super(msg);
    }
}
