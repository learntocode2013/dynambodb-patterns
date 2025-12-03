package com.github.learntocode2013.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for creating and managing DynamoDB tables in tests.
 */
public class DynamoDBTableTestUtil {

    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private static final int POLL_INTERVAL_MS = 500;

    /**
     * Create a DynamoDB table from a bean class.
     * If the table already exists, it will be deleted and recreated.
     *
     * @param <T> the bean type
     * @param enhancedClient the DynamoDB enhanced client
     * @param tableClass the bean class
     * @param tableName the name of the table to create
     * @return DynamoDbTable instance for the created table
     */
    public static <T> DynamoDbTable<T> createTable(
            DynamoDbEnhancedClient enhancedClient,
            Class<T> tableClass,
            String tableName) {

        // Create a table schema from the bean class
        TableSchema<T> tableSchema = TableSchema.fromBean(tableClass);

        // Create a table object
        DynamoDbTable<T> table = enhancedClient.table(tableName, tableSchema);

        // Delete the table if it exists
        try {
            table.deleteTable();
            waitForTableDeletion(table);
        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, which is fine
        }

        // Create the table
        table.createTable();
        waitForTableCreation(table);

        return table;
    }

    /**
     * Delete a DynamoDB table.
     *
     * @param <T> the bean type
     * @param table the table to delete
     * @param enhancedClient the DynamoDB enhanced client
     * @param tableName the name of the table to delete
     */
    public static <T> void deleteTable(
            DynamoDbTable<T> table,
            DynamoDbEnhancedClient enhancedClient,
            String tableName) {

        try {
            table.deleteTable();
            waitForTableDeletion(table);
        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, which is fine
        }
    }


    private static <T> void waitForTableCreation(DynamoDbTable<T> dynamoDbTable) {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS);

            while (System.currentTimeMillis() < endTime) {
                try {
                    String tableStatus = dynamoDbTable
                        .describeTable()
                        .table()
                        .tableStatusAsString();

                    if (TableStatus.ACTIVE.toString().equals(tableStatus)) {
                        return;
                    }

                    TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
                } catch (ResourceNotFoundException e) {
                    // Table doesn't exist yet, keep waiting
                    TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
                }
            }

            throw new RuntimeException("Timed out waiting for table " + dynamoDbTable.tableName() + " to become active");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for table creation", e);
        }
    }

    private static <T> void waitForTableDeletion(DynamoDbTable<T> dynamoDbTable) {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS);

            while (System.currentTimeMillis() < endTime) {
                try {
                  dynamoDbTable.describeTable();
                  // If we get here, the table still exists
                  TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
                } catch (ResourceNotFoundException e) {
                    // Table is gone, we're done
                    return;
                }
            }

            throw new RuntimeException("Timed out waiting for table " + dynamoDbTable.tableName() + " to be deleted");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for table deletion", e);
        }
    }
}