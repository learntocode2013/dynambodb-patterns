package com.github.learntocode2013;

import com.github.learntocode2013.model.UserProfile;
import com.github.learntocode2013.service.EventService;
import com.github.learntocode2013.service.UserProfileService;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DynamoDB Patterns Demo
 *
 * Note: This example uses real AWS credentials from your default profile.
 * To use with a local DynamoDB instance, see the test classes for examples.
 */
public class App
{
    public static void main(String[] args)
    {
        System.out.println("DynamoDB Patterns Demo");
        System.out.println("---------------------");

        // Create a standard DynamoDB client using AWS credentials
        // This will connect to the real AWS service!
        DynamoDbClient standardClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        // Create the enhanced client
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(standardClient)
                .build();

        // Create service instances
        UserProfileService userService = new UserProfileService(enhancedClient);
        EventService eventService = new EventService(enhancedClient, 5);

        System.out.println("Creating tables if they don't exist...");
        userService.createTableIfNotExists();
        eventService.createTableIfNotExists();

        // Demo operations
        System.out.println("\nKey-Value Pattern Example:");
        String userId = "demo-user-" + UUID.randomUUID().toString().substring(0, 8);
        UserProfile user = new UserProfile(userId, "demo@example.com", "active");

        System.out.println("Creating user: " + user);
        userService.putUserProfile(user);

        System.out.println("Retrieving user: " + userService.getUserProfile(userId).orElse(null));

        System.out.println("\nSharded Events Pattern Example:");
        String eventId = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("Recording event with ID: " + eventId);
        eventService.recordShardedEvent("DEMO_EVENT", eventId, "Demo event data");

        System.out.println("Retrieving events for today:");
        eventService.getEventsByDate(LocalDate.now())
                .forEach(System.out::println);

        System.out.println("\nDemo completed.");
    }
}
