package ir.faravandgroup.carsa.business.deadlines;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DeadlineReminderStore {
    private static final String PREFS_NAME = "carsa_deadline_reminders";
    private static final String KEY_LIST = "list";
    private static final String KEY_SCHEDULED_CODES = "scheduled_codes";
    private static final String KEY_PERMISSION_ASKED = "permission_asked";

    private DeadlineReminderStore() {
    }

    static List<DeadlineReminder> load(Context context) {
        try {
            return DeadlineReminder.listFromJson(prefs(context).getString(KEY_LIST, "[]"));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    static DeadlineReminder find(Context context, long id) {
        for (DeadlineReminder item : load(context)) {
            if (item.id == id)
                return item;
        }
        return null;
    }

    static void save(Context context, List<DeadlineReminder> items) {
        prefs(context).edit().putString(KEY_LIST, DeadlineReminder.listToJson(items)).apply();
    }

    static Set<Integer> scheduledCodes(Context context) {
        Set<Integer> codes = new HashSet<>();
        for (String code : prefs(context).getStringSet(KEY_SCHEDULED_CODES, new HashSet<>())) {
            try {
                codes.add(Integer.parseInt(code));
            } catch (NumberFormatException ignored) {
            }
        }
        return codes;
    }

    static void saveScheduledCodes(Context context, Set<Integer> codes) {
        Set<String> values = new HashSet<>();
        for (Integer code : codes)
            values.add(String.valueOf(code));
        prefs(context).edit().putStringSet(KEY_SCHEDULED_CODES, values).apply();
    }

    static boolean permissionAsked(Context context) {
        return prefs(context).getBoolean(KEY_PERMISSION_ASKED, false);
    }

    static void markPermissionAsked(Context context) {
        prefs(context).edit().putBoolean(KEY_PERMISSION_ASKED, true).apply();
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_LIST).remove(KEY_SCHEDULED_CODES).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
