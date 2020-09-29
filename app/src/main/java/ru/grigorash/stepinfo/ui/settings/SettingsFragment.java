package ru.grigorash.stepinfo.ui.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.adapter.SettingsAdapter;

public class SettingsFragment extends Fragment
{
    private static final String TAG = new Object() {}.getClass().getEnclosingClass().getName();

    private SettingsViewModel m_model;
    private ListView m_rv_settings;
    private SettingsAdapter m_adapter;
    private ArrayList<SettingsAdapter.SimpleSetting> m_settings;
    private View m_root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_model = new SettingsViewModel(getActivity());
        m_root = inflater.inflate(R.layout.fragment_settings, container, false);
        m_rv_settings = m_root.findViewById(R.id.rvSettings);
        initSettings();
        initSettingsAdapter();
        return m_root;
    }

    private void initSettings()
    {
        m_settings = new ArrayList<>();

        SettingsAdapter.SimpleSetting setting = new SettingsAdapter.SimpleSetting(getString(R.string.goal),
                getString(R.string.goal_summary, m_model.getGoal()),
                SettingsViewModel.GOAL_SETTING);
        m_settings.add(setting);

        float stepsize = m_model.getStepSize();
        String unit = m_model.getStepUnit();
        setting = new SettingsAdapter.SimpleSetting(getString(R.string.step_size),
                getString(R.string.step_size_summary, stepsize, unit),
                SettingsViewModel.STEPSIZE_SETTING);
        m_settings.add(setting);

        setting = new SettingsAdapter.SimpleSetting(getString(R.string.weight_size),
                getString(R.string.weight_summary, m_model.getWeight()),
                SettingsViewModel.WEIGHT_SETTING);
        m_settings.add(setting);
    }

    private void initSettingsAdapter()
    {
        if (m_rv_settings != null)
        {
            m_adapter = new SettingsAdapter(getActivity(), m_settings);
            m_rv_settings.setAdapter(m_adapter);
            m_rv_settings.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    try
                    {
                        final SettingsAdapter.SimpleSetting setting = (SettingsAdapter.SimpleSetting)m_adapter.getItem(position);
                        if (setting.getKey().equals(SettingsViewModel.GOAL_SETTING))
                        {
                            editNumericSetting(R.string.goal, Integer.toString(m_model.getGoal()), new_value ->
                            {
                                int new_goal = Integer.parseInt(new_value);
                                if (new_goal > 0)
                                {
                                    m_model.setGoal(new_goal);
                                    setting.setSummary(getString(R.string.goal_summary, new_goal));
                                    m_adapter.notifyDataSetInvalidated();
                                }
                            });
                        }
                        else if (setting.getKey().equals(SettingsViewModel.STEPSIZE_SETTING))
                        {
                            editNumericSetting(R.string.step_size, Float.toString(m_model.getStepSize()), new_value ->
                            {
                                float new_stepsize = Float.parseFloat(new_value);
                                if (new_stepsize > 0)
                                {
                                    m_model.setStepSize(new_stepsize);
                                    String unit = m_model.getStepUnit();
                                    setting.setSummary(getString(R.string.step_size_summary, new_stepsize, unit));
                                    m_adapter.notifyDataSetInvalidated();
                                }
                            });
                        }
                        else if (setting.getKey().equals(SettingsViewModel.WEIGHT_SETTING))
                        {
                            editNumericSetting(R.string.weight_size, Integer.toString(m_model.getWeight()), new_value ->
                            {
                                int new_weight = Integer.parseInt(new_value);
                                if (new_weight > 0)
                                {
                                    m_model.setWeight(new_weight);
                                    setting.setSummary(getString(R.string.weight_summary, new_weight));
                                    m_adapter.notifyDataSetInvalidated();
                                }
                            });
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, e.getStackTrace()[0].getMethodName(), e);
                    }
                }
            });
        }
    }

    private void editNumericSetting(int caption_res_id, String value, final Consumer<String> fn_on_edit_ok)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText ed = new EditText(getActivity());
        ed.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ed.setText(value);
        builder.setView(ed);
        builder.setTitle(caption_res_id);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) ->
        {
            ed.clearFocus();
            fn_on_edit_ok.accept(ed.getText().toString());
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }
/*
    @Override
    public boolean onPreferenceClick(final Preference preference)
    {
        AlertDialog.Builder builder;
        View v;

        if (preference.getKey().equals(getString(R.string.goal_setting)))
        {
            builder = new AlertDialog.Builder(getActivity());
            final NumberPicker np = new NumberPicker(getActivity());
            np.setMinValue(1);
            np.setMaxValue(100000);
            np.setValue(m_model.getGoal());
            builder.setView(np);
            builder.setTitle(R.string.set_goal);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    np.clearFocus();
                    m_model.setGoal(np.getValue());
                    preference.setSummary(getString(R.string.goal_summary, np.getValue()));
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            Dialog dialog = builder.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
        return false;
    }
     */
}