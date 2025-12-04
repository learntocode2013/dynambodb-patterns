#### Topic(s) to cover

-[x] Getting started
-[x] Learn basics
  - [x] Work with immutable data classes
  - [x] Use expressions and conditions
  - [x] Perform scans and queries
  - [ ] Perform batch operations
  - [ ] Perform transaction operations
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