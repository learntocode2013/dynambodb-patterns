package com.github.learntocode2013.data;

import com.github.learntocode2013.util.Operations;
import io.vavr.control.Try;
import org.slf4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;

public interface Repository {
  String TTL_ATTRIBUTE_NAME = "ttl";

  default Try<UpdateTimeToLiveResponse> enableTtl(
      DynamoDbClient dynamoDbClient,
      String tableName,
      Logger log) {
    var request = UpdateTimeToLiveRequest.builder()
        .tableName(tableName)
        .timeToLiveSpecification(TimeToLiveSpecification.builder()
            .enabled(true)
            .attributeName(TTL_ATTRIBUTE_NAME)
            .build())
        .build();
    return Try.of(() -> dynamoDbClient.updateTimeToLive(request))
        .onFailure(err -> {
          if (err.getMessage().contains("TimeToLive is already enabled")) {
            log.warn("TimeToLive is already enabled");
            return;
          }
          log.error("Failed to enable TTL via attribute: {}", TTL_ATTRIBUTE_NAME, err);
        });
  }
}
