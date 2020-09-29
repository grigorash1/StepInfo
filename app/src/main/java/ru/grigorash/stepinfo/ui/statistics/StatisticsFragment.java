package ru.grigorash.stepinfo.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.dao.DayStatistic;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

public class StatisticsFragment extends Fragment
{
    private static final String TAG = new Object() {}.getClass().getEnclosingClass().getName();

    private final static NumberFormat s_formatter = NumberFormat.getInstance(Locale.getDefault());

    private StatisticsViewModel m_homeViewModel;
    private PieChart m_pieChart;
    private PieModel m_sliceGoal, m_sliceCurrent;
    private TextView m_totalStepsView;
    private TextView m_totalCaloriesView;
    private int      m_goal;
    private boolean  m_showSteps;
    private SettingsViewModel m_prefs;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_prefs = new SettingsViewModel(getContext());
        m_showSteps = true;
        m_homeViewModel = ViewModelProviders.of(this).get(StatisticsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);
        m_totalStepsView = root.findViewById(R.id.steps);
        m_totalCaloriesView = root.findViewById(R.id.calories);
        m_pieChart = root.findViewById(R.id.piechart);
        if (m_pieChart != null)
            initialzePieChart(m_pieChart);
        m_homeViewModel.getSteps().observe(getViewLifecycleOwner(), new Observer<Integer>()
        {
            @Override
            public void onChanged(@Nullable Integer steps)
            {
                updateSteps(steps);
                updatePieChart(steps);
            }
        });
        m_homeViewModel.getLastWeekStatistic().observe(getViewLifecycleOwner(), new Observer<List<DayStatistic>>()
        {
            @Override
            public void onChanged(@Nullable List<DayStatistic> weekly_stat)
            {
                updateBars(weekly_stat);
            }
        });
        return root;
    }

    private void initialzePieChart(PieChart pieChart)
    {
        // slice for the steps taken today
        m_sliceCurrent = new PieModel("", 0, Color.parseColor("#99CC00"));
        m_sliceCurrent.setStartAngle(0);
        pieChart.addPieSlice(m_sliceCurrent);
        // slice for the "missing" steps until reaching the goal
        m_sliceGoal = new PieModel("", m_goal, Color.parseColor("#CC0000"));
        pieChart.addPieSlice(m_sliceGoal);
        pieChart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                m_showSteps = !m_showSteps;
                stepUnitsChanged();
            }
        });


        pieChart.setDrawValueInPie(false);
        pieChart.setPieRotation(0);
        pieChart.setUsePieRotation(false);
        pieChart.setUseCustomInnerValue(false);
        pieChart.startAnimation();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void stepUnitsChanged()
    {
        if (m_showSteps)
            ((TextView) getView().findViewById(R.id.unit)).setText(getString(R.string.steps));
        else
        {
            String unit = m_prefs.getStepUnit();
            unit = getString(unit.equals("cm") ?  R.string.km : R.string.mi);
            ((TextView)getView().findViewById(R.id.unit)).setText(unit);
        }
        int steps = m_homeViewModel.getSteps().getValue();
        updateSteps(steps);
        updatePieChart(steps);
        updateBars(m_homeViewModel.getLastWeekStatistic().getValue());
    }

    private void updateSteps(int steps_today)
    {
        try
        {
            float stepsize = m_prefs.getStepSize();
            float weight = m_prefs.getWeight();
            float distance_today = steps_today * stepsize;

            if (m_showSteps)
                m_totalStepsView.setText(s_formatter.format(steps_today));
            else // show distance
            {
                if (m_prefs.getStepUnit().equals("cm"))
                    distance_today /= 100000;
                else
                    distance_today /= 5280;
                m_totalStepsView.setText(s_formatter.format(distance_today));
            }
            m_totalCaloriesView.setText(getString(R.string.calorie, /*s_formatter.format(*/Math.round((distance_today / 100000) * 3.85 * weight)/*)*/));
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getStackTrace()[0].getMethodName(), e);
        }
    }

    private void updatePieChart(int steps_today)
    {
        m_sliceCurrent.setValue(steps_today);
        if (m_goal - steps_today > 0)
        {
            // goal not reached yet
            if (m_pieChart.getData().size() == 1)
            {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                m_pieChart.addPieSlice(m_sliceGoal);
            }
            m_sliceGoal.setValue(m_goal - steps_today);
        }
        else
        {
            // goal reached
            m_pieChart.clearChart();
            m_pieChart.addPieSlice(m_sliceCurrent);
        }
        m_pieChart.update();
    }

    // Updates the bar graph to show the steps/distance of the last week.
    private void updateBars(List<DayStatistic> weekly_stat)
    {
        SimpleDateFormat df = new SimpleDateFormat("E", Locale.getDefault());
        BarChart barChart = getView().findViewById(R.id.bargraph);
        if (barChart.getData().size() > 0) barChart.clearChart();
        //int steps;
        float distance, stepsize = 0;
        boolean stepsize_cm = true;
        if (!m_showSteps)
        {
            // load some more settings if distance is needed
            stepsize = m_prefs.getStepSize();
            stepsize_cm = m_prefs.getStepUnit().equals("cm");
        }
        barChart.setShowDecimal(!m_showSteps); // show decimal in distance view only
        BarModel bm;
        for (int i = weekly_stat.size() - 1; i > 0; i--)
        {
            DayStatistic current = weekly_stat.get(i);
            if (current.Steps > 0)
            {
                bm = new BarModel(df.format(new Date(current.Date)), 0,
                        (current.Steps > m_goal) ? Color.parseColor("#99CC00") : Color.parseColor("#0099cc"));
                if (m_showSteps)
                    bm.setValue(current.Steps);
                else
                {
                    distance = current.Steps * stepsize;
                    if (stepsize_cm)
                        distance /= 100000;
                    else
                        distance /= 5280;
                    distance = Math.round(distance * 1000) / 1000f; // 3 decimals
                    bm.setValue(distance);
                }
                barChart.addBar(bm);
            }
        }

        if (barChart.getData().size() > 0)
        {
            barChart.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
                    //Dialog_Statistics.getDialog(getActivity(), since_boot).show();
                }
            });
            barChart.startAnimation();
        }
        else
            barChart.setVisibility(View.GONE);
    }

    @Override
    public void onResume()
    {
        m_goal = m_prefs.getGoal();
        super.onResume();
        m_homeViewModel.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        m_homeViewModel.onPause();
    }

}