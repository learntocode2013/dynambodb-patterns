package com.github.learntocode2013.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Simple User Profile entity with a Partition Key only.
 * This demonstrates a Key-Value store pattern.
 */
@DynamoDbBean
public class UserProfile {
    private String userId;
    private String email;
    private String status; // e.g., 'active', 'inactive'

    // The Partition Key (PK) is the only component of the primary key
    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Default constructor required by the DynamoDbBean annotation
    public UserProfile() {}

    // Convenience constructor
    public UserProfile(String userId, String email, String status) {
        this.userId = userId;
        this.email = email;
        this.status = status;

    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}