package com.github.learntocode2013.service;

import com.github.learntocode2013.data.BillingRepository;
import com.github.learntocode2013.model.BillingAdmin;
import com.github.learntocode2013.model.SaasAppInfo;
import com.github.learntocode2013.model.SaasAppInfo.SubscriptionType;
import com.github.learntocode2013.util.DynamoDBClientFactory;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillingServiceTest {
  private static final Logger log = LoggerFactory.getLogger(BillingServiceTest.class);
  private static final String ORG_ENTITY_PK_PREFIX = "Billing#";
  private static final String ADMIN_ENTITY_PK_PREFIX = "Admin#";
  private static DynamoDbClient dynamoDbClient;
  private static BillingService subject;

  @BeforeAll
  static void setUp() {
    dynamoDbClient = DynamoDBClientFactory.createLocalClient();
    DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDBClientFactory.createEnhancedLocalClient();
    BillingRepository billingRepository = new BillingRepository(dynamoDbEnhancedClient);
    subject = new BillingService(billingRepository);
    billingRepository.createTableIfNotExists();
    billingRepository.enableTtl(dynamoDbClient)
        .onFailure(th -> log.error("Failed to enable TTL on table {}",
            BillingRepository.TABLE_NAME,
            th));
    loadData();
  }

  @Test
  @Order(3)
  void deleteSubscriptionWorks_Via_Admin_User() {
    var failedResponse = subject.deleteSubscriptionViaOrgAdmin(
        String.format("%s%s",ORG_ENTITY_PK_PREFIX, "Amazon"),
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Amazon"),
        "Dibakar Sen"
    );
    Assertions.assertTrue(failedResponse.isFailure());

    var response = subject.deleteSubscriptionViaOrgAdmin(
        String.format("%s%s",ORG_ENTITY_PK_PREFIX, "Amazon"),
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Amazon"),
        "Andy Jassy"
    );
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertEquals(1,response.get().size());
  }

  @Test
  @Order(1)
  void multipleUpdatesOnTenantItem_Works() {
    var response = subject.updateEmailAndAdminsForAnOrg(
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Amazon"),
        Set.of("Dibakar Sen", "Venket Subramaniam"),
        "billing_admins@amazon.com",
        "91-9999999999",
        dynamoDbClient
    );
    Assertions.assertTrue(response.isSuccess());
    log.info("After update attributes on item, attributes updated: {}", response.get());
  }

  @Test
  @Order(2)
  void removingAdminsFromAnOrg_Works() {
    var offboardedAdmins = Set.of("Dibakar Sen", "Venket Subramaniam");
    var adminPk = String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Amazon");
    log.info("Removing offboarded admins from the database: {}", offboardedAdmins);
    var response = subject.removeAdminsForOrg(
        adminPk,
        offboardedAdmins,
        dynamoDbClient
    );
    Assertions.assertTrue(response.isSuccess());
    subject.getOrgAdmins(adminPk).onSuccess(info -> {
      Assertions.assertFalse(info.getAdmins().containsAll(offboardedAdmins));
    });
  }

  static void loadData() {
    generateTenantData().forEach((pk, item) -> subject.createTenant(item));
    generateAdminData().forEach((pk, item) -> subject.createTenantAdmin(item));
  }

  static Map<String, SaasAppInfo> generateTenantData() {
    var secondsToExpiry = Instant.now().plusSeconds(3600).getEpochSecond();
    return Map.of(
        String.format("%s%s",ORG_ENTITY_PK_PREFIX,"Amazon"),
        SaasAppInfo.builder()
            .pk(String.format("%s%s",ORG_ENTITY_PK_PREFIX,"Amazon"))
            .organization("Amazon")
            .subscriptionType(SubscriptionType.ENTERPRISE)
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ORG_ENTITY_PK_PREFIX, "Google"),
        SaasAppInfo.builder()
            .pk(String.format("%s%s",ORG_ENTITY_PK_PREFIX,"Google"))
            .organization("Google")
            .subscriptionType(SubscriptionType.PRO)
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ORG_ENTITY_PK_PREFIX, "Oracle"),
        SaasAppInfo.builder()
            .pk(String.format("%s%s",ORG_ENTITY_PK_PREFIX,"Oracle"))
            .organization("Oracle")
            .subscriptionType(SubscriptionType.FREE)
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ORG_ENTITY_PK_PREFIX, "Meta"),
        SaasAppInfo.builder()
            .pk(String.format("%s%s",ORG_ENTITY_PK_PREFIX,"Meta"))
            .organization("Meta")
            .subscriptionType(SubscriptionType.ENTERPRISE)
            .ttl(secondsToExpiry)
            .build()
    );
  }
  static Map<String, BillingAdmin> generateAdminData() {
    var secondsToExpiry = Instant.now().plusSeconds(3600).getEpochSecond();
    return Map.of(
        String.format("%s%s",ADMIN_ENTITY_PK_PREFIX,"Amazon"),
        BillingAdmin.builder()
            .pk(String.format("%s%s",ADMIN_ENTITY_PK_PREFIX,"Amazon"))
            .organization("Amazon")
            .admins(Set.of("Jeff Bezos", "Andy Jassy"))
            .phoneNumbers(Map.of())
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Google"),
        BillingAdmin.builder()
            .pk(String.format("%s%s",ADMIN_ENTITY_PK_PREFIX,"Google"))
            .organization("Google")
            .admins(Set.of("Sundar Picchai"))
            .phoneNumbers(Map.of())
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Oracle"),
        BillingAdmin.builder()
            .pk(String.format("%s%s",ADMIN_ENTITY_PK_PREFIX,"Oracle"))
            .organization("Oracle")
            .admins(Set.of("Larry Ellison", "AndyJassy"))
            .phoneNumbers(Map.of())
            .ttl(secondsToExpiry)
            .build(),
        String.format("%s%s", ADMIN_ENTITY_PK_PREFIX, "Meta"),
        BillingAdmin.builder()
            .pk(String.format("%s%s",ADMIN_ENTITY_PK_PREFIX,"Meta"))
            .organization("Meta")
            .admins(Set.of("Mark Zuckerberg"))
            .phoneNumbers(Map.of())
            .ttl(secondsToExpiry)
            .build()
    );
  }
}