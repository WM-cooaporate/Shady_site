package com.shady.landing.common.slug;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** SEO-friendly, URL-safe slugs: lowercase ASCII letters, digits and single hyphens. */
@Component
public class SlugGenerator {

    public static final int MAX_LENGTH = 120;
    public static final Pattern VALID = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Slugifies the text; if nothing usable remains (e.g. Arabic-only input) a random slug is returned. */
    public String slugify(String text) {
        String base = text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKD);
        base = DIACRITICS.matcher(base).replaceAll("").toLowerCase(Locale.ROOT);
        base = NON_ALNUM.matcher(base).replaceAll("-");
        base = trimHyphens(base);
        if (base.length() > MAX_LENGTH) {
            base = trimHyphens(base.substring(0, MAX_LENGTH));
        }
        return base.isEmpty() ? "item-" + randomSuffix(8) : base;
    }

    /** Returns {@code base}, or {@code base-2}, {@code base-3}, ... — the first one not taken. */
    public String unique(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = base + '-' + i;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        return base + '-' + randomSuffix(6);
    }

    public boolean isValid(String slug) {
        return slug != null && slug.length() <= MAX_LENGTH + 8 && VALID.matcher(slug).matches();
    }

    private static String trimHyphens(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '-') {
            end--;
        }
        return s.substring(start, end);
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
