package ru.grigorash.stepinfo.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

import java.util.Locale;

public class SettingsViewModel
{
    public static final String ACTION_SETTING_CHANGED = "StepInfo.SettingChanged";

    private static final String APP_PREFERENCES = "StepInfoSettings";
    public static final String STEPSIZE_SETTING = "stepsize";
    public static final String STEPSIZE_UNITS_SETTING = "stepsize_units";
    public static final String WEIGHT_SETTING = "weight";
    public static final String GOAL_SETTING = "goal";
    public static final String NOTIFICATION_SETTING = "notification";

    public static final String LAST_MAP_CENTER_SETTING = "last_map_center";
    public static final String LAST_MAP_SCALE_SETTING = "last_map_scale";

    public final static float DEFAULT_STEP_SIZE = Locale.getDefault() == Locale.US ? 2.5f : 75f;
    public final static String DEFAULT_STEP_UNIT = Locale.getDefault() == Locale.US ? "ft" : "cm";

    private final static int DEFAULT_GOAL = 10000;
    private final static int DEFAULT_WEIGHT = 70;

    private final SharedPreferences m_prefs;
    private final Context           m_context;

    public SettingsViewModel(@NonNull Context context)
    {
        m_context = context;
        m_prefs = m_context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public int getGoal()
    {
        return m_prefs.getInt(GOAL_SETTING, DEFAULT_GOAL);
    }

    public void setGoal(int new_value)
    {
        m_prefs.edit().putInt(GOAL_SETTING, new_value).commit();
        onSettingChanged(GOAL_SETTING);
    }

    public float getStepSize()
    {
        return m_prefs.getFloat(STEPSIZE_SETTING, DEFAULT_STEP_SIZE);
    }

    public void setStepSize(float new_value)
    {
        m_prefs.edit().putFloat(STEPSIZE_SETTING, new_value).commit();
        onSettingChanged(STEPSIZE_SETTING);
    }

    public String getStepUnit()
    {
        return m_prefs.getString(STEPSIZE_UNITS_SETTING, DEFAULT_STEP_UNIT);
    }

    public void setStepUnit(String new_value)
    {
        m_prefs.edit().putString(STEPSIZE_UNITS_SETTING, new_value).commit();
        onSettingChanged(STEPSIZE_UNITS_SETTING);
    }

    public int getWeight()
    {
        return m_prefs.getInt(WEIGHT_SETTING, DEFAULT_WEIGHT);
    }

    public void setWeight(int new_value)
    {
        m_prefs.edit().putInt(WEIGHT_SETTING, new_value).commit();
        onSettingChanged(WEIGHT_SETTING);
    }

    public GeoPoint getMapCenter()
    {
        String point_def = m_prefs.getString(LAST_MAP_CENTER_SETTING, "");
        if (TextUtils.isEmpty(point_def))
            return null;
        else
        {
            String[] parts = point_def.split(",");
            return new GeoPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        }
    }

    public void setMapCenter(GeoPoint new_value)
    {
        m_prefs.edit().putString(LAST_MAP_CENTER_SETTING, new_value.toDoubleString()).commit();
    }

    public float getMapScale()
    {
        float scale = m_prefs.getFloat(LAST_MAP_SCALE_SETTING, 17.75f);
        return (scale > 0) ? scale : 17.75f;
    }

    public void setMapScale(float new_value)
    {
        if (new_value > 0)
            m_prefs.edit().putFloat(LAST_MAP_SCALE_SETTING, new_value).commit();
    }

    public boolean getShowNotifications()
    {
        return m_prefs.getBoolean(NOTIFICATION_SETTING, true);
    }

    public void setShowNotifications(boolean new_value)
    {
        m_prefs.edit().putBoolean(NOTIFICATION_SETTING, new_value).commit();
    }

    private void onSettingChanged(String setting_name)
    {
        Intent intent = new Intent(ACTION_SETTING_CHANGED);
        intent.putExtra("setting_name", setting_name);
        m_context.sendBroadcast(intent);
    }

}
