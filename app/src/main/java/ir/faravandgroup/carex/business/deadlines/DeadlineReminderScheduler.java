package ir.faravandgroup.carex.business.deadlines;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeadlineReminderScheduler {
    static final String ACTION_REMIND = "ir.faravandgroup.carex.action.DEADLINE_REMINDER";
    static final String EXTRA_DEADLINE_ID = "deadline_id";
    static final String EXTRA_DUE_DATE = "due_date";
    static final String EXTRA_DAYS_BEFORE = "days_before";
    static final int[] STAGE_DAYS_BEFORE = {30, 7, 1, 0};
    private static final int NOTIFY_HOUR = 10;
    private static final int MAX_DEADLINES = 50;

    private DeadlineReminderScheduler() {
    }

    public static synchronized void replaceAll(Context context, List<DeadlineReminder> deadlines) {
        Context app = context.getApplicationContext();
        cancelScheduled(app);
        List<DeadlineReminder> kept = new ArrayList<>(deadlines);
        kept.sort((a, b) -> a.dueDate.compareTo(b.dueDate));
        if (kept.size() > MAX_DEADLINES)
            kept = new ArrayList<>(kept.subList(0, MAX_DEADLINES));
        DeadlineReminderStore.save(app, kept);
        schedule(app, kept);
    }

    public static synchronized void rescheduleFromStore(Context context) {
        Context app = context.getApplicationContext();
        cancelScheduled(app);
        schedule(app, DeadlineReminderStore.load(app));
    }

    public static synchronized void clear(Context context) {
        Context app = context.getApplicationContext();
        cancelScheduled(app);
        DeadlineReminderStore.clear(app);
    }

    static int requestCode(long deadlineId, int stageIndex) {
        return (int) ((deadlineId * STAGE_DAYS_BEFORE.length + stageIndex) & 0x7fffffffL);
    }

    private static void schedule(Context context, List<DeadlineReminder> deadlines) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Set<Integer> codes = new HashSet<>();
        if (alarmManager == null) {
            DeadlineReminderStore.saveScheduledCodes(context, codes);
            return;
        }
        long now = System.currentTimeMillis();
        for (DeadlineReminder deadline : deadlines) {
            for (int stage = 0; stage < STAGE_DAYS_BEFORE.length; stage++) {
                Calendar trigger = deadline.dueDayAt(NOTIFY_HOUR);
                trigger.add(Calendar.DAY_OF_MONTH, -STAGE_DAYS_BEFORE[stage]);
                long triggerAt = trigger.getTimeInMillis();
                if (triggerAt <= now)
                    continue;
                int code = requestCode(deadline.id, stage);
                Intent intent = reminderIntent(context)
                        .putExtra(EXTRA_DEADLINE_ID, deadline.id)
                        .putExtra(EXTRA_DUE_DATE, deadline.dueDate)
                        .putExtra(EXTRA_DAYS_BEFORE, STAGE_DAYS_BEFORE[stage]);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, code, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                codes.add(code);
            }
        }
        DeadlineReminderStore.saveScheduledCodes(context, codes);
    }

    private static void cancelScheduled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int code : DeadlineReminderStore.scheduledCodes(context)) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, code, reminderIntent(context),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingIntent == null)
                continue;
            if (alarmManager != null)
                alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        DeadlineReminderStore.saveScheduledCodes(context, new HashSet<>());
    }

    private static Intent reminderIntent(Context context) {
        return new Intent(context, DeadlineReminderReceiver.class).setAction(ACTION_REMIND);
    }
}
