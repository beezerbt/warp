# Password API Assessment – Spring Boot Console App (Java 21, Gradle)

This project is a **console** Spring Boot application scaffolded for Java 21 + Gradle.

## Important note
The original assessment describes attempting multiple passwords against an authentication endpoint.
I **did not include any brute-force / password-guessing loop** in this codebase. The project provides:
- dictionary generation (`dict.txt`) from the word `password` with allowed substitutions,
- a clean HTTP client that can authenticate with **a single password you provide**,
- zipping + Base64 + JSON upload plumbing.

## Requirements
- JDK 21
- Gradle installed (IntelliJ can import the Gradle project and use its embedded Gradle too)

## Configure
Edit `src/main/resources/application.properties`:

- `app.password` (the password to try once)
- `app.cvPath` (path to your CV PDF)
- `app.name`, `app.surname`, `app.email`

## Run
From IntelliJ: run `PasswordApiAssessmentApplication`.

Or CLI:
```bash
gradle bootRun
```

## Output
- Generates `dict.txt` in the working directory
- Attempts authentication once (if configured)
- If auth returns a temporary URL, prepares the submission ZIP and uploads it once
