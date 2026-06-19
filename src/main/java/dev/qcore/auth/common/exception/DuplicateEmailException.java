package dev.qcore.auth.common.exception;

public class DuplicateEmailException extends RuntimeException{

    public DuplicateEmailException(String message) {
        super(message);
    }
}
