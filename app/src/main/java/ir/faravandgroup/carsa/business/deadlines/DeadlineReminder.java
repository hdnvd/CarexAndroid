package ir.faravandgroup.carsa.business.deadlines;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public final class DeadlineReminder {
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final int MAX_TEXT_LENGTH = 100;

    public final long id;
    public final String title;
    public final String vehicleName;
    public final String dueDate;

    public DeadlineReminder(long id, String title, String vehicleName, String dueDate) {
        this.id = id;
        this.title = title;
        this.vehicleName = vehicleName;
        this.dueDate = dueDate;
    }

    public Calendar dueDayAt(int hour) {
        String[] parts = dueDate.split("-");
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]), hour, 0, 0);
        return calendar;
    }

    public static List<DeadlineReminder> listFromJson(String json) throws JSONException {
        List<DeadlineReminder> items = new ArrayList<>();
        if (json == null || json.trim().isEmpty())
            return items;
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null)
                continue;
            long id = item.optLong("id", 0);
            String title = limit(item.optString("title", ""));
            String vehicleName = limit(item.optString("vehicleName", ""));
            String dueDate = item.optString("dueDate", "");
            if (id <= 0 || title.isEmpty() || !DATE_PATTERN.matcher(dueDate).matches())
                continue;
            items.add(new DeadlineReminder(id, title, vehicleName, dueDate));
        }
        return items;
    }

    public static String listToJson(List<DeadlineReminder> items) {
        JSONArray array = new JSONArray();
        for (DeadlineReminder item : items) {
            try {
                array.put(new JSONObject()
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("vehicleName", item.vehicleName)
                        .put("dueDate", item.dueDate));
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    private static String limit(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }
}
