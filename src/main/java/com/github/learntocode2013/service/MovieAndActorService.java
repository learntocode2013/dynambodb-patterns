package com.github.learntocode2013.service;

import com.github.learntocode2013.model.MovieAndActor;
import com.github.learntocode2013.model.MovieAndActor.Genre;
import com.github.learntocode2013.util.ItemBasedAction;
import com.github.learntocode2013.util.ItemCollectionAction;
import com.github.learntocode2013.util.Operations;
import io.vavr.control.Try;
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
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

public class MovieAndActorService {
  private static final Logger log = LoggerFactory.getLogger(MovieAndActorService.class);
  private static final String TABLE_NAME = "MoviesAndActors";
  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<MovieAndActor> table;

  public MovieAndActorService(DynamoDbEnhancedClient enhancedClient) {
    this.enhancedClient = enhancedClient;
    this.table = enhancedClient.table(
        TABLE_NAME,
        TableSchema.fromImmutableClass(MovieAndActor.class));
  }

  public Try<DescribeTableEnhancedResponse> createTableIfNotExists() {
    return Operations.createTableIfNotExists(table, TABLE_NAME, log);
  }

  @ItemBasedAction
  public Try<PutItemEnhancedResponse<MovieAndActor>> saveItem(MovieAndActor movieAndActor) {
    var request = PutItemEnhancedRequest.builder(MovieAndActor.class)
        .item(movieAndActor)
        .conditionExpression(Expression.builder()
            .expression("attribute_not_exists(actor)")
            .build())
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .returnValues(ReturnValue.NONE)
        .build();
    return Try.of(() -> table.putItemWithResponse(request))
        .onSuccess(resp -> log.info("Consumed: {} to insert item: {}",
            resp.consumedCapacity().toString(), movieAndActor.getActor())
        )
        .onFailure(err -> log.warn("Failed to insert item for: {}",
            movieAndActor.getActor(),
            err)
        );
  }

  public Try<List<MovieAndActor>> queryItemsUsing_KeyConditionExpressions_And_FilterExpressions(
      String pKey,
      Genre genre) {
    var qc = QueryConditional.keyEqualTo(Key.builder().partitionValue(pKey).build());
    var request = QueryEnhancedRequest.builder()
        .queryConditional(qc)
        .filterExpression(Expression.builder()
            .expression("#genre = :genre")
            .expressionNames(Map.of("#genre", "genre"))
            .expressionValues(Map.of(":genre", AttributeValue.fromS(genre.name())))
            .build()
        )
        .build();
    return Try.of(() -> table.query(request))
        .map(pages -> {
          List<MovieAndActor> movieAndActors = new ArrayList<>();
          pages.forEach(page -> {
            movieAndActors.addAll(page.items());
          });
          return movieAndActors;
        })
        .onFailure(err -> log.error("Failed to fetch items for partition key:"
            + " {} | filter: {}", pKey, genre, err));
  }

  @ItemCollectionAction
  public Try<List<MovieAndActor>> queryItemsUsing_KeyConditionExpressions(
      String pKey,
      String fromTitlePrefix,
      String toTitlePrefix) {
    var qc = QueryConditional.sortBetween(
        Key.builder()
            .partitionValue(pKey)
            .sortValue(fromTitlePrefix)
            .build(),
        Key.builder()
            .partitionValue(pKey)
            .sortValue(toTitlePrefix)
            .build()
    );

    var request = QueryEnhancedRequest.builder()
        .queryConditional(qc)
        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .build();

    return Try.of(() -> table.query(request))
        .map(pages -> {
          List<MovieAndActor> movieAndActors = new ArrayList<>();
          pages.forEach(page -> movieAndActors.addAll(page.items()));
          return movieAndActors;
        })
    .onFailure(err -> {log.error("Query operation failed for partition: {}", pKey, err);});
  }
}
