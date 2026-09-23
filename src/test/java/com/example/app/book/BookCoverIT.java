package com.example.app.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.Browser;
import com.example.app.IntegrationTest;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class BookCoverIT extends IntegrationTest {

    @Autowired
    private JsonMapper json;

    private Browser alice;
    private long bookId;

    private Browser newReader(String name) {
        Browser browser = browser();
        browser.register(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com", "correct horse battery", name);
        return browser;
    }

    private static byte[] image(String format, int width, int height) {
        // JPEG : pas de canal alpha
        int type = format.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, width / 2, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format, out)) {
                throw new IllegalStateException("Pas d'encodeur pour " + format);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private JsonNode upload(Browser browser, byte[] content) {
        var response = browser.putFile("/api/books/" + bookId + "/cover", "file", "cover.png", "image/png", content);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json.readTree(response.body());
    }

    @BeforeEach
    void setUp() {
        alice = newReader("Alice");
        bookId = json.readTree(
                        alice.post("/api/books", """
                                {"title": "Dune", "rating": 5}""")
                                .body())
                .get("id")
                .asLong();
    }

    @Test
    void uploadedCoverIsReencodedAsJpegAndResized() throws IOException {
        JsonNode book = upload(alice, image("png", 1600, 2400));

        String coverUrl = book.get("coverUrl").asString();
        assertThat(coverUrl).startsWith("/api/books/" + bookId + "/cover?v=");
        var cover = alice.getBytes(coverUrl);
        assertThat(cover.statusCode()).isEqualTo(200);
        assertThat(cover.headers().firstValue("Content-Type")).hasValue("image/jpeg");
        assertThat(cover.headers().firstValue("Cache-Control"))
                .hasValueSatisfying(v -> assertThat(v).contains("private", "immutable"));
        BufferedImage stored = decode(cover.body());
        assertThat(stored.getWidth()).isEqualTo(533);
        assertThat(stored.getHeight()).isEqualTo(CoverImages.MAX_SIZE);

        assertThat(json.readTree(alice.get("/api/books").body())
                        .get(0)
                        .get("coverUrl")
                        .asString())
                .isEqualTo(coverUrl);
    }

    @Test
    void smallImagesKeepTheirSize() throws IOException {
        String url = upload(alice, image("jpeg", 300, 450)).get("coverUrl").asString();
        BufferedImage stored = decode(alice.getBytes(url).body());
        assertThat(stored.getWidth()).isEqualTo(300);
        assertThat(stored.getHeight()).isEqualTo(450);
    }

    @Test
    void replacingTheCoverChangesItsUrl() {
        String first = upload(alice, image("png", 100, 150)).get("coverUrl").asString();
        String second = upload(alice, image("png", 200, 300)).get("coverUrl").asString();
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        byte[] fake = "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8);
        var response = alice.putFile("/api/books/" + bookId + "/cover", "file", "cover.jpg", "image/jpeg", fake);
        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.body()).contains("JPEG ou PNG");

        var gif = alice.putFile("/api/books/" + bookId + "/cover", "file", "a.gif", "image/gif", image("gif", 10, 10));
        assertThat(gif.statusCode()).isEqualTo(415);
    }

    @Test
    void deletingTheCover() {
        String url = upload(alice, image("png", 100, 150)).get("coverUrl").asString();

        assertThat(alice.delete("/api/books/" + bookId + "/cover").statusCode()).isEqualTo(204);

        assertThat(alice.getBytes(url).statusCode()).isEqualTo(404);
        assertThat(json.readTree(alice.get("/api/books").body())
                        .get(0)
                        .get("coverUrl")
                        .isNull())
                .isTrue();
    }

    @Test
    void coversArePrivate() {
        String url = upload(alice, image("png", 100, 150)).get("coverUrl").asString();
        Browser bob = newReader("Bob");

        assertThat(bob.getBytes(url).statusCode()).isEqualTo(404);
        assertThat(bob.putFile("/api/books/" + bookId + "/cover", "file", "c.png", "image/png", image("png", 10, 10))
                        .statusCode())
                .isEqualTo(404);
        assertThat(browser().getBytes(url).statusCode()).isEqualTo(401);
    }

    @Test
    void deletingTheBookDeletesItsCover() {
        String url = upload(alice, image("png", 100, 150)).get("coverUrl").asString();
        alice.delete("/api/books/" + bookId);
        assertThat(alice.getBytes(url).statusCode()).isEqualTo(404);
    }
}
