package com.github.learntocode2013.util;

import com.github.learntocode2013.model.SaasAppInfo;
import io.vavr.control.Try;
import org.slf4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class Operations {
  public static Try<DescribeTableEnhancedResponse> createTableIfNotExists(
      DynamoDbTable<?> table,
      String tableName,
      Logger log) {
    return Try.of(
            table::describeTable)
        .onFailure(ex -> {
          if (ex instanceof ResourceNotFoundException) {
            table.createTable();
            try(DynamoDbWaiter dynamoDbWaiter = DynamoDbWaiter.create()) {
              dynamoDbWaiter.waitUntilTableExists(b -> b.tableName(tableName));
              log.info("{} was created since it does not exist", tableName);
            }
            return;
          }
          log.warn(ex.getMessage(), ex);
        });
  }
  // TODO: Generic save item operation
//  public static <T> Try<PutItemEnhancedResponse<T>> saveItem(
//      T item,
//      DynamoDbTable<T> table,
//      String attrForExistCheck,
//      Logger log) {
//    var request = PutItemEnhancedRequest.builder(T.class)
//        .item(item)
//        .conditionExpression(Expression.builder()
//            .expression(String.format("%s(%s)","attribute_not_exists",attrForExistCheck))
//            .build())
//        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
//        .returnValues(ReturnValue.NONE)
//        .build();
//    return Try.of(() -> table.putItemWithResponse(request))
//        .onSuccess(resp
//            -> log.info("Consumed: {} to insert item: {}",
//            resp.consumedCapacity().toString(),
//            item.getPk())
//        )
//        .onFailure(err -> log.warn("Failed to insert item for: {}",
//            item.getPk(),
//            err)
//        );
//  }
}
