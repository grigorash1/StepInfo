package ru.grigorash.stepinfo.ui.statistics;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.dao.DayStatistic;
import ru.grigorash.stepinfo.dao.StatisticDatabase;
import ru.grigorash.stepinfo.service.StepCounterEventListener;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

public class StatisticsViewModel extends AndroidViewModel implements StepCounterEventListener.IStepCounterListenerConsumer
{
    private static final IntentFilter s_intentFilter;

    static
    {
        s_intentFilter = new IntentFilter();
        s_intentFilter.addAction(Intent.ACTION_TIME_TICK);
        s_intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        s_intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        s_intentFilter.addAction(SettingsViewModel.ACTION_SETTING_CHANGED);
    }

    private MutableLiveData<Integer> m_steps;
    private MutableLiveData<List<DayStatistic>> m_weekly_stat;
    private StepCounterEventListener m_sensor_listener;
    private BroadcastReceiver m_timeChangedReceiver;

    public StatisticsViewModel(Application app)
    {
        super(app);
        try
        {
            m_steps = new MutableLiveData<>();
            m_weekly_stat = new MutableLiveData<>();
            initializeTimeChangedReceiver();
            app.registerReceiver(m_timeChangedReceiver, s_intentFilter);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplication(), "HomeViewModel : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        getApplication().unregisterReceiver(m_timeChangedReceiver);
    }

    public void onResume()
    {
        registerSensor();
        updateValues();
    }

    public LiveData<Integer> getSteps()
    {
        return m_steps;
    }
    public LiveData<List<DayStatistic>> getLastWeekStatistic()
    {
        return m_weekly_stat;
    }

    @Override
    public void onSensorEvent(int dx_steps)
    {
        int new_value = m_steps.getValue() + dx_steps;
        m_steps.setValue(new_value);
    }

    private void updateValues()
    {
        m_steps.setValue(StatisticDatabase.getInstance(getApplication()).getTodaySteps());
        m_weekly_stat.setValue(StatisticDatabase.getInstance(getApplication()).getLastEntries(8));
    }

    private void initializeTimeChangedReceiver()
    {
        m_timeChangedReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String action = intent.getAction();
                if (action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED) || action.equals(SettingsViewModel.ACTION_SETTING_CHANGED))
                    updateValues();
            }
        };
    }

    private void registerSensor()
    {
        SensorManager sm = (SensorManager)getApplication().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null)
        {
            AlertDialog alert = new AlertDialog.Builder(getApplication()).setTitle(R.string.no_sensor)
                    .setMessage(R.string.no_sensor_explain)
                    .setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface)
                        {
                            System.exit(0);
                        }
                    }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            alert.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
            alert.show();
        }
        else
        {
            m_sensor_listener = new StepCounterEventListener(this);
            sm.registerListener(m_sensor_listener, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }

    }

    public void onPause()
    {
        try
        {
            SensorManager sm = (SensorManager)getApplication().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(m_sensor_listener);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}