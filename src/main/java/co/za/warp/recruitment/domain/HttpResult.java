package co.za.warp.recruitment.domain;

public record HttpResult(int statusCode, String value) {
    public static HttpResult of(int statusCode, String value) {
        return new HttpResult(statusCode, value == null ? "" : value);
    }
}

