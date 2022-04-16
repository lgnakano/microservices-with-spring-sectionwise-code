package com.eazybytes.accounts.service.exceptions;

import feign.FeignException;
import feign.Request;

public class LoansFeignClientException extends FeignException.ServiceUnavailable {

    public LoansFeignClientException(String message, Request request, byte[] data) {
        super(message, request, data);
    }
}
