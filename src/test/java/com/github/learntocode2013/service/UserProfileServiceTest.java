package com.github.learntocode2013.service;

import com.github.learntocode2013.dynamodb.DynamoDBLocalTestUtil;
import com.github.learntocode2013.dynamodb.DynamoDBTableTestUtil;
import com.github.learntocode2013.model.UserProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UserProfileService using DynamoDB Local.
 */
class UserProfileServiceTest {

    private static DynamoDbEnhancedClient dynamoDbClient;
    private static DynamoDbTable<UserProfile> userProfileTable;
    private UserProfileService userProfileService;

    /**
     * Set up the DynamoDB Local server before all tests.
     * You can choose either the in-memory or container approach.
     */
    @BeforeAll
    static void setupClass() throws Exception {
        // Option 1: Use in-memory DynamoDB (easier setup, no Docker required)
        dynamoDbClient = DynamoDBLocalTestUtil.startInMemoryDynamoDB();

        // Option 2: Use Testcontainers (requires Docker)
        // dynamoDbClient = DynamoDBLocalTestUtil.startTestContainerDynamoDB();

        // Create the table for testing
        userProfileTable = DynamoDBTableTestUtil.createTable(
                dynamoDbClient,
                UserProfile.class,
                UserProfileService.TABLE_NAME);
    }

    /**
     * Clean up resources after all tests.
     */
    @AfterAll
    static void tearDownClass() throws Exception {
        // Clean up the table
        DynamoDBTableTestUtil.deleteTable(
                userProfileTable,
                dynamoDbClient,
                UserProfileService.TABLE_NAME);

        // Option 1: Stop the in-memory server
        DynamoDBLocalTestUtil.stopInMemoryDynamoDB();

        // Option 2: Stop the container
        // DynamoDBLocalTestUtil.stopTestContainerDynamoDB();
    }

    /**
     * Set up each test with a fresh service instance.
     */
    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(dynamoDbClient);
    }

    /**
     * Test putting and getting a user profile.
     */
    @Test
    void testPutAndGetUserProfile() {
        // Create a test user profile
        UserProfile testProfile = new UserProfile(
                "user123",
                "test@example.com",
                "active");

        // Put the profile in the database
        userProfileService.putUserProfile(testProfile);

        // Get the profile from the database
        Optional<UserProfile> retrievedProfile = userProfileService.getUserProfile("user123");

        // Verify the profile was retrieved correctly
        assertTrue(retrievedProfile.isPresent(), "Profile should be present");
        UserProfile profile = retrievedProfile.get();
        assertEquals("user123", profile.getUserId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("active", profile.getStatus());
    }

    /**
     * Test retrieving a non-existent user profile.
     */
    @Test
    void testGetNonExistentProfile() {
        Optional<UserProfile> profile = userProfileService.getUserProfile("nonexistent");
        assertFalse(profile.isPresent(), "Profile should not be present");
    }

    /**
     * Test deleting a user profile.
     */
    @Test
    void testDeleteUserProfile() {
        // Create a test user profile
        UserProfile testProfile = new UserProfile(
                "user456",
                "delete@example.com",
                "active");

        // Put the profile in the database
        userProfileService.putUserProfile(testProfile);

        // Verify the profile exists
        assertTrue(userProfileService.getUserProfile("user456").isPresent());

        // Delete the profile
        userProfileService.deleteUserProfile("user456");

        // Verify the profile no longer exists
        assertFalse(userProfileService.getUserProfile("user456").isPresent());
    }
}