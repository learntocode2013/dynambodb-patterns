package com.github.learntocode2013.dynamodb;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Utility class for setting up DynamoDB Local for testing.
 * Provides two methods for testing with DynamoDB:
 * 1. In-memory DynamoDB Local
 * 2. Docker-based Testcontainers
 */
public class DynamoDBLocalTestUtil {

    private static final String DEFAULT_PORT = "8000";
    private static DynamoDBProxyServer server;
    private static GenericContainer<?> dynamoDBContainer;

    /**
     * Start DynamoDB Local in-memory server for testing.
     * This method starts an in-memory DynamoDB server for testing
     * without requiring a Docker installation.
     *
     * @return DynamoDbEnhancedClient configured to use the local DynamoDB server
     * @throws Exception if server fails to start
     */
    public static DynamoDbEnhancedClient startInMemoryDynamoDB() throws Exception {
        if (server != null) {
            stopInMemoryDynamoDB();
        }

        System.setProperty("sqlite4java.library.path", "native-libs");

        // Create and start the local DynamoDB server
        server = ServerRunner.createServerFromCommandLineArgs(
            new String[]{"-inMemory", "-port", DEFAULT_PORT}
        );
        server.start();

        // Create and return a client connected to the local server
        return createLocalDynamoDBClient("http://localhost:" + DEFAULT_PORT);
    }

    /**
     * Stop the in-memory DynamoDB server.
     *
     * @throws Exception if server fails to stop
     */
    public static void stopInMemoryDynamoDB() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Start DynamoDB using Testcontainers (requires Docker).
     * This approach is more robust for CI/CD environments and doesn't have
     * platform-specific native library dependencies.
     *
     * @return DynamoDbEnhancedClient configured to use the containerized DynamoDB
     */
    public static DynamoDbEnhancedClient startTestContainerDynamoDB() {
        if (dynamoDBContainer != null && dynamoDBContainer.isRunning()) {
            dynamoDBContainer.stop();
        }

        dynamoDBContainer = new GenericContainer<>("amazon/dynamodb-local:latest")
                .withExposedPorts(8000);
        dynamoDBContainer.start();

        String endpoint = "http://" + dynamoDBContainer.getHost()
                + ":" + dynamoDBContainer.getFirstMappedPort();

        return createLocalDynamoDBClient(endpoint);
    }

    /**
     * Stop the DynamoDB Testcontainer.
     */
    public static void stopTestContainerDynamoDB() {
        if (dynamoDBContainer != null) {
            dynamoDBContainer.stop();
            dynamoDBContainer = null;
        }
    }

    /**
     * Create a DynamoDB client connected to the specified local endpoint.
     *
     * @param endpoint the local endpoint URL
     * @return DynamoDbEnhancedClient configured for local testing
     */
    private static DynamoDbEnhancedClient createLocalDynamoDBClient(String endpoint) {
        // Create fake AWS credentials for local development
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                "DUMMYACCESSKEY", "DUMMYSECRETKEY");

        // Configure the client to use the local endpoint
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // Region is required but doesn't matter for local DynamoDB
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        // Create and return the enhanced client
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}