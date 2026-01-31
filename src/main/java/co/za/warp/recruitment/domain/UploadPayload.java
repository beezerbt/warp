package co.za.warp.recruitment.domain;

public record UploadPayload(String data, String name, String surname, String email) {
    public static final String CANDIDATE_FIRST_NAME = "Kambiz";
    public static final String CANDIDATE_SURNAME = "Eghbali Tabar-Shahri";
    public static final String CANDIDATE_EMAIL_ADDRESS = "kambiz.shahri@gmail.com";
}
