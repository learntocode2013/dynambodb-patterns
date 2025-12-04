package com.github.learntocode2013.service;

import static com.github.learntocode2013.model.CustomerProfile.Status.ACTIVE;
import static com.github.learntocode2013.model.CustomerProfile.Status.INACTIVE;

import com.github.learntocode2013.model.CustomerProfile;
import io.vavr.control.Try;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class CustomerProfileService {
  private static final Logger log = LoggerFactory.getLogger(CustomerProfileService.class);
  private static final String TABLE_NAME = "CustomerProfiles";
  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<CustomerProfile> table;

  public CustomerProfileService(DynamoDbEnhancedClient enhancedClient) {
    this.enhancedClient = enhancedClient;
    this.table = enhancedClient.table(
        TABLE_NAME,
        TableSchema.fromImmutableClass(CustomerProfile.class));
  }

  public Try<DescribeTableEnhancedResponse> createTableIfNotExists() {
    return Try.of(
            table::describeTable)
        .onFailure(ex -> {
          if (ex instanceof ResourceNotFoundException) {
            table.createTable();
            try(DynamoDbWaiter dynamoDbWaiter = DynamoDbWaiter.create()) {
              dynamoDbWaiter.waitUntilTableExists(b -> b.tableName(TABLE_NAME));
              log.info("{} was created since it does not exist", TABLE_NAME);
            }
            return;
          }
          log.warn(ex.getMessage(), ex);
        });
  }

  // We do not want a customer profile entry to be overwritten ever once created;
  // only update will be supported.
  public Try<Void> createItem(CustomerProfile item) {
    var createRequest = PutItemEnhancedRequest.builder(CustomerProfile.class)
        .item(item)
        .conditionExpression(existenceCondition())
        .build();
    return Try.run(() -> table.putItem(createRequest))
        .onFailure(ex -> {
          if (ex instanceof ConditionalCheckFailedException) {
            log.warn("Item with key: {} already exists", item.getId());
            return;
          }
          log.warn(ex.getMessage(), ex);
          ex.printStackTrace();
        });
  }

  private Expression existenceCondition() {
    return Expression.builder()
        .expression("attribute_not_exists(id)")
        .build();
  }

  public Try<CustomerProfile> deleteItem(String pKey) {
    return Try.of(() -> table.deleteItem(Key.builder().partitionValue(pKey).build()))
        .onFailure(ex -> log.warn(ex.getMessage(), ex));
  }

  public Try<CustomerProfile> updateItem(CustomerProfile updatedItem) {
    return Try.of(() -> table.updateItem(updatedItem))
        .onFailure(ex -> log.warn(ex.getMessage(), ex));
  }

  public Try<CustomerProfile> getItem(String pKey) {
    log.info("Attempting to fetch Customer by pKey: {}", pKey);
    var fetchReq = GetItemEnhancedRequest.builder()
        .key(Key.builder().partitionValue(pKey).build())
        .consistentRead(true)
        .build();
    return Try.of(() -> {
      var item = table.getItem(fetchReq);
      if (item == null) {
        throw new RuntimeException(String.format("Item with key: %s does not exist", pKey));
      }
      return item;
    }).onFailure(ex -> {
          log.warn("Failed to fetch item with partition key: {} due to: {}",
              pKey, ex.getMessage());
        });
  }

  public Try<List<CustomerProfile>> getProfilesCreatedBetween(Instant start, Instant end) {
    var request = ScanEnhancedRequest.builder()
        .filterExpression(Expression.builder()
            .expression("regDate between :start and :end")
            .expressionValues(Map.of(
                ":start", AttributeValue.fromS(start.toString()),
                ":end", AttributeValue.fromS(end.toString())
            ))
            .build()
        )
        .build();
    log.info("Attempting to fetch Customers via {}", request.filterExpression().toString());
    return Try.of(() -> table.scan(request))
        .map(pageIterable -> {
          List<CustomerProfile> items = pageIterable.items().stream().toList();
          log.info("Found {} Customer items matching the filter condition", items.size());
          return items;
        })
        .onFailure(ex -> {
          log.warn(ex.getMessage(), ex);
        });
  }

  public Try<List<CustomerProfile>> getAllProfilesWithStatus(CustomerProfile.Status status) {
    var queryConditional = QueryConditional.keyEqualTo(
        Key.builder().partitionValue(status.name()).build());
    log.info("Querying customers via {}", queryConditional);
    return Try.of(() -> table.index("status-index").query(queryConditional))
        .map(pageIterable -> {
          List<CustomerProfile> items = pageIterable.stream().toList().stream()
              .map(Page::items)
              .flatMap(List::stream)
              .toList();
          log.info("Found {} customer profiles with status {}", items.size(), status.name());
          return  items;
        }).onFailure(ex -> log.warn(ex.getMessage(), ex));
  }

  public Try<List<CustomerProfile>> softDeleteAllItems() {
    var request = ScanEnhancedRequest.builder()
        .filterExpression(Expression.builder()
            .expression("#status = :val")
            .expressionNames(Map.of("#status", "status"))
            .expressionValues(Map.of(":val", AttributeValue.fromS(ACTIVE.name())))
            .build())
        .build();

    return Try.of(() -> table.scan(request))
        .map(pageIterable -> {
          var items = pageIterable.stream()
              .flatMap(page -> page.items().stream())
              .toList();
          log.info("Found {} active customer profiles items for soft delete", items.size());
          var conditionExpression = Expression.builder()
              .expression("#status = :activeStatus")
              .expressionNames(Map.of("#status", "status"))
              .expressionValues(Map.of(":activeStatus", AttributeValue.fromS(ACTIVE.name())))
              .build();
          List<CustomerProfile> updatedProfiles = new ArrayList<>();
          for (var item : items) {
            var updatedItem = item.toBuilder().status(INACTIVE).build();
            var updateRequest = UpdateItemEnhancedRequest.builder(CustomerProfile.class)
                .item(updatedItem)
                .conditionExpression(conditionExpression)
                .build();
            Try.of(() -> {
                  var resp = table.updateItem(updateRequest);
                  updatedProfiles.add(resp);
                  return resp;
                })
                .onFailure(ex -> {
                  log.warn(ex.getMessage(), ex);
                }).isSuccess();
          }

          return updatedProfiles;
        });
  }

  public Try<List<CustomerProfile>> fetchBatchOfCustomerProfiles(List<String> pKeys) {
    var limitedPKeys = pKeys.subList(0, Math.min(pKeys.size(), 100));
    log.info("Limited input list of size: {} to {} for fetching batch of Customers",
        pKeys.size(),
        limitedPKeys.size());
    var builder = ReadBatch.builder(CustomerProfile.class)
        .mappedTableResource(table);
    limitedPKeys.forEach(pkey -> builder
        .addGetItem(Key.builder().partitionValue(pkey).build()));
    var batch = builder.build();
    return Try.of(() -> enhancedClient
        .batchGetItem( b -> b.addReadBatch(batch))
    ).map(resultPages -> {
      return resultPages.resultsForTable(table).stream().toList();
    }).onFailure(e -> log.warn(e.getMessage(), e));
  }
}
