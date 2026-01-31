package co.za.warp.recruitment.service;

import co.za.warp.recruitment.util.TestUtility;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Path realProjectRoot = TestUtility.findProjectRoot(Path.of("").toAbsolutePath());

        Path projectRoot = Files.createTempDirectory("proj");

        // Copy entire src directory (must exist for a real project)
        Path realSrc = realProjectRoot.resolve("src");
        TestUtility.copyTree(realSrc, projectRoot.resolve("src"));

        // Copy key root files if they exist in the real project
        TestUtility.copyFileIfExists(realProjectRoot.resolve("build.gradle"), projectRoot.resolve("build.gradle"));
        TestUtility.copyFileIfExists(realProjectRoot.resolve("settings.gradle"), projectRoot.resolve("settings.gradle"));
        TestUtility.copyFileIfExists(realProjectRoot.resolve("README.md"), projectRoot.resolve("README.md"));
        TestUtility.copyFileIfExists(realProjectRoot.resolve(".gitignore"), projectRoot.resolve(".gitignore"));
        TestUtility.copyFileIfExists(realProjectRoot.resolve("gradlew"), projectRoot.resolve("gradlew"));
        TestUtility.copyFileIfExists(realProjectRoot.resolve("gradlew.bat"), projectRoot.resolve("gradlew.bat"));

        // Sanity checks: src must exist and contain at least one file
        assertTrue(Files.exists(projectRoot.resolve("src")), "Temp project src/ should exist");
        assertTrue(TestUtility.directoryHasFiles(projectRoot.resolve("src")), "Temp project src/ should contain files");
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
        Set<String> zipEntries = TestUtility.readZipEntries(zip);
        assertTrue(zipEntries.contains("CV.pdf"), "ZIP should contain CV.pdf");
        assertTrue(zipEntries.contains("dict.txt"), "ZIP should contain dict.txt");
        // Root files: assert only those that actually exist in the temp project
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, "build.gradle");
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, "settings.gradle");
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, "README.md");
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, ".gitignore");
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, "gradlew");
        TestUtility.assertZipContainsIfExists(zipEntries, projectRoot, "gradlew.bat");
        // src: must contain at least one file entry
        assertTrue(
                zipEntries.stream().anyMatch(n -> n.startsWith("src/") && !n.endsWith("/")),
                "ZIP should contain files under src/"
        );
    }

}
