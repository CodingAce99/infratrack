package com.infratrack.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Architecture Boundary — No Micrometer in domain/ or application/")
class ArchitectureBoundaryTest {

    private static final Path DOMAIN_SRC = Paths.get("src/main/java/com/infratrack/domain");
    private static final Path APPLICATION_SRC = Paths.get("src/main/java/com/infratrack/application");
    private static final String FORBIDDEN_IMPORT = "io.micrometer";

    @Test
    @DisplayName("domain/ must contain zero io.micrometer imports")
    void domainHasNoMicrometerImports() throws IOException {
        assertNoForbiddenImports(DOMAIN_SRC, "domain/");
    }

    @Test
    @DisplayName("application/ must contain zero io.micrometer imports")
    void applicationHasNoMicrometerImports() throws IOException {
        assertNoForbiddenImports(APPLICATION_SRC, "application/");
    }

    private void assertNoForbiddenImports(Path sourceDir, String packageLabel) throws IOException {
        if (!Files.exists(sourceDir)) {
            fail("Source directory does not exist: " + sourceDir.toAbsolutePath());
        }

        List<String> violations;
        try (Stream<Path> files = Files.walk(sourceDir)) {
            violations = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .flatMap(p -> {
                        try {
                            return Files.lines(p)
                                    .filter(line -> line.contains("import " + FORBIDDEN_IMPORT))
                                    .map(line -> p.getFileName() + ": " + line.trim());
                        } catch (IOException e) {
                            return Stream.of(p.getFileName() + ": ERROR reading file");
                        }
                    })
                    .toList();
        }

        assertTrue(violations.isEmpty(),
                packageLabel + " must not import " + FORBIDDEN_IMPORT
                + ". Violations:\n  " + String.join("\n  ", violations));
    }
}
