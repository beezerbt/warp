package co.za.warp.recruitment;

import co.za.warp.recruitment.service.DictionaryGeneratorService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryGeneratorServiceTest {

    public static final int EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT = 1296;

    @Test
    void generatesExpectedCountForPassword() throws Exception {
        // For "password":
        // p: 2 (p/P)
        // a: 3 (a/A/@)
        // s: 3 (s/S/5) [first s]
        // s: 3 (s/S/5) [second s]
        // w: 2
        // o: 3 (o/O/0)
        // r: 2
        // d: 2
        // Total = 2*3*3*3*2*3*2*2 = 1296
        DictionaryGeneratorService dictionaryGeneratorService = new DictionaryGeneratorService();
        Path tmpFilePath = Files.createTempFile("dict", ".txt");
        int count = dictionaryGeneratorService.generate(tmpFilePath);
        assertEquals(1296, count);
        assertTrue(Files.size(tmpFilePath) > 0);
        List<String> fileEntriesList = dictionaryGeneratorService.readFileIntoList(tmpFilePath);
        assertNotNull(fileEntriesList);
        assertEquals(EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT, fileEntriesList.size());
    }
}
