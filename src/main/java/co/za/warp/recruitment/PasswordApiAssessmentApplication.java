package co.za.warp.recruitment;

import co.za.warp.recruitment.config.AppProperties;
import co.za.warp.recruitment.service.AuthenticationService;
import co.za.warp.recruitment.service.DictionaryGeneratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PasswordApiAssessmentApplication implements CommandLineRunner {

    public static final int EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT = 1296;
    private final AuthenticationService authenticationService;
    public PasswordApiAssessmentApplication (AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PasswordApiAssessmentApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String authUrl = "https://recruitment.warpdevelopment.co.za/v2/api/authenticate"; // Assign the authentication URL
        String username = "John"; // Assign the username
        DictionaryGeneratorService dictionaryGeneratorService = new DictionaryGeneratorService();
        Path tmpFilePath = Files.createTempFile("dict", ".txt");
        int count = dictionaryGeneratorService.generate(tmpFilePath);
        if(count != EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT) {
            throw new IllegalAccessException("Password list count was expected to be:" + EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT);
        }
        List<String> fileEntriesList = dictionaryGeneratorService.readFileIntoList(tmpFilePath);
        Optional<String> authenticationResult = authenticationService.authenticateWithRateLimiter(authUrl, username, fileEntriesList);
        authenticationResult.ifPresentOrElse(System.out::println, () -> System.out.println("Authentication failed."));
    }
}