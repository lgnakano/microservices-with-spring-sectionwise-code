/**
 * 
 */
package com.eazybytes.accounts.controller;

import java.nio.ByteBuffer;
import java.util.List;

import com.eazybytes.accounts.service.exceptions.CardsFeignClientException;
import com.eazybytes.accounts.service.exceptions.LoansFeignClientException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.eazybytes.accounts.config.AccountsServiceConfig;
import com.eazybytes.accounts.model.Accounts;
import com.eazybytes.accounts.model.Cards;
import com.eazybytes.accounts.model.Customer;
import com.eazybytes.accounts.model.CustomerDetails;
import com.eazybytes.accounts.model.Loans;
import com.eazybytes.accounts.model.Properties;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.service.client.CardsFeignClient;
import com.eazybytes.accounts.service.client.LoansFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import javax.inject.Inject;

import io.micrometer.core.annotation.Timed;

/**
 * @author Eazy Bytes
 *
 */

@RestController
public class AccountsController {
	
	private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);

	private final AccountsRepository accountsRepository;

	private final AccountsServiceConfig accountsConfig;

	LoansFeignClient loansFeignClient;

	CardsFeignClient cardsFeignClient;

	@Inject
	public AccountsController(AccountsRepository accountsRepository,
							  AccountsServiceConfig accountsConfig,
							  LoansFeignClient loansFeignClient,
							  CardsFeignClient cardsFeignClient) {
		Assert.notNull(accountsRepository, "AccountsRepository should not be null");
		this.accountsRepository = accountsRepository;

		Assert.notNull(accountsConfig, "AccountsConfig should not be null");
		this.accountsConfig = accountsConfig;

		Assert.notNull(loansFeignClient, "LoansFeignClient should not be null");
		this.loansFeignClient = loansFeignClient;

		Assert.notNull(cardsFeignClient, "CardsFeignClient should not be null");
		this.cardsFeignClient = cardsFeignClient;

	}
	
	@PostMapping("/myAccount")
	@Timed(value = "getAccountDetails.time", description = "Time taken to return Account Details")
	public Accounts getAccountDetails(@RequestBody Customer customer) {

		return accountsRepository.findByCustomerId(customer.getCustomerId());

	}
	
	@GetMapping("/account/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Properties properties = new Properties(accountsConfig.getMsg(), accountsConfig.getBuildVersion(),
				accountsConfig.getMailDetails(), accountsConfig.getActiveBranches());
		return ow.writeValueAsString(properties);
	}
	
	@PostMapping("/myCustomerDetails")
	@CircuitBreaker(name = "detailsForCustomerSupportApp",fallbackMethod="myCustomerDetailsFallBack")
	@Retry(name = "retryForCustomerDetails", fallbackMethod = "myCustomerDetailsFallBack")
	public CustomerDetails myCustomerDetails(@RequestHeader("eazybank-correlation-id") String correlationId,@RequestBody Customer customer) {
		logger.info("myCustomerDetails() method started");
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans;
		try {
			loans = loansFeignClient.getLoansDetails(correlationId, customer);
		} catch (FeignException.ServiceUnavailable e) {
			for(StackTraceElement ste: e.getStackTrace()) {
				logger.info(ste.toString());
			}
			throw new LoansFeignClientException(e.getMessage(), e.request(),
					e.responseBody().orElse(ByteBuffer.wrap(new byte[0])).array());
		}
		List<Cards> cards;
		try {
			cards = cardsFeignClient.getCardDetails(correlationId, customer);
		} catch (FeignException.ServiceUnavailable e) {
			for(StackTraceElement ste: e.getStackTrace()) {
				logger.info(ste.toString());
			}
			throw new CardsFeignClientException(e.getMessage(), e.request(),
				e.responseBody().orElse(ByteBuffer.wrap(new byte[0])).array());
		}

		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		customerDetails.setCards(cards);
		logger.info("myCustomerDetails() method ended");
		return customerDetails;
	}
	
	private CustomerDetails myCustomerDetailsFallBack(@RequestHeader ("eazybank-correlation-id") String correlationId, Customer customer, Throwable t) {
		logger.info("message in myCustomerDetailsFallback " + t.getMessage());
		logger.info("exception class is " + t.getClass());
		logger.info(t.getMessage());
		for(StackTraceElement ste:
				t.getStackTrace()) {
			logger.info(ste.toString());
		}
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		if (t.getClass() == CardsFeignClientException.class) {
			logger.info("got a Cards Feign Client Exception, try to get loans");
			customerDetails.setLoans(loansFeignClient.getLoansDetails(correlationId, customer));
		} else if (t.getClass() == LoansFeignClientException.class) {
			logger.debug("got a Loans Feign Client Exception, try to get cards");
			customerDetails.setCards(cardsFeignClient.getCardDetails(correlationId, customer));
		}
		logger.info("about to return from myCustomerDetailsFallback");
		return customerDetails;

	}
	
	@GetMapping("/sayHello")
	@RateLimiter(name = "sayHello", fallbackMethod = "sayHelloFallback")
	public String sayHello() {
		return "Hello, Welcome to EazyBank";
	}

	private String sayHelloFallback(Throwable t) {
		return "Hi, Welcome to EazyBank";
	}

}
