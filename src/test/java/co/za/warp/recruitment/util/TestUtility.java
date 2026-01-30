package co.za.warp.recruitment.util;

import co.za.warp.recruitment.client.UploadApiClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
}
