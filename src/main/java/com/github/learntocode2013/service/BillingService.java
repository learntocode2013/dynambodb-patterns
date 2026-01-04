package com.github.learntocode2013.service;

import static java.util.Objects.isNull;

import com.github.learntocode2013.data.BillingRepository;
import com.github.learntocode2013.model.BillingAdmin;
import com.github.learntocode2013.model.SaasAppInfo;
import com.github.learntocode2013.util.ItemBasedAction;
import io.vavr.control.Try;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbResponseMetadata;

public class BillingService {
  private static final Logger log = LoggerFactory.getLogger(BillingService.class);
  private final BillingRepository repository;

  public BillingService(BillingRepository repository) {
    this.repository = repository;
  }

  @ItemBasedAction
  public Try<PutItemEnhancedResponse<SaasAppInfo>> createTenant(SaasAppInfo saasAppInfo) {
    if (isNull(saasAppInfo)) {
      return Try.failure(new IllegalArgumentException("Cannot create tenant with invalid input"));
    }
    log.info("Creating tenant for organization: {}", saasAppInfo.getOrganization());
    return repository.saveItem(saasAppInfo);
  }

  @ItemBasedAction
  public Try<PutItemEnhancedResponse<BillingAdmin>> createTenantAdmin(
      BillingAdmin billingAdmin) {
    if (isNull(billingAdmin)) {
      return Try.failure(new IllegalArgumentException("Cannot create tenant admin with invalid input"));
    }
    log.info("Creating tenant admin for organization: {}", billingAdmin.getOrganization());
    return repository.saveItem(billingAdmin);
  }

  @ItemBasedAction
  public Try<List<ConsumedCapacity>> deleteSubscriptionViaOrgAdmin(
      String pk,
      String adminPk,
      String requestingUser) {
    if (isNull(pk) ||  isNull(adminPk) || isNull(requestingUser)) {
      return Try.failure(new IllegalArgumentException("Cannot delete tenant with invalid input"));
    }
    log.info("Received a tenant deletion request from : {}", requestingUser);
    return repository.deleteSubscriptionViaOrgAdmin(pk, adminPk, requestingUser);
  }

  @ItemBasedAction
  public Try<DynamoDbResponseMetadata> updateEmailAndAdminsForAnOrg(
      String adminPk,
      Set<String> newAdminsToAppend,
      String newEmailAddress,
      String cellPhoneNumber,
      DynamoDbClient dynamoDbClient) {
    if (isNull(adminPk) || isNull(newAdminsToAppend) || isNull(newEmailAddress)
        || isNull(cellPhoneNumber) || isNull(dynamoDbClient)) {
      return Try.failure(new IllegalArgumentException("Cannot update tenant with invalid input"));
    }
    log.info("Received a tenant admin update request from : {}", adminPk);
    return repository.updateEmailAndAdminsForAnOrg(
        adminPk,
        newAdminsToAppend,
        newEmailAddress,
        cellPhoneNumber,
        dynamoDbClient
    );
  }

  @ItemBasedAction
  public Try<DynamoDbResponseMetadata> removeAdminsForOrg(
      String adminPk,
      Set<String> adminsToRemove,
      DynamoDbClient dynamoDbClient) {
    if (isNull(adminPk) || isNull(adminsToRemove) || isNull(dynamoDbClient)) {
      return Try.failure(
          new IllegalArgumentException("Cannot remove tenant admins with invalid input")
      );
    }
    log.info("Received a request to remove tenant admins: {}", adminsToRemove);
    return repository.removeAdminsForOrg(adminPk, adminsToRemove, dynamoDbClient);
  }

  @ItemBasedAction
  public Try<BillingAdmin> getOrgAdmins(String adminPk) {
    if (isNull(adminPk)) {
      return Try.failure(
          new IllegalArgumentException("Cannot retrieve tenant admins with invalid input")
      );
    }
    log.info("Received a request to fetch tenant admins for: {}", adminPk);
    return repository.getOrgAdmins(adminPk);
  }
}
