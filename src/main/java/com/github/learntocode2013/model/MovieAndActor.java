package com.github.learntocode2013.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbImmutable(builder = MovieAndActor.MovieAndActorBuilder.class)
@Value
@Builder(toBuilder = true)
public class MovieAndActor {
  @Getter(onMethod_ = {@DynamoDbPartitionKey})
  String actor;
  @Getter(onMethod_ = {@DynamoDbSortKey, @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_movie_pk"})})
  String movie;
  String role;
  String year;
  Genre genre;
  long ttl;
  public enum Genre {
    DRAMA, MYSTERY, THRILLER, FANTASY, ACTION, CHILDREN
  }
}
