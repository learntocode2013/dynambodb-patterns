package com.github.learntocode2013.data;

import com.github.learntocode2013.model.BillingAdmin;
import com.github.learntocode2013.model.SaasAppInfo;
import com.github.learntocode2013.util.Operations;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbResponseMetadata;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;

public class BillingRepository implements Repository {
  public static final String TABLE_NAME = "SaasApp";
  private static final Logger log = LoggerFactory.getLogger(BillingRepository.class);
  private static final String TTL_ATTRIBUTE_NAME = "ttl";
  private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
  private final DynamoDbTable<SaasAppInfo> table;
  private final DynamoDbTable<BillingAdmin> adminTable;

  public BillingRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    this.table = dynamoDbEnhancedClient.table(
        TABLE_NAME,
        TableSchema.fromImmutableClass(SaasAppInfo.class));
    this.adminTable = dynamoDbEnhancedClient.table(
        TABLE_NAME,
        TableSchema.fromImmutableClass(BillingAdmin.class)
    );
  }

  public Try<UpdateTimeToLiveResponse> enableTtl(DynamoDbClient dynamoDbClient) {
    return enableTtl(dynamoDbClient, TABLE_NAME, log);
  }

  public Try<DescribeTableEnhancedResponse> createTableIfNotExists() {
    return Operations.createTableIfNotExists(table, TABLE_NAME, log);
  }

  public Try<PutItemEnhancedResponse<SaasAppInfo>> saveItem(SaasAppInfo saasAppInfo) {
    var request = PutItemEnhancedRequest.builder(SaasAppInfo.class)
        .item(saasAppInfo)
        .conditionExpression(Expression.builder()
            .expression("attribute_not_exists(pk)")
            .build())
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .returnValues(ReturnValue.NONE)
        .build();
    return Try.of(() -> table.putItemWithResponse(request))
        .onSuccess(resp
            -> log.info("Consumed: {} to insert item: {}",
            resp.consumedCapacity().toString(),
            saasAppInfo.getPk())
        )
        .onFailure(err -> log.warn("Failed to insert item for: {}",
            saasAppInfo.getPk(),
            err)
        );
  }

  public Try<PutItemEnhancedResponse<BillingAdmin>> saveItem(BillingAdmin billingAdmin) {
    var request = PutItemEnhancedRequest.builder(BillingAdmin.class)
        .item(billingAdmin)
        .conditionExpression(Expression.builder()
            .expression("attribute_not_exists(pk)")
            .build())
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .returnValues(ReturnValue.NONE)
        .build();
    return Try.of(() -> adminTable.putItemWithResponse(request))
        .onSuccess(resp
            -> log.info("Consumed: {} to insert billing admin item: {}",
            resp.consumedCapacity().toString(),
            billingAdmin.getPk())
        )
        .onFailure(err -> log.warn("Failed to insert item for: {}",
            billingAdmin.getPk(),
            err)
        );
  }

  public Try<List<ConsumedCapacity>> deleteSubscriptionViaOrgAdmin(String pk, String adminPk, String requestingUser) {
    var request = TransactWriteItemsEnhancedRequest.builder()
        .addConditionCheck(
            adminTable,
            ConditionCheck.builder()
                .key(Key.builder().partitionValue(adminPk).build())
                .conditionExpression(Expression.builder()
                    .expression("contains(#a, :user)")
                    .expressionNames(Map.of("#a", "admins"))
                    .expressionValues(Map.of(":user", AttributeValue.fromS(requestingUser)))
                    .build()
                )
                .build()
        )
        .addDeleteItem(table, Key.builder().partitionValue(pk).build())
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .build();
    return Try.of(() -> dynamoDbEnhancedClient.transactWriteItemsWithResponse(
            request)
        )
        .map(TransactWriteItemsEnhancedResponse::consumedCapacity)
        .onFailure(err ->
            log.error("Delete subscription request from user: {} for tenant: {} failed",
                requestingUser,
                pk,
                err)
        );
  }

  public Try<DynamoDbResponseMetadata> updateEmailAndAdminsForAnOrg(
      String adminPk,
      Set<String> newAdminsToAppend,
      String newEmailAddress,
      String cellPhoneNumber,
      DynamoDbClient dynamoDbClient) {
    String updateExpression = "SET #phone.#mobile = :cell, #email = :email ADD #admins :new_admins";
    var keyMap = Map.of("pk",AttributeValue.fromS(adminPk));
    Map<String, String> expressionNames = new HashMap<>();
    Map<String, AttributeValue> expressionValues = new HashMap<>();

    if (Objects.nonNull(newAdminsToAppend) &&  !newAdminsToAppend.isEmpty()) {
      expressionNames.put("#admins", "admins");
      expressionValues.put(":new_admins", AttributeValue.builder().ss(newAdminsToAppend).build());
    }

    if (Objects.nonNull(newEmailAddress)) {
      expressionNames.put("#email", "emailAddress");
      expressionValues.put(":email", AttributeValue.fromS(newEmailAddress));
    }

    if (Objects.nonNull(cellPhoneNumber)) {
      expressionNames.put("#phone", "phoneNumbers");
      expressionNames.put("#mobile", "MobileNumber");
      expressionValues.put(":cell", AttributeValue.fromS(cellPhoneNumber));
    }

    var request = UpdateItemRequest.builder()
        .tableName(TABLE_NAME)
        .key(keyMap)
        .updateExpression(updateExpression)
        .expressionAttributeNames(expressionNames)
        .expressionAttributeValues(expressionValues)
        .build();

    return Try.of(() -> dynamoDbClient.updateItem(request))
        .map(DynamoDbResponse::responseMetadata)
        .onFailure(err -> log.error("Failed to update attributes for item: {}",
            adminPk,
            err));

  }

  public Try<DynamoDbResponseMetadata> removeAdminsForOrg(
      String adminPk,
      Set<String> adminsToRemove,
      DynamoDbClient dynamoDbClient) {
    Map<String, AttributeValue> keyMap = Map.of("pk",AttributeValue.fromS(adminPk));
    var updateExpression = "DELETE #admins :old_admins";
    Map<String, String> expressionNames = new HashMap<>();
    Map<String, AttributeValue> expressionValues = new HashMap<>();

    if (Objects.nonNull(adminsToRemove) &&  !adminsToRemove.isEmpty()) {
      expressionNames.put("#admins", "admins");
      expressionValues.put(":old_admins", AttributeValue.builder().ss(adminsToRemove).build());
    }

    var request = UpdateItemRequest.builder()
        .tableName(TABLE_NAME)
        .key(keyMap)
        .updateExpression(updateExpression)
        .expressionAttributeNames(expressionNames)
        .expressionAttributeValues(expressionValues)
        .build();

    return Try.of(() -> dynamoDbClient.updateItem(request))
        .map(DynamoDbResponse::responseMetadata)
        .onFailure(th -> log.error("Failed to remove old admins from {}",
            TABLE_NAME,
            th)
        );
  }

  public Try<BillingAdmin> getOrgAdmins(String adminPk) {
    var request = GetItemEnhancedRequest.builder()
        .key(Key.builder().partitionValue(adminPk).build())
        .consistentRead(true)
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .build();
    return Try.of(() ->  adminTable.getItem(request))
        .onFailure(th -> log.error("Failed to fetch admin info for key: {}",
            adminPk,
            th));
  }
}
