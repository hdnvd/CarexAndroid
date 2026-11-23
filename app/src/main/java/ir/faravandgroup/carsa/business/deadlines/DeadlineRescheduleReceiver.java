package ir.faravandgroup.carsa.business.deadlines;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DeadlineRescheduleReceiver extends BroadcastReceiver {
    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED));

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !ACTIONS.contains(intent.getAction()))
            return;
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                DeadlineReminderScheduler.rescheduleFromStore(context);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
