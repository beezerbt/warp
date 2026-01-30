package co.za.warp.recruitment.domain;

public record HttpResultDTO(int statusCode, String value) {
    public static HttpResultDTO of(int statusCode, String value) {
        return new HttpResultDTO(statusCode, value == null ? "" : value);
    }
}

