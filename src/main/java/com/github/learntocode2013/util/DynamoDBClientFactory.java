package com.github.learntocode2013.util;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Factory class for creating DynamoDB clients for testing and production.
 */
public class DynamoDBClientFactory {

    /**
     * Create a DynamoDB client that connects to a real AWS endpoint.
     *
     * @param region the AWS region to use
     * @return a DynamoDbEnhancedClient configured to use the real AWS endpoint
     */
    public static DynamoDbEnhancedClient createProductionClient(Region region) {
        // Use credentials from ~/.aws/credentials
        DynamoDbClient standardClient = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(standardClient)
                .build();
    }

    /**
     * Create a DynamoDB client that connects to a local DynamoDB endpoint.
     * This is useful for testing and development.
     *
     * @param endpoint the local endpoint URL (e.g., "http://localhost:8000")
     * @return a DynamoDbEnhancedClient configured to use the local endpoint
     */
    public static DynamoDbEnhancedClient createLocalClient(String endpoint) {
        // Create fake AWS credentials for local development
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                "DUMMYACCESSKEY", "DUMMYSECRETKEY");

        // Configure the client to use the local endpoint
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // Region is required but doesn't matter for local
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        // Create and return the enhanced client
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * Convenience method that creates a client for the default local DynamoDB endpoint.
     *
     * @return a DynamoDbEnhancedClient configured for http://localhost:8000
     */
    public static DynamoDbEnhancedClient createLocalClient() {
        return createLocalClient("http://localhost:8000");
    }
}