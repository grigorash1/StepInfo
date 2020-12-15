package ru.grigorash.stepinfo.track;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.balsikandar.crashreporter.CrashReporter;

import org.osmdroid.views.Projection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RivalWatcher
{
    public static final String ACTION_RIVAL_MOVE = "ru.StepInfo.ACTION_RIVAL_MOVE";
    public static final String ACTION_RIVAL_FINISHED = "ru.StepInfo.ACTION_RIVAL_FINISHED";

    private ArrayList<RecordPosition> m_positions;
    private int                       m_current_position;
    private boolean                   m_playing;
    private final Context             m_context;

    public RivalWatcher(Context context)
    {
        m_context = context;
    }

    public List<RecordPosition> getTrackData()
    {
        return m_positions;
    }

    public void setTrack(String track_file)
    {
        try (ITrackReader reader = TrackReaderFactory.getReader(new File(track_file)))
        {
            m_positions = new ArrayList<>();
            m_current_position = 0;
            ITrackRecord record;
            while ((record = reader.read()) != null)
            {
                switch (record.Type())
                {
                    case TrackRecorder.RECORD_POSITION:
                        m_positions.add((RecordPosition)record);
                        break;
                }
            }
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    public void Start()
    {
        m_playing = true;
        animateMarker();
    }

    public void Stop()
    {
        m_playing = false;
    }

    private void nextPoint()
    {
        m_current_position++;
        if (m_current_position >= m_positions.size())
        {
            m_context.sendBroadcast(new Intent(ACTION_RIVAL_FINISHED));
            m_playing = false;
            m_current_position = 0;
        }
        else
            animateMarker();
    }

    private void animateMarker()
    {
        final RecordPosition currentPosition = m_positions.get(m_current_position);
        final RecordPosition toPosition = m_positions.get(m_current_position + 1);
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        //Projection proj = map.getProjection();
        //Point startPoint = proj.toPixels(marker.getPosition(), null);
        //final IGeoPoint startGeoPoint = proj.fromPixels(startPoint.x, startPoint.y);
        final long duration = toPosition.time - currentPosition.time;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!m_playing)
                    return;
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float)elapsed / duration);
                double lng = t * toPosition.lon + (1.0 - t) * currentPosition.lon;
                double lat = t * toPosition.lat + (1.0 - t) * currentPosition.lat;
                Intent intent = new Intent();
                intent.setAction(ACTION_RIVAL_MOVE);
                intent.putExtra("lat", lat);
                intent.putExtra("lon", lng);
                m_context.sendBroadcast(intent);
                // marker.setPosition(new GeoPoint(lat, lng)); // raise event
                if (t < 1.0)
                    handler.postDelayed(this, 50);
                else
                    nextPoint();
            }
        });
    }
}
