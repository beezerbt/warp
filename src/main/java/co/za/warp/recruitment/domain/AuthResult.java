package co.za.warp.recruitment.domain;

public record AuthResult(int statusCode, String url, String message) {

    public static AuthResult ok(String url) {
        return new AuthResult(200, url, "");
    }

    public static AuthResult of(int statusCode, String message) {
        return new AuthResult(statusCode, "", message == null ? "" : message);
    }

    public boolean isOk() { return statusCode == 200; }
}

