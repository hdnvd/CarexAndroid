package ir.faravandgroup.carex.business.deadlines;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import ir.faravandgroup.carex.R;
import ir.faravandgroup.carex.view.activities.MainActivity;

public class DeadlineReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL_ID = "deadline_reminders";
    private static final String DEADLINES_DEEP_LINK = "https://app.mycarex.ir/openapp/carex/deadlines";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DeadlineReminderScheduler.ACTION_REMIND.equals(intent.getAction()))
            return;
        long deadlineId = intent.getLongExtra(DeadlineReminderScheduler.EXTRA_DEADLINE_ID, 0);
        String dueDate = intent.getStringExtra(DeadlineReminderScheduler.EXTRA_DUE_DATE);
        int daysBefore = intent.getIntExtra(DeadlineReminderScheduler.EXTRA_DAYS_BEFORE, -1);
        DeadlineReminder deadline = DeadlineReminderStore.find(context, deadlineId);
        if (deadline == null || !deadline.dueDate.equals(dueDate) || stageIndex(daysBefore) < 0)
            return;
        if (!canNotify(context))
            return;
        ensureChannel(context);
        show(context, deadline, daysBefore);
    }

    static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false;
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null)
            return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.deadline_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.deadline_channel_description));
        manager.createNotificationChannel(channel);
    }

    @SuppressLint("MissingPermission")
    private static void show(Context context, DeadlineReminder deadline, int daysBefore) {
        int code = DeadlineReminderScheduler.requestCode(deadline.id, stageIndex(daysBefore));
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(DEADLINES_DEEP_LINK), context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, code, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String stageText = context.getString(stageTextResource(daysBefore));
        String text = deadline.vehicleName.isEmpty() ? stageText : deadline.vehicleName + " · " + stageText;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_deadline)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(deadline.title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(code, builder.build());
    }

    private static int stageIndex(int daysBefore) {
        int[] stages = DeadlineReminderScheduler.STAGE_DAYS_BEFORE;
        for (int i = 0; i < stages.length; i++) {
            if (stages[i] == daysBefore)
                return i;
        }
        return -1;
    }

    private static int stageTextResource(int daysBefore) {
        switch (daysBefore) {
            case 30:
                return R.string.deadline_stage_30_days;
            case 7:
                return R.string.deadline_stage_7_days;
            case 1:
                return R.string.deadline_stage_1_day;
            default:
                return R.string.deadline_stage_due_day;
        }
    }
}
