package com.github.learntocode2013.service;

import com.github.learntocode2013.model.CustomerProfile;
import com.github.learntocode2013.util.DynamoDBClientFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerProfileServiceTest {
  private static final Logger logger = LoggerFactory.getLogger(CustomerProfileServiceTest.class);
  private static final List<String> EMAIL_IDS = List.of(
      "adroit_dibs@yahoo.com",
      "sonic.yssss@gmail.com",
      "softwarepractitioner@gmail.com",
      "learntocode2013@yahoo.com"
  );
  private static final List<String> NAMES = List.of(
      "Dibakar Sen",
      "Venket Subramaniam",
      "Daniel Hinojosa",
      "Trisha Gee"
  );
  private static final List<String> PKEYS = List.of(
      String.format("%s#%s", "USER", UUID.nameUUIDFromBytes(EMAIL_IDS.getFirst().getBytes())),
      String.format("%s#%s", "USER", UUID.nameUUIDFromBytes(EMAIL_IDS.get(1).getBytes())),
      String.format("%s#%s", "USER", UUID.nameUUIDFromBytes(EMAIL_IDS.get(2).getBytes())),
      String.format("%s#%s", "USER", UUID.nameUUIDFromBytes(EMAIL_IDS.getLast().getBytes()))
  );
  private static Map<String, CustomerProfile> CUSTOMERS = new HashMap<>();
  private static CustomerProfileService subject;

  @BeforeAll
  static void setUp() {
    subject = new CustomerProfileService(DynamoDBClientFactory.createLocalClient());
    CUSTOMERS = generateItems();
    CUSTOMERS.putAll(generateBatchOfCustomerProfiles(150));
  }

  @Test
  @Order(1)
  void tableCreationWorks() {
    var response = subject.createTableIfNotExists();
    Assertions.assertTrue(response.isSuccess());
    response.onSuccess(resp -> logger.info(resp.toString()));
  }

  @Test
  @Order(3)
  void saveItemWorks() {
    saveItems(CUSTOMERS);
  }

  private void saveItems(Map<String, CustomerProfile> items) {
    items.forEach((pk, item) -> {
      var response = subject.createItem(item);
      Assertions.assertTrue(response.isSuccess());
      response.onSuccess(v -> logger.info("Item created for tenant: {}", item.getFirstName()));
    });
  }

  @Test
  @Order(5)
  void testItemFetch() {
    CUSTOMERS.forEach((key, item)-> {
      var response = subject.getItem(key);
      Assertions.assertTrue(response.isSuccess());
      response.onSuccess(v -> logger.info("Item fetched for tenant: {}",
          item.getFirstName()));
    });
  }

  @Test
  @Order(5)
  void fetchProfiles_Within_A_Window() {
    var start = LocalDateTime.of(2025,01, 01,00,00)
        .toInstant(ZoneOffset.UTC);
    var end = LocalDateTime.of(2025,10, 01,00,00)
        .toInstant(ZoneOffset.UTC);
    var response = subject.getProfilesCreatedBetween(start, end);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertFalse(response.get().isEmpty());
    System.out.println("----- Items fetched -----");
    response.get().forEach(item -> {
      logger.info(item.toString());
    });
  }

  @Test
  @Order(5)
  void fetchAllActiveProfiles() {
    var response = subject.getAllProfilesWithStatus(CustomerProfile.Status.ACTIVE);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertFalse(response.get().isEmpty());
    System.out.println("----- Items fetched -----");
    response.get().forEach(item -> {
      logger.info(item.toString());
    });
  }

  @Test
  @Order(5)
  void fetchAllProfiles_In_Batch() {
    var response = subject.fetchBatchOfCustomerProfiles(
        CUSTOMERS.keySet().stream().toList());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertEquals(100, response.get().size());
    System.out.println("----- Items fetched -----");
    response.get().forEach(item -> {
      logger.info(item.toString());
    });
  }

  @Test
  @Order(6)
  void softDeleteAllProfiles() {
    var response = subject.softDeleteAllItems();
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertFalse(response.get().isEmpty());
    response.get().forEach(item -> {
      logger.info("{}'s status now is: {}",item.getFirstName(), item.getStatus());
    });
  }

  @Test
  @Order(4)
  void testItemUpdate() {
    CUSTOMERS.forEach((key, value) -> {
      var response = subject.updateItem(CustomerProfile.builder()
          .id(key)
          .firstName(value.getFirstName())
          .lastName(value.getLastName())
          .email(value.getEmail())
          .regDate(value.getRegDate().minus(30, ChronoUnit.DAYS))
          .status(CustomerProfile.Status.ACTIVE)
          .build());
      Assertions.assertTrue(response.isSuccess());
      logger.info("Updated details on tenant: {}", response.get());
    });
  }

  @Test
  @Order(2)
  void deleteAllItems() {
    CUSTOMERS.forEach((key, value) -> subject
        .deleteItem(key)
        .onFailure(ex -> {
              Assertions.fail("Failed to delete item with partition key: " + key);
            }
        ));
  }

  private static Map<String, CustomerProfile> generateItems() {
    Map<String, CustomerProfile> items = new HashMap<>();
    for(int i = 0; i < CustomerProfileServiceTest.PKEYS.size(); i++) {
      var registeredOn = (i % 2 == 0) ?
          Instant.now().minus(30*6, ChronoUnit.DAYS) :
          Instant.now();
      var fullName = NAMES.get(i).split("\\s+");
      var fName = fullName[0];
      var lName = fullName[1];
      var customer = CustomerProfile.builder()
          .id(CustomerProfileServiceTest.PKEYS.get(i))
          .firstName(fName)
          .lastName(lName)
          .email(EMAIL_IDS.get(i))
          .regDate(registeredOn)
          .build();
      items.put(CustomerProfileServiceTest.PKEYS.get(i), customer);
    }
    return items;
  }

  private static Map<String, CustomerProfile> generateBatchOfCustomerProfiles(int maxCount) {
    Map<String, CustomerProfile> profiles = new HashMap<>();
    for(var i = 0; i < maxCount; i++) {
      var email = String.format("%s@%s", "profile-"+i, "gmail.com");
      var pKey = String.format("%s#%s", "USER", UUID.nameUUIDFromBytes(email.getBytes()));
      var fName = "Profile-" + i;
      var lName = "LastName-" + i;
      var customer = CustomerProfile.builder()
          .id(pKey)
          .firstName(fName)
          .lastName(lName)
          .email(email)
          .regDate(Instant.now())
          .status(CustomerProfile.Status.ACTIVE)
          .build();
      profiles.put(pKey, customer);
    }
    return profiles;
  }
}