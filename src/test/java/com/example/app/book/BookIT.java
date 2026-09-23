package com.example.app.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.Browser;
import com.example.app.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class BookIT extends IntegrationTest {

    private static final String DUNE =
            """
            {"title": "  Dune ", "author": "Frank Herbert", "readOn": "2026-03-14", "rating": 5,
             "comment": "Un classique."}""";

    @Autowired
    private JsonMapper json;

    private Browser alice;

    private Browser newReader(String name) {
        Browser browser = browser();
        browser.register(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com", "correct horse battery", name);
        return browser;
    }

    private JsonNode body(java.net.http.HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    @BeforeEach
    void setUp() {
        alice = newReader("Alice");
    }

    @Test
    void booksRequireAuthentication() {
        assertThat(browser().get("/api/books").statusCode()).isEqualTo(401);
    }

    @Test
    void createListUpdateAndDelete() {
        var created = alice.post("/api/books", DUNE);
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode book = body(created);
        long id = book.get("id").asLong();
        assertThat(created.headers().firstValue("Location")).hasValue("/api/books/" + id);
        assertThat(book.get("title").asString()).isEqualTo("Dune");
        assertThat(book.get("rating").asInt()).isEqualTo(5);

        var list = body(alice.get("/api/books"));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("comment").asString()).isEqualTo("Un classique.");

        var updated =
                alice.put("/api/books/" + id, """
                {"title": "Dune", "rating": 4, "comment": ""}""");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(updated).get("rating").asInt()).isEqualTo(4);
        assertThat(body(updated).get("comment").isNull()).isTrue();
        assertThat(body(updated).get("author").isNull()).isTrue();

        assertThat(alice.delete("/api/books/" + id).statusCode()).isEqualTo(204);
        assertThat(body(alice.get("/api/books"))).isEmpty();
    }

    @Test
    void mostRecentReadingsComeFirst() {
        alice.post("/api/books", """
                {"title": "Sans date", "rating": 3}""");
        alice.post("/api/books", """
                {"title": "Ancien", "readOn": "2020-01-01", "rating": 3}""");
        alice.post("/api/books", """
                {"title": "Récent", "readOn": "2026-01-01", "rating": 3}""");

        var titles = body(alice.get("/api/books"))
                .valueStream()
                .map(b -> b.get("title").asString())
                .toList();

        assertThat(titles).containsExactly("Récent", "Ancien", "Sans date");
    }

    @Test
    void validatesInput() {
        assertThat(alice.post("/api/books", """
                {"title": "Dune", "rating": 6}""")
                        .statusCode())
                .isEqualTo(400);
        assertThat(alice.post("/api/books", """
                {"title": "Dune"}""")
                        .statusCode())
                .isEqualTo(400);
        assertThat(alice.post("/api/books", """
                {"title": " ", "rating": 3}""")
                        .statusCode())
                .isEqualTo(400);
        assertThat(alice.post(
                                "/api/books",
                                """
                {"title": "Futur", "readOn": "2999-01-01", "rating": 3}""")
                        .statusCode())
                .isEqualTo(400);
    }

    @Test
    void readersCannotSeeOrChangeEachOthersBooks() {
        long id = body(alice.post("/api/books", DUNE)).get("id").asLong();
        Browser bob = newReader("Bob");

        assertThat(body(bob.get("/api/books"))).isEmpty();
        assertThat(bob.put("/api/books/" + id, DUNE).statusCode()).isEqualTo(404);
        assertThat(bob.delete("/api/books/" + id).statusCode()).isEqualTo(404);
        assertThat(body(alice.get("/api/books"))).hasSize(1);
    }
}
