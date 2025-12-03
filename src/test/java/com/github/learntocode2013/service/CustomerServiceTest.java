package com.github.learntocode2013.service;

import com.github.learntocode2013.model.Customer;
import com.github.learntocode2013.util.DynamoDBClientFactory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class CustomerServiceTest {
  private static final Logger log = LoggerFactory.getLogger(CustomerServiceTest.class);
  private static final String custId = UUID.randomUUID().toString();
  private static DynamoDbEnhancedClient enhancedClient;
  private static CustomerService subject;

  @BeforeAll
  static void setup() {
    enhancedClient = DynamoDBClientFactory.createLocalClient();
    subject = new CustomerService(enhancedClient);
  }

  @AfterAll
  static void tearDown() {
    enhancedClient = null;
  }

  @Test
  void tableCreationWorks() {
    var result = subject.createTableIfNotExists();
    Assertions.assertTrue(result.isSuccess());
    result.onSuccess(resp ->
        log.info("----  Table info ----  {} {} ",
            System.lineSeparator(),
            resp.table()));
  }

  @Test
  void itemInsertionWorks() {
    var result = subject.putCustomer(new Customer(
            custId,
            "Dibakar",
            "Sen",
            "adroit_dibs@yahoo.com",
            Instant.now()))
        .onSuccess(v -> log.info("Successfully added a new customer with id: {}", custId));
    Assertions.assertTrue(result.isSuccess());
  }

  @Test
  void itemDeletionWorks() {
    var result = subject.deleteCustomer(custId)
        .onSuccess(v -> log.info("Successfully deleted customer with id: {}", custId));
    Assertions.assertTrue(result.isSuccess());
  }

  @Test
  void itemUpdateWorks() {
    var result = subject.updateCustomer(new Customer(
        custId,
        "Dibakar",
        "Sen",
        "adroit_dibs@yahoo.com",
        Instant.now().minusSeconds(60)
    )).onSuccess(v -> log.info("Successfully updated customer with id: {}", custId));
    Assertions.assertTrue(result.isSuccess());
  }

}