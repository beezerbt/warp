package co.za.warp.recruitment.service;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates dict.txt with all permutations of the word "password" where:
 * - each character can be uppercase or lowercase
 * - and 'a' can also be '@', 's' can also be '5', 'o' can also be '0'
 *
 * This class is deterministic and writes one password per line.
 */
@Component
public class DictionaryGeneratorService {

    private static final String BASE = "password";

    public int generate(Path outputPath) throws IOException {
        List<char[]> choices = new ArrayList<>();

        for (char c : BASE.toCharArray()) {
            choices.add(optionsFor(c));
        }

        try (BufferedWriter w = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            int[] count = {0};
            backtrackWrite(choices, 0, new StringBuilder(), w, count);
            return count[0];
        }
    }

    public List<String> readFileIntoList(Path filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (Stream<String> stream = Files.lines(filePath)) {
            stream.map(String::trim).forEach(lines::add);
        }
        return lines;
    }

    private void backtrackWrite(List<char[]> choices, int idx, StringBuilder sb, BufferedWriter w, int[] count) throws IOException {
        if (idx == choices.size()) {
            w.write(sb.toString());
            w.newLine();
            count[0]++;
            return;
        }

        for (char option : choices.get(idx)) {
            int len = sb.length();
            sb.append(option);
            backtrackWrite(choices, idx + 1, sb, w, count);
            sb.setLength(len);
        }
    }

    private char[] optionsFor(char c) {
        // base char
        List<Character> out = new ArrayList<>();

        // case variants where applicable
        char lower = Character.toLowerCase(c);
        char upper = Character.toUpperCase(c);

        out.add(lower);
        if (upper != lower) out.add(upper);

        // leet substitutions (for a, s, o) – keep as-is (no case)
        if (lower == 'a') out.add('@');
        if (lower == 's') out.add('5');
        if (lower == 'o') out.add('0');

        // de-duplicate while preserving order
        return distinctPreserveOrder(out);
    }

    private char[] distinctPreserveOrder(List<Character> chars) {
        List<Character> distinct = new ArrayList<>();
        for (Character ch : chars) {
            if (!distinct.contains(ch)) distinct.add(ch);
        }
        char[] arr = new char[distinct.size()];
        for (int i = 0; i < distinct.size(); i++) arr[i] = distinct.get(i);
        return arr;
    }
}
