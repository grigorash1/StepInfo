package ru.grigorash.stepinfo.service;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.Toast;

import java.util.function.Consumer;
import java.util.logging.Logger;

import ru.grigorash.stepinfo.BuildConfig;

public class StepCounterEventListener implements SensorEventListener
{
    public interface IStepCounterListenerConsumer
    {
        void onSensorEvent(int dx_steps);
    }

    private final IStepCounterListenerConsumer m_consumer;
    private Integer m_first_steps_value;
    private Integer m_last_dx;

    public StepCounterEventListener(IStepCounterListenerConsumer consumer)
    {
        m_consumer = consumer;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if (sensorEvent.values[0] > Integer.MAX_VALUE)
        {
            if (BuildConfig.DEBUG)
                Logger.getGlobal().info("probably not a real value: " + sensorEvent.values[0]);
            return;
        }

        try
        {
            // first event - remember current sensor value
            if (m_first_steps_value == null)
            {
                m_first_steps_value = (int) sensorEvent.values[0];
                m_last_dx = 0;
                return;
            }
            int normalized_value = (int) sensorEvent.values[0] - m_first_steps_value;
            int current_dx = normalized_value - m_last_dx;
            m_last_dx = normalized_value;
            m_consumer.onSensorEvent(current_dx);
            /*
            m_total_today_steps += current_dx;
            m_steps_changed = true;
            sendDataToGUI();
             */
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
