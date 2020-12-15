package ru.grigorash.stepinfo;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;

import ru.grigorash.stepinfo.adapter.SettingsAdapter;
import ru.grigorash.stepinfo.adapter.TracksAdapter;
import ru.grigorash.stepinfo.track.TrackInfo;
import ru.grigorash.stepinfo.track.TrackRecorder;

public class TracksListActivity extends AppCompatActivity
{
    private ListView      m_lv_tracks;
    private TracksAdapter m_adapter;
    private Toolbar       m_toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks_list);
        m_lv_tracks = findViewById(R.id.tracks_list);
        m_toolbar = findViewById(R.id.toolbar);
        m_toolbar.setOnMenuItemClickListener(item -> onToolbarItemSelected(item));
        hideActionBar();
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        initSettingsAdapter();
    }

    @Override
    public void onBackPressed()
    {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    private boolean onToolbarItemSelected(MenuItem item)
    {
        hideActionBar();
        switch (item.getItemId())
        {
            case R.id.delete_track_file:
                deleteSelectedTrackFile();
                return true;
            default:
                return false;
        }
    }

    private void deleteSelectedTrackFile()
    {
        if (m_adapter.getSelected() >= 0)
        {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(R.string.delete_track_title);
            alertDialog.setMessage(R.string.delete_track_message);
            alertDialog.setPositiveButton(R.string.yes, (dialog, which) ->
            {
                m_adapter.deleteTrack(m_adapter.getSelected());
                m_adapter.setSelected(-1);
            });
            alertDialog.setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel());
            alertDialog.show();
        }
    }

    protected void hideActionBar()
    {
        if (m_toolbar != null)
            m_toolbar.animate().translationY(-112).setDuration(600L).start();
    }

    protected void showActionBar()
    {
        if (m_toolbar != null)
            m_toolbar.animate().translationY(0).setDuration(600L).start();
    }

    private void initSettingsAdapter()
    {
        if (m_lv_tracks != null)
        {
            m_adapter = new TracksAdapter(this, TrackRecorder.getTracksDir(this));
            m_lv_tracks.setAdapter(m_adapter);

            m_lv_tracks.setOnItemClickListener((parent, view, position, id) ->
            {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", ((TrackInfo)m_adapter.getItem(position)).fileUri());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            });

            m_lv_tracks.setOnItemLongClickListener((parent, view, position, id) ->
            {
                if (position == m_adapter.getSelected())
                {
                    m_adapter.setSelected(-1);
                    hideActionBar();
                }
                else
                {
                    m_adapter.setSelected(position);
                    showActionBar();
                }
                return true;
            });
        }
    }

}