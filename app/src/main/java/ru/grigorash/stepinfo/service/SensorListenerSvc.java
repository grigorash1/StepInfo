package ru.grigorash.stepinfo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

import ru.grigorash.stepinfo.MainActivity;
import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.dao.StatisticDatabase;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;
import ru.grigorash.stepinfo.utils.API26Wrapper;

import static ru.grigorash.stepinfo.utils.CommonUtils.getCurrentDate;

public class SensorListenerSvc extends Service implements StepCounterEventListener.IStepCounterListenerConsumer
{
    public final static String ACTION_NAME = "ON_STEP";
    public final static String STEP_SECTION_NAME = "TODAY_STEP_COUNT";

    private final static int NOTIFICATION_ID = 1;
    private final static int SAVE_INTERVAL = 30 * 1000;
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;

    private int m_total_today_steps;
    private StatisticDatabase m_db;
    private Handler  m_save_handler;
    private Runnable m_save_worker;
    private long     m_last_save_date;
    private boolean  m_steps_changed;
    private StepCounterEventListener m_sensor_listener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_sensor_listener = new StepCounterEventListener(this);
        m_db = StatisticDatabase.getInstance(this);
        m_total_today_steps = m_db.getTodaySteps();
        sendDataToGUI();
        m_save_handler = new Handler();
        // save current steps count to Db every SAVE_INTERVAL seconds
        m_save_worker = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    saveStepsToDB();
                    showNotification();
                }
                finally
                {
                    m_save_handler.postDelayed(m_save_worker, SAVE_INTERVAL);
                }
            }
        };
        m_save_worker.run();
    }

    @Override
    public void onDestroy()
    {
        try
        {
            super.onDestroy();
            m_save_handler.removeCallbacks(m_save_worker);
            m_db.close();
        }
        catch (Exception e)
        {
            Toast.makeText(this, "onDestroy : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorEvent(int dx_steps)
    {
        try
        {
            m_total_today_steps += dx_steps;
            m_steps_changed = true;
            sendDataToGUI();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId)
    {
        try
        {
            reRegisterSensor();
            showNotification();
        }
        catch (Exception e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    private void sendDataToGUI()
    {
        Intent sendLevel = new Intent();
        sendLevel.setAction(ACTION_NAME);
        sendLevel.putExtra(STEP_SECTION_NAME, m_total_today_steps);
        sendBroadcast(sendLevel);
    }

    private synchronized void saveStepsToDB()
    {
        try
        {
            if (!m_steps_changed)
                return;
            long save_date = getCurrentDate();
            // next day
            if ((m_last_save_date != 0) && (m_last_save_date != save_date))
            {
                m_db.updateSteps(m_last_save_date, m_total_today_steps);
                m_total_today_steps = 0;
            }
            else
                m_db.updateTodaySteps(m_total_today_steps);
            m_last_save_date = save_date;
            m_steps_changed = false;
        }
        catch (Exception e)
        {
            Toast.makeText(this, "saveStepsToDB failed : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void reRegisterSensor()
    {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            try
            {
                sm.unregisterListener(m_sensor_listener);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            // enable batching with delay of max 5 min
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor != null)
            {
                boolean result = sm.registerListener(m_sensor_listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, (int)(5 * MICROSECONDS_IN_ONE_MINUTE));
                if (!result)
                    Toast.makeText(this, sensor.getName() + " registerListener failed", Toast.LENGTH_SHORT).show();
            }
    }

    private void showNotification()
    {
        SettingsViewModel vm = new SettingsViewModel(this);
        if (Build.VERSION.SDK_INT >= 26)
            startForeground(NOTIFICATION_ID, getNotification(this, vm));
        else if (vm.getShowNotifications())
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotification(this, vm));
    }

    public static Notification getNotification(final Context context, SettingsViewModel vm)
    {
        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        int goal = vm.getGoal();
        StatisticDatabase db = StatisticDatabase.getInstance(context);
        int steps = db.getTodaySteps(); // use saved value if we haven't anything better
        db.close();
        Notification.Builder notificationBuilder =
                Build.VERSION.SDK_INT >= 26 ? API26Wrapper.getNotificationBuilder(context) :
                        new Notification.Builder(context);
        if (steps > 0)
        {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            notificationBuilder.setProgress(goal, steps, false)
                    .setContentText(
                    steps >= goal ?
                            context.getString(R.string.goal_reached_notification, format.format(steps)) :
                            context.getString(R.string.notification_text, format.format((goal - steps))))
                    .setContentTitle(format.format(steps) + " " + context.getString(R.string.steps));
        }
        else
            notificationBuilder
                    .setContentText(context.getString(R.string.your_progress_will_be_shown_here_soon))
                    .setContentTitle(context.getString(R.string.notification_title));

        notificationBuilder
                .setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_step_info).setOngoing(true);
        return notificationBuilder.build();
    }
}
