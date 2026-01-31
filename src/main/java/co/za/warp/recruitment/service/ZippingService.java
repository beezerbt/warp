package co.za.warp.recruitment.service;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates a ZIP containing:
 * - CV PDF
 * - dict.txt
 * - project source files
 * TODO::N.B. the zip file will not contain empty folders of the project
 * Then Base64 encodes the ZIP bytes and POSTs JSON to the temporary upload URL.
 */
@Component
public class ZippingService {

    private static final long MAX_ZIP_BYTES = 5L * 1024L * 1024L; // 5MB

    public byte[] buildZip(Path cvPdfPath, Path dictPath, Path projectRoot) throws IOException {
        Objects.requireNonNull(cvPdfPath, "cvPdfPath must not be null");
        Objects.requireNonNull(dictPath, "dictPath must not be null");
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // CV TODO::must be refactored
            addFile(zos, cvPdfPath, "CV.pdf");
            // dict.txt
            addFile(zos, dictPath, "dict.txt");
            // Project content (best effort)
            Path srcDir = projectRoot.resolve("src");
            if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
                throw new IllegalArgumentException("Expected src directory does not exist: " + srcDir);
            }
            // Project content (best effort)
            addFolderIfExists(zos, srcDir, "src");
            addFileIfExists(zos, projectRoot.resolve("build.gradle"), "build.gradle");
            addFileIfExists(zos, projectRoot.resolve("settings.gradle"), "settings.gradle");
            addFileIfExists(zos, projectRoot.resolve("README.md"), "README.md");
            // ✅ add the missing ones you created in the test
            addFileIfExists(zos, projectRoot.resolve(".gitignore"), ".gitignore");
            addFileIfExists(zos, projectRoot.resolve("gradlew"), "gradlew");
            addFileIfExists(zos, projectRoot.resolve("gradlew.bat"), "gradlew.bat");
            zos.finish();
            return baos.toByteArray();
        }
    }

    private void addFileIfExists(ZipOutputStream zos, Path file, String entryName) throws IOException {
        if (Files.exists(file) && Files.isRegularFile(file)) {
            addFile(zos, file, entryName);
        }
    }

    private void addFolderIfExists(ZipOutputStream zos, Path path, String zipName) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("path does not exist: " + path);
        }
        if (Files.isRegularFile(path)) {
            addFile(zos, path, zipName);
            return;
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path exists but is not a file or directory: " + path);
        }
        // ✅ MINIMAL ADDITION: ensure directory entry exists so empty dirs appear in ZIP
        String dirEntryName = zipName.endsWith("/") ? zipName : zipName + "/";
        zos.putNextEntry(new ZipEntry(dirEntryName));
        zos.closeEntry();

        try (var stream = Files.walk(path)) {
            for (var it = stream.filter(Files::isRegularFile).iterator(); it.hasNext(); ) {
                Path file = it.next();
                String rel = path.relativize(file).toString().replace('\\', '/');
                String entryName = dirEntryName + rel;
                addFile(zos, file, entryName);
            }
        }
    }

    private void addFile(ZipOutputStream zos, Path file, String zipEntryName) throws IOException {
        ZipEntry entry = new ZipEntry(zipEntryName.replace('\\', '/'));
        entry.setTime(Files.getLastModifiedTime(file).toMillis());
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    public Path findProjectRoot(Path start) {
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
