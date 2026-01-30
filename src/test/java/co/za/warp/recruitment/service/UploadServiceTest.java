package co.za.warp.recruitment.service;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.*;

class UploadServiceTest {

    @SpyBean
    private UploadService uploadService;


}