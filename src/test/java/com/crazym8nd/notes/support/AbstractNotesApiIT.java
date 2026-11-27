package com.crazym8nd.notes.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractNotesApiIT {

    private static final Path REALM_FILE =
            Path.of("infrastructure", "keycloak", "notes-realm.json").toAbsolutePath();

    static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.0"))
            .withCommand("start-dev --import-realm")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withFileSystemBind(REALM_FILE.toString(),
                    "/opt/keycloak/data/import/notes-realm.json", BindMode.READ_ONLY)
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/realms/notes").forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void keycloakIssuer(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", AbstractNotesApiIT::issuerUrl);
    }

    protected static final String SEEDED_NOTE_ID = "00000000-0000-0000-0000-000000000001";
    protected static final String SEEDED_CREATED_AT = "2026-01-01T10:00:00Z";

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String token;

    @BeforeAll
    void setUpInfrastructure() {
        token = fetchToken();
    }

    protected ResponseEntity<String> get(String url) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<Void>(bearer()), String.class);
    }

    protected ResponseEntity<String> getWithQuery(String url, String paramName, String paramValue) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<Void>(bearer()),
                String.class, java.util.Map.of(paramName, paramValue));
    }

    protected ResponseEntity<String> post(String url, String json) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(json, jsonBearer()), String.class);
    }

    protected ResponseEntity<String> put(String url, String json) {
        return rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, jsonBearer()), String.class);
    }

    protected ResponseEntity<String> delete(String url) {
        return rest.exchange(url, HttpMethod.DELETE, new HttpEntity<Void>(bearer()), String.class);
    }

    protected ResponseEntity<String> postWithoutToken(String url, String json) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(json), String.class);
    }

    protected JsonNode read(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Неожиданное тело ответа", e);
        }
    }

    protected List<String> idsOf(JsonNode array) {
        return array.valueStream().map(node -> node.path("id").asText()).toList();
    }

    protected HttpHeaders jsonBearer() {
        HttpHeaders headers = bearer();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private static String issuerUrl() {
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
        }
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/notes";
    }

    private static String fetchToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(issuerUrl() + "/protocol/openid-connect/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "grant_type=password&client_id=notes-cli&username=demo&password=demo"))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return new ObjectMapper().readTree(response.body()).path("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить токен у Keycloak", e);
        }
    }
}
