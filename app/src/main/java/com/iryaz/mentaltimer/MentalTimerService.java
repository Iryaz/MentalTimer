package com.iryaz.mentaltimer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class MentalTimerService extends Service {

    public static final String MENTAL_TIMER_SERVICE_CONTROL_ACTION =
            "com.iryaz.mentaltimer.MentalTimerService.MENTAL_TIMER_SERVICE_CONTROL_ACTION";

    public static  final String STATE_CONTROL_KEY =
            "com.iryaz.mentaltimer.MentalTimerService.STATE_CONTROL_KEY";
    public static final String SET_TIMER_MINUTES_KEY =
            "com.iryaz.mentaltimer.MentalTimerService.SET_TIMER_MINUTES_KEY";

    public static final int STATE_CONTROL_START_TIMER = 1;
    public static final int STATE_CONTROL_STOP_TIMER = 0;

    public static Intent getIntent(Context context) {
        return new Intent(context, MentalTimerService.class);
    }

    public static void startSprint(Context context, int minutes) {
        Intent intent = new Intent(MENTAL_TIMER_SERVICE_CONTROL_ACTION);
        intent.putExtra(STATE_CONTROL_KEY, STATE_CONTROL_START_TIMER);
        intent.putExtra(SET_TIMER_MINUTES_KEY, minutes);
        context.sendBroadcast(intent);
    }

    public static void stopSprint(Context context) {
        Intent intent = new Intent(MENTAL_TIMER_SERVICE_CONTROL_ACTION);
        intent.putExtra(STATE_CONTROL_KEY, STATE_CONTROL_STOP_TIMER);
        context.sendBroadcast(intent);
    }


    public MentalTimerService() {
        mCurrentTimerState = SERVICE_STATE_TIMER_STOP;
    }

    public void onCreate() {
        mSoundBox = new SoundBox(this);
        registerReceiver(new MentalTimerServiceBroadcastCommandReceiver(),
                new IntentFilter(MENTAL_TIMER_SERVICE_CONTROL_ACTION));
    }

    public void onDestroy() {
        Log.i(TAG, "Service destroy");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, String.valueOf(mCurrentTimerState));
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_stat_in_yan)
                .setAutoCancel(false);

        Intent activityIntent = new Intent(this, WidgetActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(activityIntent);
        PendingIntent activityPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(activityPendingIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        Notification notification = mBuilder.build();
        startForeground(1, notification);

        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock =  mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tag");

        return START_STICKY;
    }
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final int NOTIFICATION_ID = 1;
    private final String TAG = "MentalTimerService";
    private final int SERVICE_STATE_TIMER_START = 1;
    private final int SERVICE_STATE_TIMER_STOP = 0;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private CountDownTimer mTimer;
    private int mCurrentTimerState;

    private int mCurrentMinute;
    private int mCurrentSecondes;
    private SoundBox mSoundBox;


    private class MentalTimerServiceBroadcastCommandReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == MENTAL_TIMER_SERVICE_CONTROL_ACTION) {

                if (intent.getIntExtra(STATE_CONTROL_KEY, STATE_CONTROL_STOP_TIMER) ==
                        STATE_CONTROL_START_TIMER) {

                    if (mCurrentTimerState == STATE_CONTROL_START_TIMER) {
                        mTimer.cancel();
                    }

                    int minutesNum = intent.getIntExtra(SET_TIMER_MINUTES_KEY, 0);
                    setNotificationCount(minutesNum, 0);
                    Log.i(TAG, "Timer start");
                    mCurrentTimerState = SERVICE_STATE_TIMER_START;
                    mCurrentMinute = minutesNum;
                    mCurrentSecondes = 0;
                    mTimer = getTimer(mCurrentMinute);
                    mTimer.start();

                }

                if (intent.getIntExtra(STATE_CONTROL_KEY, STATE_CONTROL_STOP_TIMER) ==
                        STATE_CONTROL_STOP_TIMER) {
                    setNotificationCount(0, 0);
                    mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                    Log.i(TAG, "Timer stop");
                    mCurrentTimerState = SERVICE_STATE_TIMER_STOP;
                    if (mTimer != null) mTimer.cancel();
                }
            }
        }
    }

    private void setNotificationCount(long minutes, long secondes) {
        String m, s;
        if (minutes < 10)
            m = "0" + String.valueOf(minutes);
        else
            m = String.valueOf(minutes);

        if (secondes < 10)
            s = "0" + String.valueOf(secondes);
        else
            s = String.valueOf(secondes);

        mBuilder.setContentText(m + ":" + s);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private CountDownTimer getTimer(long minutes) {
        return new CountDownTimer(1000 + minutes*60*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int m = mCurrentMinute;
                int s = mCurrentSecondes;
                if (s == 0) {
                    s = 59;
                    m--;
                } else {
                    s--;
                }

                setNotificationCount(m, s);
                mCurrentMinute = m;
                mCurrentSecondes = s;
                sendTickMessage();
            }

            @Override
            public void onFinish() {
                mCurrentTimerState = SERVICE_STATE_TIMER_STOP;
                Log.i(TAG, "Sprint Finish");
                sendFinishMessage();

                // Play sound
                mSoundBox.play("sounds/pacman_death.wav");
            }
        };
    }

    public static final String MENTAL_SERVICE_TICK_EVENT_ACTION =
            "com.iryaz.mentaltimer.MENTAL_SERVICE_TICK_EVENT_ACTION";

    public static final String MENTAL_SERVICE_TICK_MINUTES_KEY =
            "com.iryaz.mentaltimer.MENTAL_SERVICE_TICK_MINUTES_KEY";
    public static final String MENTAL_SERVICE_TICK_SECONDES_KEY =
            "com.iryaz.mentaltimer.MENTAL_SERVICE_TICK_SECONDES_KEY";

    public static final String MENTAL_SERVICE_TIMER_FINISH_EVENT_ACTION =
            "com.iryaz.mentaltimer.MENTAL_SERVICE_TIMER_FINISH_EVENT_ACTION";

    private void sendTickMessage() {
        Intent i = new Intent(MENTAL_SERVICE_TICK_EVENT_ACTION);
        i.putExtra(MENTAL_SERVICE_TICK_MINUTES_KEY, mCurrentMinute);
        i.putExtra(MENTAL_SERVICE_TICK_SECONDES_KEY, mCurrentSecondes);
        sendBroadcast(i);
    }

    private void sendFinishMessage() {
        Intent i = new Intent(MENTAL_SERVICE_TIMER_FINISH_EVENT_ACTION);
        sendBroadcast(i);
        // Start result activity
        Intent activityIntent = new Intent(this, WidgetActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);
    }
}
