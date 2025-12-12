package com.github.learntocode2013.service;

import com.github.learntocode2013.model.MovieAndActor;
import com.github.learntocode2013.model.MovieAndActor.Genre;
import com.github.learntocode2013.util.DynamoDBClientFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MovieAndActorServiceTest {
  private static final Logger log = LoggerFactory.getLogger(MovieAndActorServiceTest.class);
  private static final Map<String, List<MovieAndActor>> ITEMS = new HashMap<>();
  private static MovieAndActorService subject;

  @BeforeAll
  static void setUp() {
    ITEMS.putAll(generateItems());
    subject = new MovieAndActorService(DynamoDBClientFactory.createLocalClient());
    loadData(ITEMS);
  }

  @Test
  @Order(1)
  void tableCreationWorks() {
    var response = subject.createTableIfNotExists();
    Assertions.assertTrue(response.isSuccess());
  }


  @Test
  @Order(2)
  void fetchItemsUsing_KeyConditionExpressions() {
    var response = subject.queryItemsUsing_KeyConditionExpressions(
        "Tom Hanks",
        "R",
        "U");
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertEquals(1, response.get().size());
    response.get().forEach(item -> log.info("Fetched item:> {}", item.toString()));
  }

  private static void loadData(Map<String, List<MovieAndActor>> data) {
    for (Map.Entry<String, List<MovieAndActor>> entry : data.entrySet()) {
      entry.getValue().forEach(item -> subject.saveItem(item));
    }
  }

  private static Map<String, List<MovieAndActor>> generateItems() {
    Map<String, List<MovieAndActor>> items = new HashMap<>();
    items.put("Tom Hanks", List.of(
      MovieAndActor.builder()
          .actor("Tom Hanks")
          .year("1995")
          .movie("Toy Story")
          .role("Woody")
          .genre(Genre.CHILDREN)
          .build(),
      MovieAndActor.builder()
          .actor("Tom Hanks")
          .year("2000")
          .movie("Cast Away")
          .role("Chuck Noland")
          .genre(Genre.DRAMA)
          .build()
    ));
    items.put("Tim Allen", List.of(
        MovieAndActor.builder()
            .actor("Tim Allen")
            .year("1995")
            .movie("Toy Story")
            .role("Buzz LightYear")
            .genre(Genre.CHILDREN)
            .build()
    ));
    items.put("Natalie Portman", List.of(
        MovieAndActor.builder()
            .actor("Natalie Portman")
            .year("2010")
            .movie("Black Swan")
            .role("Nina Sayers")
            .genre(Genre.DRAMA)
            .build()
    ));
    return items;
  }
}