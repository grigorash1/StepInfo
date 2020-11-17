package ru.grigorash.stepinfo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import ru.grigorash.stepinfo.adapter.SettingsAdapter;
import ru.grigorash.stepinfo.adapter.TracksAdapter;
import ru.grigorash.stepinfo.track.TrackInfo;
import ru.grigorash.stepinfo.track.TrackRecorder;

public class TracksListActivity extends AppCompatActivity
{
    private ListView      m_lv_tracks;
    private TracksAdapter m_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks_list);
        m_lv_tracks = findViewById(R.id.tracks_list);
        initSettingsAdapter();
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
        }
    }

}