package com.assignment.phoneinventory.exception;

public class InvalidCsvFormatException extends RuntimeException {
    public InvalidCsvFormatException(String message) {
        super(message);
    }
}
