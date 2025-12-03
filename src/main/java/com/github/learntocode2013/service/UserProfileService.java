package com.github.learntocode2013.service;

import com.github.learntocode2013.model.UserProfile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Optional;

/**
 * Service class for UserProfile operations.
 * Demonstrates simple Key-Value operations using DynamoDB.
 */
public class UserProfileService {
    private final DynamoDbTable<UserProfile> userTable;
    public static final String TABLE_NAME = "UserProfiles";

    /**
     * Constructor that takes a DynamoDbEnhancedClient.
     * This allows us to inject either a real AWS client or a local testing client.
     *
     * @param enhancedClient the DynamoDB enhanced client
     */
    public UserProfileService(DynamoDbEnhancedClient enhancedClient) {
        this.userTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UserProfile.class));
    }

    /**
     * Create a table for UserProfiles if it doesn't exist.
     */
    public void createTableIfNotExists() {
        try {
            userTable.describeTable();
        } catch (Exception e) {
            // Table doesn't exist, create it
            userTable.createTable();
        }
    }

    /**
     * Insert or update a user profile.
     *
     * @param profile the user profile to save
     */
    public void putUserProfile(UserProfile profile) {
        userTable.putItem(profile);
    }

    /**
     * Get a user profile by ID.
     * This is a pure key-value operation.
     *
     * @param userId the user ID
     * @return the user profile, or empty if not found
     */
    public Optional<UserProfile> getUserProfile(String userId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .build();

        UserProfile profile = userTable.getItem(
                GetItemEnhancedRequest.builder()
                        .key(key)
                        .build());

        return Optional.ofNullable(profile);
    }

    /**
     * Delete a user profile.
     *
     * @param userId the user ID
     */
    public void deleteUserProfile(String userId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .build();

        userTable.deleteItem(key);
    }
}