package com.eazybytes.accounts.service.exceptions;

public class LoansFeignClientException extends Throwable {
    public String message;
    public LoansFeignClientException(String message) {
        this.message = message;
    }
}
