package com.nova.domain.result;

public sealed interface Result<T> permits Result.Success, Result.Failure {
    
    record Success<T>(T value) implements Result<T> {}
    
    record Failure<T>(String message, Throwable cause) implements Result<T> {
        public Failure(String message) {
            this(message, null);
        }
    }
}
