/**
 * 
 */
package com.eazybytes.accounts.controller;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.eazybytes.accounts.service.exceptions.CardsFeignClientException;
import com.eazybytes.accounts.service.exceptions.LoansFeignClientException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * @author Eazy Bytes
 *
 */

@RestController
public class AccountsController {

	Logger log = Logger.getLogger("AccountsController");
	
	@Autowired
	private AccountsRepository accountsRepository;

	@Autowired
	AccountsServiceConfig accountsConfig;
	
	@Autowired
	LoansFeignClient loansFeignClient;

	@Autowired
	CardsFeignClient cardsFeignClient;
	
	@PostMapping("/myAccount")
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
//	@CircuitBreaker(name = "detailsForCustomerSupportApp", fallbackMethod = "myCustomerDetailsFallBack")
	@Retry(name = "retryForCustomerDetails"
//			, fallbackMethod = "myCustomerDetailsFallBack"
	)
	public CustomerDetails myCustomerDetails(@RequestBody Customer customer) throws LoansFeignClientException, CardsFeignClientException {
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans;
//		try {
			loans = loansFeignClient.getLoansDetails(customer);
//		} catch (Exception e) {
////			e.printStackTrace();
//			throw new LoansFeignClientException(e.getMessage());
//		}
		List<Cards> cards;
//		try {
			cards = cardsFeignClient.getCardDetails(customer);
//		} catch (Exception e) {
////			e.printStackTrace();
//			throw new CardsFeignClientException(e.getMessage());
//		}

		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		customerDetails.setCards(cards);
		
		return customerDetails;
	}
	
	private CustomerDetails myCustomerDetailsFallBack(Customer customer, Throwable t) {
		System.out.println(t.getMessage());
		System.out.println(t.getClass());
		log.info(t.getMessage());
		for(StackTraceElement ste:
				t.getStackTrace()) {
			log.info(ste.toString());
		}
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		if (t.getClass() == CardsFeignClientException.class) {
			customerDetails.setLoans(loansFeignClient.getLoansDetails(customer));
		} else if (t.getClass() == LoansFeignClientException.class) {
			customerDetails.setCards(cardsFeignClient.getCardDetails(customer));
		}

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
