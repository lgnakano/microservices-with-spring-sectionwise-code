package com.eazybytes.accounts.service.exceptions;

public class CardsFeignClientException extends Throwable {
    public String message;
    public CardsFeignClientException(String message) {
        this.message = message;
    }
}
