package com.github.learntocode2013.service;

import com.github.learntocode2013.model.Customer;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class CustomerService {
  private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
  private static final String TABLE_NAME = "Customers";
  private final DynamoDbTable<Customer> customersTable;

  public CustomerService(DynamoDbEnhancedClient enhancedClient) {
    this.customersTable = enhancedClient.table(
        TABLE_NAME,
        TableSchema.fromBean(Customer.class));
  }

  public Try<DescribeTableEnhancedResponse> createTableIfNotExists() {
    return Try.of(
            customersTable::describeTable)
        .onFailure(err ->  {
          if (err instanceof ResourceNotFoundException) {
            log.info("Table {} not found, creating it now", TABLE_NAME);
            customersTable.createTable();
            return;
          }
          log.warn("Checking existence of `{}` table failed due to: {}",
              TABLE_NAME, err.getMessage());
        })
        .onSuccess(resp -> log.info("{} table already exists |"
            + " Created on: {}", TABLE_NAME, resp.table().creationDateTime()));
  }

  public Try<Void> putCustomer(Customer newCustomer) {
    return Try.runRunnable(() -> customersTable.putItem(newCustomer))
        .onFailure(err -> log.warn("Failed to add customer to table {} due to: {}",
            TABLE_NAME, err.getMessage()));
  }

  public Try<Void> updateCustomer(Customer newCustomer) {
    return Try.runRunnable(() -> customersTable.updateItem(newCustomer))
        .onFailure(err -> log.warn("Failed to update customer with id: {} due to: {}",
            newCustomer.getId(), err.getMessage()));
  }

  public Try<Void> deleteCustomer(String id) {
    return Try.runRunnable(() -> customersTable
        .deleteItem(Key.builder().partitionValue(id).build())
    ).onFailure(err -> log.warn("Failed to delete customer with id: {} due to: {}",
        id, err.getMessage()));
  }
}
