package com.github.learntocode2013.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = SaasAppInfo.SaasAppInfoBuilder.class)
@Value
@Builder(toBuilder = true)
public class SaasAppInfo implements SingleTableDesign {
  @Getter(onMethod_ = {@DynamoDbPartitionKey})
  String pk;
  String organization;
  SubscriptionType subscriptionType;
  EntityType type;
  String website;
  long ttl;
  public enum SubscriptionType {
    ENTERPRISE, PRO, FREE
  }
}
