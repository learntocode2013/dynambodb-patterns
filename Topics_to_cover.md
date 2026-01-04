#### Topic(s) to cover

-[x] Getting started
-[x] Learn basics
  - [x] Work with immutable data classes
  - [x] Use expressions and conditions
    - [x] **Key Condition Expressions:** Used in Query API to describe which items we want to retrieve
    in our query.
    - [x] **Filter Expressions:** Used in Query and Scan operations to describe which items should be
    returned to the client after finding items that match our key condition expression.
    - [x] **Projection Expressions:** Used in all read operations to describe which attributes we want
    to return on the items that were read.
    - [x] **Condition Expressions:** Used in write operations to assert the existing condition
    (or non-condition) of an item before writing to it.
    - [x] **Update Expressions:** Used in the update item call to describe the desired updates to an
    existing item.
  - [x] Perform scans and queries
  - [x] Perform batch operations
  - [x] Perform transaction operations
  - [ ] Use secondary indices
-[ ] Use advanced mapping features
-[ ] Work with Json documents
-[ ] Customize operations with extensions
-[ ] Go asynchronous
-[ ] Annotation reference

---
#### The DynamoDB book chapters

- [x] Chapter-1
- [x] Chapter-2
- [x] Chapter-3
- [x] Chapter-4
- [x] Chapter-5
- [x] Chapter-6: Expressions
- [x] Chapter-7: How to approach data modelling in DynamoDB
  - [x] No joins
  - [x] Normalization
  - [x] Why denormalize with DynamoDB
  - [x] Multiple entity types per table
  - [x] Filtering
  - [x] Steps for modeling
    - [x] Create an entity relationship diagram
    - [x] Define your access patterns
    - [x] Model your primary key structure
    - [x] Handle additional access patterns with secondary indexes and streams
- [x] Chapter-8: What, Why and When of Single-Table design
  - [x] What is a single table design
  - [x] Why is single table design needed 
  - [x] The downsides of single table design
  - [x] When the downsides of single table design outweigh the benefits
- [x] Chapter-9: From modeling to implementation
  - [x] Separate application attributes from indexing attributes
  - [x] Implement your data model at the very boundary of your
    application
  - [x] Don’t reuse attributes across multiple indexes
  - [x] Add a "Type" attribute to every item
  - [x] Write scripts to help debug access patterns
  - [x] Shorten attribute names to save storage
- [x] Chapter-10: The importance of strategies
- [ ] Chapter-11: Strategies for one-to-many relationships
  - [ ] Denormalization by using a complex attribute
  - [ ] Denormalization by duplicating data
  - [ ] Composite primary key + the Query API action
  - [ ] Secondary index + the Query API action
  - [ ] Composite sort keys with hierarchical data
- [ ] Chapter-12
- [ ] Chapter-13
- [ ] Chapter-14
- [ ] Chapter-15
- [ ] Chapter-16
- [ ] Chapter-17
- [ ] Chapter-18
- [ ] Chapter-19
- [ ] Chapter-20
- [ ] Chapter-21
- [ ] Chapter-22

---

#### How to start DynamoDb local ?

```shell
 java --enable-native-access=ALL-UNNAMED -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -sharedDb
```

---


#### Reference
[Modelling with enhanched dynamdodb client](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ddb-en-client-gs-tableschema.html)