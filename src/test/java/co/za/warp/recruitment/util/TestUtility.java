package co.za.warp.recruitment.util;

import co.za.warp.recruitment.client.UploadApiClient;
import co.za.warp.recruitment.service.ZippingService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtility {
    public static final String UPLOAD_API_CLIENT_429_RESPONSE_BODY_CONTENT = "You are rate limited. Try again in 5 minutes, along with an additional 15 minute penalty.";
    public static final int HTTP_STATUS_CODE_RATE_LIMIT_EXCEEDED = 429;

    public static final String UPLOAD_API_CLIENT_201_RESPONSE_BODY_CONTENT = "{\"ok\":true}";
    public static final int HTTP_STATUS_CODE_POST_SUCCEEDED = 201;

    public static final String UPLOAD_API_CLIENT_200_RESPONSE_BODY_CONTENT = "{\"ok\":true}";
    public static final int HTTP_STATUS_CODE_OK = 200;

    public static final String UPLOAD_API_CLIENT_204_RESPONSE_BODY_CONTENT = "";
    public static final int HTTP_STATUS_CODE_NO_CONTENT = 204;

    public static final String UPLOAD_API_CLIENT_400_RESPONSE_BODY_CONTENT = "{\"error\":\"Bad Request\"}";
    public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;

    public static final String UPLOAD_API_CLIENT_401_RESPONSE_BODY_CONTENT = "{\"error\":\"Unauthorized\"}";
    public static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;

    public static final String UPLOAD_API_CLIENT_403_RESPONSE_BODY_CONTENT = "{\"error\":\"Forbidden\"}";
    public static final int HTTP_STATUS_CODE_FORBIDDEN = 403;

    public static final String UPLOAD_API_CLIENT_404_RESPONSE_BODY_CONTENT = "{\"error\":\"Not Found\"}";
    public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;

    public static final String UPLOAD_API_CLIENT_500_RESPONSE_BODY_CONTENT = "{\"error\":\"Internal Server Error\"}";
    public static final int HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR = 500;

    public static UploadApiClient generateMockedUploadApiClient(int httpStatusCode,
                                                                String responseContent,
                                                                HttpClient mockedHttpClient,
                                                                HttpResponse<String> mockedHttpResponse
    ) throws IOException, InterruptedException {
        when(mockedHttpResponse.statusCode()).thenReturn(httpStatusCode);
        when(mockedHttpResponse.body()).thenReturn(responseContent);
        when(mockedHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockedHttpResponse);
        return new UploadApiClient(mockedHttpClient);
    }

    public static Path createSubmissionZip() throws Exception {
        ZippingService zippingService = new ZippingService();

        // --- Inputs that are not part of the repo tree ---
        Path cv = Files.createTempFile("cv", ".pdf");
        Files.writeString(cv, "%PDF-1.4 fake", StandardCharsets.UTF_8);

        Path dict = Files.createTempFile("dict", ".txt");
        Files.writeString(dict, "Password\n", StandardCharsets.UTF_8);

        // --- Create a temp "project" folder and copy the real project content into it ---
        Path realProjectRoot = findProjectRoot(Path.of("").toAbsolutePath());

        Path projectRoot = Files.createTempDirectory("proj");

        // Copy entire src directory (must exist for a real project)
        Path realSrc = realProjectRoot.resolve("src");
        copyTree(realSrc, projectRoot.resolve("src"));

        // Copy key root files if they exist in the real project
        copyFileIfExists(realProjectRoot.resolve("build.gradle"), projectRoot.resolve("build.gradle"));
        copyFileIfExists(realProjectRoot.resolve("settings.gradle"), projectRoot.resolve("settings.gradle"));
        copyFileIfExists(realProjectRoot.resolve("README.md"), projectRoot.resolve("README.md"));
        copyFileIfExists(realProjectRoot.resolve(".gitignore"), projectRoot.resolve(".gitignore"));
        copyFileIfExists(realProjectRoot.resolve("gradlew"), projectRoot.resolve("gradlew"));
        copyFileIfExists(realProjectRoot.resolve("gradlew.bat"), projectRoot.resolve("gradlew.bat"));

        // Sanity checks: src must exist and contain at least one file
        assertTrue(Files.exists(projectRoot.resolve("src")), "Temp project src/ should exist");
        assertTrue(directoryHasFiles(projectRoot.resolve("src")), "Temp project src/ should contain files");
        // --- Create ZIP ---
        byte[] zip = zippingService.buildZip(cv, dict, projectRoot);
        // --- Assert zip size <= 5 MiB ---
        assertNotNull(zip, "zip bytes should not be null");
        assertTrue(zip.length > 0, "zip bytes should not be empty");
        long maxBytes = 5L * 1024 * 1024; // 5 MiB
        assertTrue(zip.length <= maxBytes, "zip must be <= 5 MiB, but was " + zip.length + " bytes");
        // --- Write zip INSIDE the projectRoot (submission folder) ---
        Path zipFile = projectRoot.resolve("submission.zip");
        Files.write(zipFile, zip);
        assertTrue(Files.exists(zipFile), "zip file should exist inside projectRoot");
        assertTrue(Files.size(zipFile) > 0, "zip file should not be empty");
        // --- Verify ZIP contents ---
        Set<String> zipEntries = readZipEntries(zip);
        assertTrue(zipEntries.contains("CV.pdf"), "ZIP should contain CV.pdf");
        assertTrue(zipEntries.contains("dict.txt"), "ZIP should contain dict.txt");
        // Root files: assert only those that actually exist in the temp project
        assertZipContainsIfExists(zipEntries, projectRoot, "build.gradle");
        assertZipContainsIfExists(zipEntries, projectRoot, "settings.gradle");
        assertZipContainsIfExists(zipEntries, projectRoot, "README.md");
        assertZipContainsIfExists(zipEntries, projectRoot, ".gitignore");
        assertZipContainsIfExists(zipEntries, projectRoot, "gradlew");
        assertZipContainsIfExists(zipEntries, projectRoot, "gradlew.bat");
        // src: must contain at least one file entry
        assertTrue(
                zipEntries.stream().anyMatch(n -> n.startsWith("src/") && !n.endsWith("/")),
                "ZIP should contain files under src/"
        );
        return zipFile;
    }

    // ---------------- helpers ----------------

    /**
     * Copies an entire directory tree (directories + files) from sourceDir to targetDir.
     * targetDir is created if needed.
     */
    public static void copyTree(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Source dir does not exist or is not a directory: " + sourceDir);
        }

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Files.createDirectories(targetDir.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(file);
                Path dest = targetDir.resolve(relative);
                Files.createDirectories(dest.getParent());
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Copies a single file if it exists; no-op if missing. */
    public static void copyFileIfExists(Path sourceFile, Path targetFile) throws IOException {
        if (Files.exists(sourceFile) && Files.isRegularFile(sourceFile)) {
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /** Reads all ZIP entry names from the in-memory zip bytes. */
    public static Set<String> readZipEntries(byte[] zipBytes) throws IOException {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                entries.add(e.getName());
            }
        }
        return entries;
    }

    /** Asserts that a ZIP contains a given entry only if the file exists on disk under projectRoot. */
    public static void assertZipContainsIfExists(Set<String> zipEntries, Path projectRoot, String fileName) {
        Path f = projectRoot.resolve(fileName);
        if (Files.exists(f) && Files.isRegularFile(f)) {
            assertTrue(zipEntries.contains(fileName), "ZIP should contain " + fileName);
        }
    }

    /** Returns true if a directory contains at least one regular file anywhere under it. */
    public static boolean directoryHasFiles(Path dir) {
        try (var s = Files.walk(dir)) {
            return s.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Finds the project root by walking upwards until it finds a build marker.
     * Works whether the test is executed from the project root, a module root, or a build dir.
     */
    public static Path findProjectRoot(Path start) {
        Path p = start;
        while (p != null) {
            if (Files.exists(p.resolve("build.gradle"))
                    || Files.exists(p.resolve("settings.gradle"))
                    || Files.exists(p.resolve("pom.xml"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("Could not locate project root from: " + start);
    }
}
