package com.github.learntocode2013.service;

import com.github.learntocode2013.dynamodb.DynamoDBLocalTestUtil;
import com.github.learntocode2013.dynamodb.DynamoDBTableTestUtil;
import com.github.learntocode2013.model.EventEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for EventService using DynamoDB Local.
 * Demonstrates the sharding pattern for hot partition mitigation.
 */
class EventServiceTest {

    private static DynamoDbEnhancedClient dynamoDbClient;
    private static DynamoDbTable<EventEntity> eventTable;
    private EventService eventService;
    private static final int SHARD_COUNT = 5;
    private static final LocalDate TODAY = LocalDate.now();

    /**
     * Set up the DynamoDB Local server before all tests.
     */
    @BeforeAll
    static void setupClass() throws Exception {
        // Use in-memory DynamoDB for testing
        dynamoDbClient = DynamoDBLocalTestUtil.startInMemoryDynamoDB();

        // Create the table for testing
        eventTable = DynamoDBTableTestUtil.createTable(
                dynamoDbClient,
                EventEntity.class,
                EventService.TABLE_NAME);
    }

    /**
     * Clean up resources after all tests.
     */
    @AfterAll
    static void tearDownClass() throws Exception {
        // Clean up the table
        DynamoDBTableTestUtil.deleteTable(
                eventTable,
                dynamoDbClient,
                EventService.TABLE_NAME);

        // Stop the in-memory server
        DynamoDBLocalTestUtil.stopInMemoryDynamoDB();
    }

    /**
     * Set up each test with a fresh service instance.
     */
    @BeforeEach
    void setUp() {
        eventService = new EventService(dynamoDbClient, SHARD_COUNT);
    }

    /**
     * Test recording a single event.
     */
    @Test
    void testRecordSingleEvent() {
        // Record a test event
        String eventId = UUID.randomUUID().toString();
        eventService.recordShardedEvent("TEST_EVENT", eventId, "Test data");

        // Retrieve events for today
        List<EventEntity> events = eventService.getEventsByDate(TODAY);

        // Verify event was recorded
        assertEquals(1, events.size());
        EventEntity event = events.get(0);
        assertTrue(event.getSk().contains(eventId));
        assertEquals("TEST_EVENT", event.getEventType());
        assertEquals("Test data", event.getEventData());
    }

    /**
     * Test recording multiple events of different types and retrieving them by type.
     */
    @Test
    void testGetEventsByType() {
        // Record events of different types
        eventService.recordShardedEvent("TYPE_A", UUID.randomUUID().toString(), "Data A1");
        eventService.recordShardedEvent("TYPE_A", UUID.randomUUID().toString(), "Data A2");
        eventService.recordShardedEvent("TYPE_B", UUID.randomUUID().toString(), "Data B1");

        // Retrieve events by type
        List<EventEntity> typeAEvents = eventService.getEventsByDateAndType(TODAY, "TYPE_A");
        List<EventEntity> typeBEvents = eventService.getEventsByDateAndType(TODAY, "TYPE_B");

        // Verify correct events were retrieved
        assertEquals(2, typeAEvents.size());
        assertEquals(1, typeBEvents.size());

        typeAEvents.forEach(event -> assertEquals("TYPE_A", event.getEventType()));
        typeBEvents.forEach(event -> assertEquals("TYPE_B", event.getEventType()));
    }

    /**
     * Test that events are distributed across shards.
     * This test demonstrates how sharding helps prevent hot partitions.
     */
    @Test
    void testEventDistributionAcrossShards() throws Exception {
        // Number of events to write
        int eventCount = 100;

        // Use multiple threads to simulate high write throughput
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(eventCount);

        // Record many events in parallel
        for (int i = 0; i < eventCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    eventService.recordShardedEvent(
                            "BULK_TEST",
                            "event-" + index,
                            "Bulk test data " + index);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all events to be recorded
        latch.await();
        executor.shutdown();

        // Retrieve all events
        List<EventEntity> events = eventService.getEventsByDate(TODAY);

        // Verify all events were recorded
        assertEquals(eventCount, events.size());

        // Count events per partition key to verify distribution
        long distinctPartitionKeys = events.stream()
                .map(EventEntity::getPk)
                .distinct()
                .count();

        // Should have used multiple shards (not all events in one partition)
        assertTrue(distinctPartitionKeys > 1,
                "Events should be distributed across multiple shards, but found only "
                + distinctPartitionKeys + " distinct partition keys");

        // Ideally, we'd see close to SHARD_COUNT distinct partition keys
        // But due to random distribution, we can't guarantee exactly SHARD_COUNT
        System.out.println("Events distributed across " + distinctPartitionKeys + " shards");
    }
}