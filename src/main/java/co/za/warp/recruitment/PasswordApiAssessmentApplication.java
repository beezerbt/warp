package co.za.warp.recruitment;

import co.za.warp.recruitment.client.AuthenticationApiClient;
import co.za.warp.recruitment.config.AppProperties;
import co.za.warp.recruitment.domain.HttpResultDTO;
import co.za.warp.recruitment.service.AuthenticationService;
import co.za.warp.recruitment.service.DictionaryGeneratorService;
import co.za.warp.recruitment.service.UploadService;
import co.za.warp.recruitment.service.ZippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PasswordApiAssessmentApplication implements CommandLineRunner {

    static final Logger log = Logger.getLogger(AuthenticationApiClient.class.getName());

    //TODO::these should all be in a properties file or class
    public static final int EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT = 1296;
    public static final String WARP_AUTHENTICATION_URL = "https://recruitment.warpdevelopment.co.za/v2/api/authenticate";
    public static final String WARP_AUTHENTICATION_USER_NAME = "John";
    public static final String DICTIONARY_FILE_PREFIX = "dict";
    public static final String DICTIONARY_FILE_SUFFIX = ".txt";
    public static final String KAMBIZ_SHAHRI_CV_PDF_FILENAME = "Kambiz Shahri-2026.pdf";

    private final AuthenticationService authenticationService;
    private final ZippingService zippingService;
    private final UploadService uploadService;

    @Autowired
    public PasswordApiAssessmentApplication (AuthenticationService authenticationService,
                                             ZippingService zippingService, UploadService uploadService) {
        this.authenticationService = authenticationService;
        this.zippingService = zippingService;
        this.uploadService = uploadService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PasswordApiAssessmentApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //Generate the dictionary of possible passwords
        DictionaryGeneratorService dictionaryGeneratorService = new DictionaryGeneratorService();
        Path tmpFilePath = Files.createTempFile(DICTIONARY_FILE_PREFIX, DICTIONARY_FILE_SUFFIX);
        int count = dictionaryGeneratorService.generate(tmpFilePath);
        if(count != EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT) {
            throw new IllegalAccessException("Password list count was expected to be:" + EXPECTED_GENERATED_PASSWORD_TOTAL_COUNT);
        }
        //Generate dictionary and use the authentication service to get the upload URL
        List<String> fileEntriesList = dictionaryGeneratorService.readFileIntoList(tmpFilePath);
        Optional<String> authenticationResult = authenticationService.authenticateWithRateLimiter(WARP_AUTHENTICATION_URL, WARP_AUTHENTICATION_USER_NAME, fileEntriesList);
        if(authenticationResult.isEmpty()) {
            throw new IllegalAccessException("Authentication failed");
        }
        log.info("Authentication result was POS URL: " + authenticationResult.get());
        //Generate the zip file
        Path cv = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(KAMBIZ_SHAHRI_CV_PDF_FILENAME)).toURI());
        Path projectRoot = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI());
        byte[] zip = zippingService.buildZip(cv, tmpFilePath, projectRoot);
        //Upload it
        Optional<HttpResultDTO> zipUploadHttpResult= uploadService.uploadWithRateLimiter(authenticationResult.get(), zip);
        if(zipUploadHttpResult.isPresent()) {
            if(zipUploadHttpResult.get().statusCode() == 200) {
                log.info("Zip file uploaded successfully");
            } else {
                throw new IllegalAccessException("Failed to upload zip file. HTTP Status Code: " + zipUploadHttpResult.get().statusCode());
            }
        } else {
            throw new IllegalAccessException("Failed to upload zip file");
        }
    }
}