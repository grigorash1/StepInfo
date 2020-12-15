package ru.grigorash.stepinfo.ui.tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.balsikandar.crashreporter.CrashReporter;

import org.apache.commons.lang3.StringUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.service.SensorListenerSvc;
import ru.grigorash.stepinfo.track.ITrackReader;
import ru.grigorash.stepinfo.track.ITrackRecord;
import ru.grigorash.stepinfo.track.RecordPosition;
import ru.grigorash.stepinfo.track.RecordStop;
import ru.grigorash.stepinfo.track.TrackReaderFactory;
import ru.grigorash.stepinfo.track.TrackRecorder;
import ru.grigorash.stepinfo.utils.CommonUtils;

public class TrackRenderer
{
    private final MapView     m_map;
    private final Polyline    m_current_track;
    private final String      m_event_type;
    private BroadcastReceiver m_broadcastReceiver;
    private final Activity    m_parent;

    public TrackRenderer(Activity parent, MapView map, String event_type, File initial_file, int color)
    {
        m_map = map;
        m_event_type = event_type;
        m_parent = parent;

        m_current_track = new Polyline();
        m_current_track.setGeodesic(true);
        m_current_track.getOutlinePaint().setColor(color);
        m_map.getOverlays().add(m_current_track);
        initFromFile(initial_file);
        if (!StringUtils.isEmpty(m_event_type))
            initBroadcastReceiver();
        m_map.invalidate();
    }

    public TrackRenderer(Activity parent, MapView map, List<RecordPosition> track_positions, int color)
    {
        m_map = map;
        m_event_type = null;
        m_parent = parent;

        m_current_track = new Polyline();
        m_current_track.setGeodesic(true);
        m_current_track.getOutlinePaint().setColor(color);
        m_map.getOverlays().add(m_current_track);
        initFromPositions(track_positions);
        m_map.invalidate();
    }

    private void initFromPositions(List<RecordPosition> track_positions)
    {
        for (RecordPosition pos :  track_positions)
            m_current_track.addPoint(new GeoPoint(pos.lat, pos.lon));
    }

    private void initBroadcastReceiver()
    {
        m_broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (m_event_type.equals(intent.getAction()))
                {
                    double lat = intent.getDoubleExtra("lat", 0.0);
                    double lon = intent.getDoubleExtra("lon", 0.0);
                    if ((lat != 0.0) && (lon != 0.0))
                    {
                        m_current_track.addPoint(new GeoPoint(lat, lon));
                        m_map.invalidate();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(m_event_type);
        m_parent.registerReceiver(m_broadcastReceiver, filter);
    }

    public Polyline polyline() {return  m_current_track; }

    public void removeTrack()
    {
        m_map.getOverlays().remove(m_current_track);
        for (int i = 0; i < m_map.getOverlays().size(); i++)
        {
            Overlay o = m_map.getOverlays().get(i);
            if (o instanceof Marker)
            {
                Marker m = (Marker)o;
                if (m.getId().startsWith("stop_sign_"))
                {
                    m_map.getOverlays().remove(i);
                    i--;
                }
            }
        }
        m_map.invalidate();
    }

    private void initFromFile(File initial_file)
    {
        try (ITrackReader reader = TrackReaderFactory.getReader(initial_file))
        {
            ITrackRecord record = null;
            RecordStop stop = null;
            while ((record = reader.read()) != null)
            {
                switch (record.Type())
                {
                    case TrackRecorder.RECORD_POSITION:
                        RecordPosition rp = (RecordPosition)record;
                        m_current_track.addPoint(new GeoPoint(rp.lat, rp.lon));
                        if (stop != null)
                        {
                            GeoPoint startPoint = new GeoPoint(stop.lat, stop.lon);
                            addStopMarker(m_parent, m_map, startPoint, rp.time - stop.time);
                            stop = null;
                        }
                        break;
                    case TrackRecorder.RECORD_STOP:
                        stop = (RecordStop)record;
                        break;
                }
            }
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    public static void addStopMarker(Context ctx, MapView map, GeoPoint point, long stop_time)
    {
        Marker stopMarker = new Marker(map);
        stopMarker.setPosition(point);

        stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        stopMarker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.ic_stop));
        stopMarker.setId(String.format("stop_sign_%d", map.getOverlays().size()));
        if (stop_time > 0)
        {
            int seconds = (int) ((stop_time / 1000) % 60);
            int minutes = (int) ((stop_time / 1000) / 60);
            stopMarker.setTitle(String.valueOf(minutes) + ":" + seconds);
        }
        map.getOverlays().add(stopMarker);
    }
}
