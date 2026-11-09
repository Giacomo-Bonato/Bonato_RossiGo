package org.example;

public class RossiGoException extends Exception {
    public RossiGoException(String message) {
        super(message);
    }

    public RossiGoException(String message, Throwable cause) {
        super(message, cause);
    }
}
