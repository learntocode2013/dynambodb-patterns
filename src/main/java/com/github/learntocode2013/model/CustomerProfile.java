package com.github.learntocode2013.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbImmutable(builder = CustomerProfile.CustomerProfileBuilder.class)
@Value
@Builder(toBuilder = true)
public class CustomerProfile {
  @Getter(onMethod_ = {@DynamoDbPartitionKey})
  private final String id;
  private final String email;
  private final String firstName;
  private final Instant regDate;
  private final String lastName;
  @Getter(onMethod_ = {@DynamoDbSecondaryPartitionKey(indexNames = {"status-index"})})
  private final Status status;
  public enum Status {
    ACTIVE, INACTIVE
  };
}
