package com.example.truyendex.core;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SearchText {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("[\\p{Z}\\s]+");

    private SearchText() {}

    public static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String plain = MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT).replace('đ', 'd');
        return SPACES.matcher(plain).replaceAll(" ").trim();
    }
}
