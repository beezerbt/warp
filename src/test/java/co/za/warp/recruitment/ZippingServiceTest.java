package co.za.warp.recruitment;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZippingServiceTest {

    @Test
    void zipContainsExpectedEntries() throws Exception {
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
    }

    // ---------------- helpers ----------------

    /**
     * Copies an entire directory tree (directories + files) from sourceDir to targetDir.
     * targetDir is created if needed.
     */
    private static void copyTree(Path sourceDir, Path targetDir) throws IOException {
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
    private static void copyFileIfExists(Path sourceFile, Path targetFile) throws IOException {
        if (Files.exists(sourceFile) && Files.isRegularFile(sourceFile)) {
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /** Reads all ZIP entry names from the in-memory zip bytes. */
    private static Set<String> readZipEntries(byte[] zipBytes) throws IOException {
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
    private static void assertZipContainsIfExists(Set<String> zipEntries, Path projectRoot, String fileName) {
        Path f = projectRoot.resolve(fileName);
        if (Files.exists(f) && Files.isRegularFile(f)) {
            assertTrue(zipEntries.contains(fileName), "ZIP should contain " + fileName);
        }
    }

    /** Returns true if a directory contains at least one regular file anywhere under it. */
    private static boolean directoryHasFiles(Path dir) {
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
    private static Path findProjectRoot(Path start) {
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
