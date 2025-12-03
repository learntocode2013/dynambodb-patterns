package com.github.learntocode2013.service;

import com.github.learntocode2013.model.EventEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service class for Event operations.
 * Demonstrates the "hot partition" pattern using sharding.
 */
public class EventService {
    private final DynamoDbTable<EventEntity> eventTable;
    public static final String TABLE_NAME = "Events";
    private final int SHARDS_NEEDED;

    /**
     * Constructor that takes a DynamoDbEnhancedClient.
     *
     * @param enhancedClient the DynamoDB enhanced client
     * @param shardCount the number of shards to use (for write distribution)
     */
    public EventService(DynamoDbEnhancedClient enhancedClient, int shardCount) {
        this.eventTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventEntity.class));
        this.SHARDS_NEEDED = shardCount;
    }

    /**
     * Create a table for Events if it doesn't exist.
     */
    public void createTableIfNotExists() {
        try {
            eventTable.describeTable();
        } catch (Exception e) {
            // Table doesn't exist, create it
            eventTable.createTable();
        }
    }

    /**
     * Record a new event using sharding to distribute write load.
     *
     * @param eventType the type of event
     * @param eventId a unique ID for the event
     * @param eventData the event data
     */
    public void recordShardedEvent(String eventType, String eventId, String eventData) {
        // 1. Calculate the shard ID
        int shardId = ThreadLocalRandom.current().nextInt(SHARDS_NEEDED);

        // 2. Construct the Partition Key
        String dateString = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String pk = String.format("EVENTS#%s#SHARD%d", dateString, shardId);

        // 3. Construct the Sort Key (timestamped)
        String sk = String.format("%d#%s", System.currentTimeMillis(), eventId);

        EventEntity event = new EventEntity();
        event.setPk(pk);
        event.setSk(sk);
        event.setEventType(eventType);
        event.setEventData(eventData);

        // Use Enhanced Client to perform the PutItemCommand
        eventTable.putItem(event);
    }

    /**
     * Retrieve all events for a specific date across all shards.
     * This demonstrates how to query multiple partitions in parallel and merge results.
     *
     * @param date the date to query for
     * @return a list of all events for the date across all shards
     */
    public List<EventEntity> getEventsByDate(LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ISO_DATE);
        List<CompletableFuture<List<EventEntity>>> futures = new ArrayList<>();

        // Query each shard in parallel
        for (int shardId = 0; shardId < SHARDS_NEEDED; shardId++) {
            String pk = String.format("EVENTS#%s#SHARD%d", dateString, shardId);

            // Create a future for each shard query
            CompletableFuture<List<EventEntity>> future = CompletableFuture.supplyAsync(() -> {
                // Query this specific shard
                QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                        .queryConditional(
                            QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(pk).build()
                            )
                        )
                        .build();

                // Collect all items from this shard
                return eventTable.query(request)
                        .items()
                        .stream()
                        .collect(Collectors.toList());
            });

            futures.add(future);
        }

        // Wait for all queries to complete and combine the results
        return futures.stream()
                .map(CompletableFuture::join) // This will block until all futures complete
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get all events of a specific type for a date across all shards.
     *
     * @param date the date to query for
     * @param eventType the event type to filter on
     * @return a list of matching events
     */
    public List<EventEntity> getEventsByDateAndType(LocalDate date, String eventType) {
        return getEventsByDate(date).stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .collect(Collectors.toList());
    }
}