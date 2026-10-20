package ir.sweetsoft.carex.business;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SweetDate {
    public static String getJalaliDateTimeForFileName() {
            android.icu.util.Calendar pc = android.icu.util.Calendar.getInstance(
                    new android.icu.util.ULocale("fa_IR@calendar=persian")
            );

            int year  = pc.get(android.icu.util.Calendar.YEAR);
            int month = pc.get(android.icu.util.Calendar.MONTH) + 1; // 0-based
            int day   = pc.get(android.icu.util.Calendar.DAY_OF_MONTH);
            int hour  = pc.get(android.icu.util.Calendar.HOUR_OF_DAY);
            int min   = pc.get(android.icu.util.Calendar.MINUTE);
            int sec   = pc.get(android.icu.util.Calendar.SECOND);

            return String.format(Locale.US, "%04d%02d%02d_%02d%02d%02d",
                    year, month, day, hour, min, sec);
    }

    public static String formatSmartDate(long millis) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Calendar sixDaysAgo = (Calendar) today.clone();
        sixDaysAgo.add(Calendar.DAY_OF_YEAR, -6);

        String timePart = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(millis));

        if (target.after(today)) {
            return "امروز " + timePart;
        }
        if (target.after(yesterday)) {
            return "دیروز " + timePart;
        }
        SimpleDateFormat weekDayFormat =
                new SimpleDateFormat("EEEE", new Locale("fa"));

        if (target.after(sixDaysAgo)) {
            return weekDayFormat.format(target.getTime()) + " " + timePart;
        }

        SimpleDateFormat fullFormat =
                new SimpleDateFormat("EEEE d", new Locale("fa"));

        return fullFormat.format(target.getTime()) + " ام " + timePart;
    }
}
