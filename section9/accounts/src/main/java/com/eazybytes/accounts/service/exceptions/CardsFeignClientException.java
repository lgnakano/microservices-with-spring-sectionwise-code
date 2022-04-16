package com.eazybytes.accounts.service.exceptions;

import feign.FeignException;
import feign.Request;

public class CardsFeignClientException extends FeignException.ServiceUnavailable{
    public CardsFeignClientException(String message, Request request, byte[] data) {
        super(message, request, data);
    }
}
