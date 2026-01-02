package com.github.learntocode2013.model;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = BillingAdmin.BillingAdminBuilder.class)
@Value
@Builder(toBuilder = true)
public class BillingAdmin {
  @Getter(onMethod_ = {@DynamoDbPartitionKey})
  String pk;
  String organization;
  Set<String> admins;
  String emailAddress;
  long ttl;
}
