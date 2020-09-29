package ru.grigorash.stepinfo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.Map;

import ru.grigorash.stepinfo.service.SensorListenerSvc;
import ru.grigorash.stepinfo.ui.settings.SettingsFragment;
import ru.grigorash.stepinfo.ui.statistics.StatisticsFragment;
import ru.grigorash.stepinfo.ui.tracker.TrackerFragment;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = new Object() {}.getClass().getEnclosingClass().getName();

    private Map<Integer, Fragment> m_fragments;
    private Animation m_hide_title_animation;
    private Animation m_show_title_animation;
    private Toolbar m_toolbar;
    private BottomNavigationView m_nv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ContextCompat.startForegroundService(this, new Intent(this, SensorListenerSvc.class));
        setContentView(R.layout.activity_main);
        m_toolbar = findViewById(R.id.toolbar);
        m_hide_title_animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out);
        m_show_title_animation  = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in);
        setSupportActionBar(m_toolbar);
        m_nv = findViewById(R.id.navigation);
        initializeFragments();
        m_nv.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener()
                {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item)
                    {
                        return MainActivity.this.onNavigationItemSelected(item);
                    }
                });
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        try
        {
            View v = null;
            FragmentManager fm = getSupportFragmentManager();
            changeTitle(item.getTitle().toString());
            Fragment selected_fragment = m_fragments.get(item.getItemId());
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                    .replace(R.id.nav_host_fragment, selected_fragment)
            .commit();
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getStackTrace()[0].getMethodName(), e);
        }
        return true;
    }

    private TextView getToolbarTitle()
    {
        if (m_toolbar == null)
            return null;
        int childCount = m_toolbar.getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            View child = m_toolbar.getChildAt(i);
            if (child instanceof TextView)
                return (TextView)child;
        }
        return new TextView(this);
    }

    private void changeTitle(final String new_title)
    {
        final TextView titleTv = getToolbarTitle();
        if (titleTv == null)
            return;
        titleTv.startAnimation(m_hide_title_animation);

        m_hide_title_animation.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                m_toolbar.setTitle(new_title);
                titleTv.startAnimation(m_show_title_animation);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });
        titleTv.startAnimation(m_hide_title_animation);
    }

    private void initializeFragments()
    {
        FragmentManager fm = getSupportFragmentManager();
        // clear previous fragments
        for (Fragment fragment : fm.getFragments())
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        // initialize new fragments
        m_fragments = new HashMap();
        m_fragments.put(R.id.nav_statistics, new StatisticsFragment());
        m_fragments.put(R.id.nav_tracker, new TrackerFragment());
        m_fragments.put(R.id.nav_settings, new SettingsFragment());
        // navigate to the current selected
        onNavigationItemSelected(m_nv.getMenu().findItem(m_nv.getSelectedItemId()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("SelectedItemId", m_nv.getSelectedItemId());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int selectedItemId = savedInstanceState.getInt("SelectedItemId", 0);
        if (selectedItemId != 0)
            m_nv.setSelectedItemId(selectedItemId);
    }
}