package com.craftstudio.launcher.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.craftstudio.launcher.InfoCenter;
import com.craftstudio.launcher.R;

import com.craftstudio.launcher.MainActivity;
import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.utils.NotificationUtils;

public class GameService extends Service {
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static boolean isActive;

    public static boolean isActive() {
        return isActive;
    }

    public static void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public void onCreate() {
        Tools.buildNotificationChannel(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf();
            Process.killProcess(Process.myPid());
            return START_NOT_STICKY;
        }
        Intent killIntent = new Intent(getApplicationContext(), GameService.class);
        killIntent.putExtra("kill", true);
        PendingIntent pendingKillIntent = PendingIntent.getService(this, NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE
                , killIntent, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Tools.NOTIFICATION_CHANNEL_DEFAULT)
                .setContentTitle(InfoCenter.replaceName(this, R.string.lazy_service_default_title))
                .setContentText(getString(R.string.notification_game_runs))
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,  getString(R.string.notification_terminate), pendingKillIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSilent(true);

        Notification notification = notificationBuilder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //SDK 34+
                serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            } else {
                serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
            }
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification, serviceType);
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification);
        }
        return START_NOT_STICKY; // non-sticky so android wont try restarting the game after the user uses the "Quit" button
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //At this point in time  only the game runs and the user poofed the window, time to die
        stopSelf();
        Process.killProcess(Process.myPid());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private static class IncomingHandler extends Handler {
        IncomingHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
        }
    }
}
