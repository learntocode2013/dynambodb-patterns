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
    - [ ] **Condition Expressions:** Used in write operations to assert the existing condition
    (or non-condition) of an item before writing to it.
    - [ ] **Update Expressions:** Used in the update item call to describe the desired updates to an
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

#### How to start DynamoDb local ?

```shell
 java --enable-native-access=ALL-UNNAMED -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -sharedDb
```

---

#### Reference
[Modelling with enhanched dynamdodb client](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ddb-en-client-gs-tableschema.html)