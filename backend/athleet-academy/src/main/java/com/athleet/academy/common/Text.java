package com.athleet.academy.common;

import java.util.Locale;

public final class Text {

    private Text() {
    }

    /** Trimmed value, or null when blank. */
    public static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** A LIKE pattern with wildcards escaped, or null when there is nothing to search for. */
    public static String likePattern(String search) {
        String s = clean(search);
        if (s == null) return null;
        String escaped = s.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
