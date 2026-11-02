package com.example.expensetracker.core.network.sync;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts between epoch millis and the ISO strings Postgres uses for timestamptz (java.time needs API 26). */
final class SyncTime {
    // e.g. 2026-11-02T09:01:02.123456+00:00, 2026-11-02T09:01:02Z
    private static final Pattern ISO = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}(?::?\\d{2})?)$");

    private SyncTime() {
    }

    static String format(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(millis);
    }

    /** Microseconds are cut to millis (SimpleDateFormat would read "123456" as 123456 ms). */
    static long parse(String iso) {
        Matcher m = iso == null ? null : ISO.matcher(iso.replace(' ', 'T'));
        if (m == null || !m.matches()) throw new IllegalArgumentException("Bad timestamp: " + iso);
        String fraction = m.group(2) == null ? "000" : (m.group(2) + "000").substring(0, 3);
        String zone = m.group(3);
        if (zone.equals("Z")) {
            zone = "+00:00";
        } else {
            String digits = zone.substring(1).replace(":", "");
            if (digits.length() == 2) digits += "00";
            zone = zone.charAt(0) + digits.substring(0, 2) + ":" + digits.substring(2);
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        try {
            return f.parse(m.group(1) + "." + fraction + zone).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Bad timestamp: " + iso, e);
        }
    }
}
