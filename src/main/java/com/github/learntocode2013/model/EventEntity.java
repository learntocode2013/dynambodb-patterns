package com.github.learntocode2013.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Event entity that demonstrates a sharded pattern to prevent hot partitions.
 * The partition key includes a shard ID to distribute write load.
 */
@DynamoDbBean
public class EventEntity {
    // PK will be constructed: EVENTS#YYYY-MM-DD#SHARD{N}
    private String pk;

    // SK will be constructed: {Timestamp}#{EventId}
    private String sk;
    private String eventType;
    private String eventData;

    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    // Default constructor required by the DynamoDbBean annotation
    public EventEntity() {}

    @Override
    public String toString() {
        return "EventEntity{" +
                "pk='" + pk + '\'' +
                ", sk='" + sk + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventData='" + eventData + '\'' +
                '}';
    }
}