
## 🛠️ DynamoDB Enhanced Java Client

The Java **Enhanced Client** simplifies single-table design similar using annotations and fluent builders.

----------

you are performing:

    GetItem (Key-Value Access): You want to retrieve one unique item. Therefore, you must supply the full primary key (PK, or PK + SK).

    Query (Document/Collection Access): This operation is designed to retrieve a collection of items that share the same Partition Key. In this case, you only provide the Partition Key value, and optionally specify a condition (e.g., beginsWith, between) on the Sort Key to filter the collection. The Query operation cannot be performed without specifying the Partition Key.

----------



---

## 1. Simple Primary Key (Partition Key Only)

Use a **Simple Primary Key** when you need to store and retrieve unique entities where all access is based on that single unique identifier. This models a pure **Key-Value store**.

### **Goal: Unique Entity Lookup**

| Use Case | Partition Key (PK) | Sort Key (SK) | Query Pattern |
| :--- | :--- | :--- | :--- |
| **User Profile** | `USER#{UserID}` (e.g., `USER#12345`) | N/A | Get the profile for a specific user. |
| **Configuration Settings** | `CONFIG#{AppName}` | N/A | Get all settings for a specific application. |
| **Session Data** | `SESSION#{SessionID}` | N/A | Get a user's entire session object. |

### **Example Rationale**

In the **User Profile** example, every item has a globally unique `UserID`. You only ever want to fetch one item when you provide that ID. Adding a Sort Key provides no extra value and only adds complexity to the read operation.

---

## 2. Composite Primary Key (Partition Key + Sort Key)

Use a **Composite Primary Key** when you need to:
1.  **Model one-to-many relationships** (multiple items belong to a single parent).
2.  **Enable range queries** to fetch groups of related items (time-series, filtered lists).
3.  **Ensure uniqueness** based on two attributes (e.g., a specific issue within a specific repository).

### **Goal: One-to-Many Relationships & Range Queries**

| Use Case | Partition Key (PK) | Sort Key (SK) | Query Pattern | Source Context |
| :--- | :--- | :--- | :--- | :--- |
| **GitHub Issues** | `REPO#{Owner}#{Name}` | `ISSUE#{Number}` | [cite_start]Query all issues for a repository [cite: 168-171]. | Modeling GitHub |
| **Time-Series Events** | `USER#{UserID}` | `TIMESTAMP#{EventID}` | [cite_start]Query all events for a user within a time range (e.g., last 24 hours)[cite: 114]. | Time-Based Hot Partitions |
| **User Activity** | `USER#{UserID}#{YearMonth}` | `TIMESTAMP` | [cite_start]Query a user's activity for a specific month, ordered by time [cite: 131-135]. | Time-Based Hot Partitions |


In the **GitHub Issues** example, many issues (`ISSUE#1`, `ISSUE#2`, etc.) belong to one repository (`REPO#owner#name`)

* The **PK** (`REPO#owner#name`) groups all related items (all issues for that repo) into one partition.
* The **SK** (`ISSUE#number`) makes each item unique within that group and allows you to fetch them in order (e.g., querying issues 1 through 100).

---

## 🎯 The Critical DynamoDB Tradeoff

Choosing the correct key is vital because:

**You CANNOT change the Partition or Sort Key** of an existing item. Changing the key structure requires creating a new table and copying all data, which is "incredibly painful" and can cost thousands of dollars.
The Partition Key is the true **unit of scale**. Poor PK design (like low-cardinality keys) leads to **hot partitions** and throttling. The Sort Key allows you to model complex data *within* that scaled unit.

----------
The operations involving Optimistic Concurrency Control (OCC), Railroad Processing, and Lazy Migration primarily relied on single-item retrieval and update (`GetItem`, `UpdateItem`), which are classic Key-Value patterns, the example for **Eventual Consistency Retry** used a **`Query`** operation on a Global Secondary Index (GSI).

* **Key-Value Store:** Accessing a single, complete item by its **full, unique key** (Partition Key + Sort Key).
* **Document/Query Store:** Accessing a **collection of items** by Partition Key alone, or by querying an index using a range condition on the Sort Key. 
This is the foundation of Single Table Design.

-----

## DynamoDB as a Key-Value Store (Pure Access)

The purest Key-Value operation in DynamoDB is the `getItem` command, which retrieves a single, known item using the full Primary Key.

This example shows a simple user profile retrieved directly using a unique `userId` as the Partition Key and no Sort Key (a single-attribute primary key).

### Java Implementation

```java
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

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
}

public class KeyValueService {
    private final DynamoDbTable<UserProfile> userTable;

    public KeyValueService(DynamoDbEnhancedClient enhancedClient) {
        this.userTable = enhancedClient.table("UserProfileTable", TableSchema.fromBean(UserProfile.class));
    }

    /**
     * PURE Key-Value: Inserts a new item based on its unique key.
     */
    public void put(UserProfile profile) {
        // Equivalent to PutItemCommand, using userId as the key
        userTable.putItem(profile); 
    }

    /**
     * PURE Key-Value: Retrieves a single item by its full, unique key (userId).
     * This is the fastest, most predictable operation.
     */
    public UserProfile get(String userId) {
        Key key = Key.builder()
                .partitionValue(userId)
                .build();
        
        // This is a direct GetItem request to the main table.
        return userTable.getItem(GetItemEnhancedRequest.builder().key(key).build());
    }
}
```

----------

### 1. Hot Partitions: Sharding Writes (The "LIFE" Pattern)

The article's solution uses a random shard ID in the partition key (`pk: EVENTS#\${date}#SHARD\${shardId}`).

**Java Implementation (with Sharding Utility):**

Java

```
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.*;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@DynamoDbBean
public class EventEntity {
    // PK will be constructed: EVENTS#YYYY-MM-DD#SHARD{N}
    private String pk; 
    
    // SK will be constructed: {Timestamp}#{EventId}
    private String sk; 
    private String eventType;

    // Getters and Setters omitted for brevity...
    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}

public class EventService {
    private final DynamoDbTable<EventEntity> eventTable;
    private final int SHARDS_NEEDED = 5; // Matches the example

    public EventService(DynamoDbEnhancedClient enhancedClient) {
        this.eventTable = enhancedClient.table("GitHubTable", TableSchema.fromBean(EventEntity.class));
    }

    // Equivalent to recordEvent in the article (LIFE: Distribute across partitions)
    public void recordShardedEvent(String eventType, String eventId, Object eventData) {
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
        // Set other fields from eventData...

        // Use Enhanced Client to perform the PutItemCommand
        eventTable.putItem(event);
    }
}

```

----------

### 2. The Migration Problem: Lazy Migration ("Dual-Write and Lazy Migration")

This pattern updates an old item on read if a new attribute/GSI key is missing.

**Java Implementation (Read-Migrate-Write):**

Java

```
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class IssueRepository {
    private final DynamoDbTable<IssueEntity> issueTable;
    
    // Assumes IssueEntity has fields for priority, GSI5PK, and GSI5SK

    public IssueRepository(DynamoDbEnhancedClient enhancedClient) {
        this.issueTable = enhancedClient.table("GitHubTable", TableSchema.fromBean(IssueEntity.class));
    }

    // Equivalent to getIssue (Migrate on Read)
    public IssueEntity getIssueAndMigrate(String owner, String repoName, int issueNumber) {
        Key key = Key.builder()
                .partitionValue(String.format("REPO#%s#%s", owner, repoName))
                .sortValue(String.format("ISSUE#%s", String.valueOf(issueNumber).padStart(8, '0')))
                .build();

        IssueEntity issue = issueTable.getItem(key);

        if (issue != null && issue.getPriority() == null) { // New attribute is missing
            System.out.println("Migrating issue on read: " + issueNumber);
            
            // Recalculate and update the new field and GSI keys
            String priority = calculatePriority(issue); 
            String paddedIssueNumber = String.valueOf(issueNumber).padStart(8, '0');

            // --- Use low-level client for specific update operations ---
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(issueTable.tableName())
                .key(issueTable.key(issue).asMap()) // Get PK/SK map
                .updateExpression("SET #priority = :p, #gsi5pk = :g5pk, #gsi5sk = :g5sk")
                .expressionAttributeNames(Map.of(
                    "#priority", "priority",
                    "#gsi5pk", "GSI5PK",
                    "#gsi5sk", "GSI5SK"
                ))
                .expressionAttributeValues(Map.of(
                    ":p", AttributeValue.fromS(priority),
                    ":g5pk", AttributeValue.fromS(String.format("REPO#%s#%s", owner, repoName)),
                    ":g5sk", AttributeValue.fromS(String.format("PRIORITY#%s#%s", priority, paddedIssueNumber))
                ))
                .build();
            
            issueTable.dynamoDbClient().updateItem(updateRequest);
            
            // Update the in-memory object before returning
            issue.setPriority(priority); 
        }
        return issue;
    }
    
    // Placeholder for calculatePriority logic
    private String calculatePriority(IssueEntity issue) {
        // Based on article logic: if critical label, 'high', else 'medium'/'low'
        return "medium"; 
    }
}

```

----------

### 3. Eventual Consistency: Retry with Backoff

The article provides a function `waitForGSI` for critical GSI queries.

**Java Implementation (Retry Pattern with Backoff):**

Java

```
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

public class ConsistencyUtils {

    /**
     * Executes a query function and retries with exponential backoff until a condition is met.
     */
    public <T> List<T> waitForGSI(
            Supplier<List<T>> queryFn, 
            Predicate<List<T>> validateFn, 
            int maxAttempts) throws Exception {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            List<T> items = queryFn.get();
            
            if (validateFn.test(items)) {
                return items; // Item found, GSI caught up!
            }
            
            if (attempt < maxAttempts) {
                // Exponential backoff: 50ms, 100ms, 200ms, 400ms...
                long delayMs = 50 * (long) Math.pow(2, attempt - 1);
                System.out.printf("GSI lag detected. Attempt %d/%d, waiting %dms.%n", attempt, maxAttempts, delayMs);
                TimeUnit.MILLISECONDS.sleep(delayMs);
            }
        }
        throw new RuntimeException("GSI consistency timeout - item not found after retries.");
    }
    
    // Example Usage (Requires an IssueEntity and a method to query GSI4)
    public List<IssueEntity> getIssuesByStatusWithRetry(String owner, String repoName) throws Exception {
        // Assume issueTable is available here
        // Supplier to perform the GSI Query
        Supplier<List<IssueEntity>> querySupplier = () -> {
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .indexName("GSI4")
                .queryConditional(QueryConditional.sortBeginsWith(
                    Key.builder().partitionValue(String.format("REPO#%s#%s", owner, repoName)).build(),
                    "ISSUE#OPEN#" // Range condition
                ))
                .build();
            // This retrieves all items from the query result pages
            return issueTable.query(request)
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());
        };
        
        // Predicate to check if the newly created issue (e.g., issue #1) is present
        Predicate<List<IssueEntity>> validator = (items) -> 
            items.stream().anyMatch(i -> i.getIssueNumber() == 1);

        return waitForGSI(querySupplier, validator, 5);
    }
}

```

----------

## Consolidated Java DynamoDB Patterns

Here is a summary of the Java DynamoDB patterns we've covered, crucial for production-grade, scaled applications:

### 1. Optimistic Concurrency Control (OCC)

Used to prevent concurrent writes from overwriting each other, critical in high-latency, multi-tenant environments.

**Feature**

**Description**

**Java Implementation**

**Versioning**

Atomically increments an attribute with every successful write.

Use the `@DynamoDbVersionAttribute` annotation on a `Long` field in your Java model class.

**Handling**

Fails with `ConditionalCheckFailedException` on a version mismatch.

Catch `ConditionalCheckFailedException` and implement logic to re-read the latest item, reconcile, and retry.

Java

```
@DynamoDbBean
public class MessageState {
    // ... other keys ...
    @DynamoDbVersionAttribute
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

// In the service layer:
try {
    table.updateItem(newState); // OCC check happens automatically
} catch (ConditionalCheckFailedException e) {
    // Retry logic here
}

```

### 2. Hot Partition Mitigation (Sharding/Salting)

Used to distribute high-volume write traffic across multiple physical partitions, preventing throttling.

**Feature**

**Description**

**Java Implementation**

**Sharding Key**

Appending a random or calculated number (`#SHARD{N}`) to the partition key.

Use `ThreadLocalRandom.current().nextInt(N)` to generate a shard ID and construct the Partition Key string before the `putItem`.

**Read Tradeoff**

Requires **Fan-out Queries** (reading all shards concurrently) and client-side merging.

Use Java's `CompletableFuture.allOf()` to execute multiple `query()` calls in parallel against each shard's key.

Java

```
// Logic to generate a sharded Partition Key
public String generateShardedPK(String baseKey) {
    int shardId = ThreadLocalRandom.current().nextInt(SHARDS_NEEDED);
    return String.format("%s#SHARD%d", baseKey, shardId);
}

// Write operation uses the generated PK
EventEntity event = new EventEntity();
event.setPk(generateShardedPK("EVENTS#2025-11-21"));
eventTable.putItem(event);

```

### 3. Stream Processing (Railroad Processing)

Used in consumers to maintain sequential writes for the same key while maximizing overall concurrency, and for implementing local backpressure.

**Feature**

**Description**

**Java Implementation**

**Sequencing**

Ensures all operations for `Key A` run one after the other, regardless of concurrency for other keys.

Use a `ConcurrentMap<String, ExecutorService>` where each `ExecutorService` is a `newSingleThreadExecutor` ("rail") for a specific partition key.

**Concurrency**

Uses lightweight, scalable threads for I/O-bound DDB calls.

Use **Java Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) to back the single-threaded executors.

Java

```
// Simplified submission logic
public Future<?> submitWrite(String partitionKey, Runnable task) {
    // Submits task to the dedicated single-thread executor for that partitionKey
    ExecutorService executor = keyExecutors.computeIfAbsent(partitionKey, k -> 
        Executors.newSingleThreadExecutor(virtualThreadExecutor.submit(r).get())
    );
    return executor.submit(task);
}

```

### 4. Schema Migration (Lazy Migration on Read)

Used to gradually update older items to a new schema or populate a new GSI key without performing expensive, risky full table scans.

**Feature**

**Description**

**Java Implementation**

**Check on Read**

Checks for a new attribute (`if (item.getNewField() == null)`).

Use the standard `getItem` and perform a check.

**Migration**

Updates the item if the new field is missing.

Use the low-level `DynamoDbClient.updateItem()` with a `SET` expression to atomically add the new attribute and GSI keys.

Java

```
// Example condition check
if (issue != null && issue.getPriority() == null) {
    // Perform DDB updateItem request here to set missing fields
    // using the low-level client for maximum control.
}
return issue;

```
