package com.shrishailacademy.chaos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHAOS TEST 10 — Deployment Failure Resilience
 *
 * Validates that critical deployment infrastructure files exist and contain
 * the required configuration for safe deployments: Docker health checks,
 * restart policies, CI/CD guards, and production config.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeploymentFailureTest {

    @Test
    void dockerfileShouldExistAndContainHealthCheck() throws IOException {
        Path dockerfile = findProjectRoot().resolve("Dockerfile");
        assertThat(dockerfile.toFile()).exists();
        String content = Files.readString(dockerfile);
        // Should have a HEALTHCHECK instruction or expose /health
        assertThat(content).containsIgnoringCase("health");
    }

    @Test
    void dockerComposeProductionShouldHaveRestartPolicy() throws IOException {
        Path composeFile = findProjectRoot().resolve("docker-compose.prod.yml");
        assertThat(composeFile.toFile()).exists();
        String content = Files.readString(composeFile);
        assertThat(content).contains("restart");
    }

    @Test
    void applicationProductionPropertiesShouldExist() throws IOException {
        Path prodProps = findProjectRoot().resolve(
                "src/main/resources/application-prod.properties");
        assertThat(prodProps.toFile()).exists();
        String content = Files.readString(prodProps);
        // Should configure the database
        assertThat(content).containsIgnoringCase("spring.datasource");
    }

    @Test
    void pomXmlShouldContainSpringBootPlugin() throws IOException {
        Path pom = findProjectRoot().resolve("pom.xml");
        assertThat(pom.toFile()).exists();
        String content = Files.readString(pom);
        assertThat(content).contains("spring-boot-maven-plugin");
    }

    @Test
    void jacocoPluginShouldBeConfigured() throws IOException {
        Path pom = findProjectRoot().resolve("pom.xml");
        String content = Files.readString(pom);
        assertThat(content).contains("jacoco-maven-plugin");
    }

    @Test
    void renderYamlShouldExistForCloudDeployment() throws IOException {
        Path renderYaml = findProjectRoot().resolve("render.yaml");
        assertThat(renderYaml.toFile()).exists();
        String content = Files.readString(renderYaml);
        assertThat(content).containsIgnoringCase("service");
    }

    @Test
    void schemaFileShouldExist() throws IOException {
        Path schema = findProjectRoot().resolve("database/schema.sql");
        assertThat(schema.toFile()).exists();
        String content = Files.readString(schema);
        assertThat(content).containsIgnoringCase("CREATE TABLE");
    }

    @Test
    void productionConfigShouldNotContainTestSecrets() throws IOException {
        Path prodProps = findProjectRoot().resolve(
                "src/main/resources/application-prod.properties");
        if (prodProps.toFile().exists()) {
            String content = Files.readString(prodProps);
            // Production must not have test-mode secrets
            assertThat(content).doesNotContain("testsecretkey");
            assertThat(content).doesNotContain("create-drop");
        }
    }

    private Path findProjectRoot() {
        // Walk up from current directory to find pom.xml
        Path current = Path.of(System.getProperty("user.dir"));
        while (current != null) {
            if (current.resolve("pom.xml").toFile().exists()) {
                return current;
            }
            current = current.getParent();
        }
        // Fallback — try the workspace root
        return Path.of(System.getProperty("user.dir"));
    }
}
