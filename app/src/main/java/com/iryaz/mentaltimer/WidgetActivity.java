package com.iryaz.mentaltimer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class WidgetActivity extends AppCompatActivity {

    public WidgetActivity() {
        mInternalReceiver = new TimerWidgetActivityBroadcastReceiver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();

        mTimeSeekBar = (SeekBar)findViewById(R.id.timeSetting);
        mMinutes = (TextView)findViewById(R.id.mentalTimerMinutes);
        mSecondes = (TextView)findViewById(R.id.mentalTimerSecondes);

        mTimeSeekBar.setMax(TIMER_MAX_MINUTES);
        mTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setMinutesTextView(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Create new sprint
                int selectMinutes = Integer.valueOf(mMinutes.getText().toString());
                final Sprint currentSprint = new Sprint(selectMinutes);
                Log.i(TAG, "Create new Sprint: " + String.valueOf(currentSprint.getMinutes()) +
                        " " + "minutes");
                
                if (currentSprint.getMinutes() == 0) return;
                switchTimerUIState(TIMER_STATE_RUN_NEW_SPRINT);

                // Start timer
                setSecondesTextView(0);
                MentalTimerService.startSprint(WidgetActivity.this, selectMinutes);
            }
        });

        if (savedInstanceState != null) {
            int savedMinutes = Integer.valueOf(savedInstanceState.getString(STATE_MINUTE_BUNDLE_KEY));
            int savedSecondes = Integer.valueOf(savedInstanceState.getString(STATE_SECONDES_BUNDLE_KEY));

            mMinutes.setText(String.valueOf(savedMinutes));
            mSecondes.setText(String.valueOf(savedSecondes));
            mTimeSeekBar.setProgress(savedInstanceState.getInt(STATE_SETTING_TIME_BUNDLE_KEY));
            switchTimerUIState(savedInstanceState.getInt(CURRENT_STATE_TIMER_BUNDLE_KEY));
        } else {
            mTimeSeekBar.setProgress(TIMER_MAX_MINUTES);
        }

        if (isServiceRunning(MentalTimerService.class) == false) {
            Intent timerService = MentalTimerService.getIntent(this);
            startService(timerService);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MentalTimerService.MENTAL_SERVICE_TICK_EVENT_ACTION);
        filter.addAction(MentalTimerService.MENTAL_SERVICE_TIMER_FINISH_EVENT_ACTION);
        registerReceiver(mInternalReceiver, filter);
    }

    private boolean isServiceRunning(Class<?> serviceClassName) {
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClassName.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Stop activity");
    }

    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "Restart activity");
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_MINUTE_BUNDLE_KEY, mMinutes.getText().toString());
        outState.putString(STATE_SECONDES_BUNDLE_KEY, mSecondes.getText().toString());
        outState.putInt(STATE_SETTING_TIME_BUNDLE_KEY, mTimeSeekBar.getProgress());
        outState.putInt(CURRENT_STATE_TIMER_BUNDLE_KEY, mTimerUIState);
    }

    protected void switchTimerUIState(int newState) {
        mTimerUIState = newState;
        TextView splitTextView = (TextView)findViewById(R.id.splitTextView);

        switch (mTimerUIState) {
            case TIMER_STATE_RUN_NEW_SPRINT:
                mMinutes.setTextColor(Color.GREEN);
                mSecondes.setTextColor(Color.GREEN);
                mTimeSeekBar.setEnabled(false);
                if (splitTextView != null) splitTextView.setTextColor(Color.GREEN);
                break;
            case TIMER_STATE_STOP_NEW_SPRINT:
                mMinutes.setTextColor(Color.GRAY);
                mSecondes.setTextColor(Color.GRAY);
                mTimeSeekBar.setEnabled(true);
                if (splitTextView != null) splitTextView.setTextColor(Color.GRAY);
                break;
        }
    }

    private void setMinutesTextView(int minute) {
        if (minute < 10) {
            mMinutes.setText("0" + String.valueOf(minute));
        } else {
            mMinutes.setText(String.valueOf(minute));
        }
    }

    private void setSecondesTextView(int s) {
        if (s < 10) {
            mSecondes.setText("0" + String.valueOf(s));
        } else {
            mSecondes.setText(String.valueOf(s));
        }
    }

    private int mTimerUIState;
    private TimerWidgetActivityBroadcastReceiver mInternalReceiver;

    private final String STATE_MINUTE_BUNDLE_KEY = "StateMinuteBundleKey";
    private final String STATE_SECONDES_BUNDLE_KEY = "StateSecondesBundleKey";
    private final String STATE_SETTING_TIME_BUNDLE_KEY = "StateSettingsTimeBundleKey";
    private final String CURRENT_STATE_TIMER_BUNDLE_KEY = "StateTimerIsStartedKey";

    private final int TIMER_STATE_RUN_NEW_SPRINT = 1;
    private final int TIMER_STATE_STOP_NEW_SPRINT = 0;

    private static final int TIMER_MAX_MINUTES = 20;

    private final String TAG = "WidgetActivity";

    private SeekBar mTimeSeekBar;
    private TextView mMinutes;
    private TextView mSecondes;

    private class TimerWidgetActivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == MentalTimerService.MENTAL_SERVICE_TICK_EVENT_ACTION) {
                int m = intent.getIntExtra(MentalTimerService.MENTAL_SERVICE_TICK_MINUTES_KEY, 0);
                int s = intent.getIntExtra(MentalTimerService.MENTAL_SERVICE_TICK_SECONDES_KEY, 0);
                setMinutesTextView(m);
                setSecondesTextView(s);
                mTimeSeekBar.setProgress(m);
                mTimeSeekBar.setEnabled(false);
                switchTimerUIState(TIMER_STATE_RUN_NEW_SPRINT);
            }

            if (intent.getAction() == MentalTimerService.MENTAL_SERVICE_TIMER_FINISH_EVENT_ACTION) {
                switchTimerUIState(TIMER_STATE_STOP_NEW_SPRINT);
            }
        }
    }
}
