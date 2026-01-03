package com.github.learntocode2013.util;

import io.vavr.control.Try;
import org.slf4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
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
}
