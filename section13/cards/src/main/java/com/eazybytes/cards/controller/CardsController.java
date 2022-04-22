/**
 * 
 */
package com.eazybytes.cards.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.eazybytes.cards.config.CardsServiceConfig;
import com.eazybytes.cards.model.Cards;
import com.eazybytes.cards.model.Customer;
import com.eazybytes.cards.model.Properties;
import com.eazybytes.cards.repository.CardsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import javax.inject.Inject;

/**
 * @author Eazy Bytes
 *
 */

@RestController
public class CardsController {
	
	private static final Logger logger = LoggerFactory.getLogger(CardsController.class);

	private final CardsRepository cardsRepository;

	private final CardsServiceConfig cardsConfig;

	@Inject
	public CardsController(CardsRepository cardsRepository,
						   CardsServiceConfig cardsConfig) {
		Assert.notNull(cardsRepository, "CardsRepository should not be null!");
		this.cardsRepository = cardsRepository;
		Assert.notNull(cardsConfig, "CardsConfig should not be null");
		this.cardsConfig = cardsConfig;

	}

	@PostMapping("/myCards")
	public List<Cards> getCardDetails(@RequestHeader("eazybank-correlation-id") String correlationId,@RequestBody Customer customer) {
		logger.info("getCardDetails() method started");
		List<Cards> cards = cardsRepository.findByCustomerId(customer.getCustomerId());
		logger.info("getCardDetails() method ended");
		return cards;

	}
	
	@GetMapping("/cards/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Properties properties = new Properties(cardsConfig.getMsg(), cardsConfig.getBuildVersion(),
				cardsConfig.getMailDetails(), cardsConfig.getActiveBranches());
		return ow.writeValueAsString(properties);
	}

}
