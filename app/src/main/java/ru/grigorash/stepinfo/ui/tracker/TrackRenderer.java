package ru.grigorash.stepinfo.ui.tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.balsikandar.crashreporter.CrashReporter;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

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
    private final MapView  m_map;
    private final Polyline m_current_track;
    private final String   m_event_type;
    private BroadcastReceiver m_broadcastReceiver;
    private final Activity m_parent;

    public TrackRenderer(Activity parent, MapView map, String event_type, File initial_file, int color)
    {
        m_map = map;
        m_event_type = event_type;
        m_parent = parent;

        m_current_track = new Polyline();
        m_current_track.setGeodesic(true);
        m_current_track.getOutlinePaint().setColor(color);

        initFromFile(initial_file);
        initBroadcastReceiver();
        m_map.getOverlays().add(m_current_track);
        m_map.invalidate();
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
                    double lat = intent.getDoubleExtra("Lat", 0.0);
                    double lon = intent.getDoubleExtra("Lon", 0.0);
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

    public void removeTrack()
    {
        m_map.getOverlays().remove(m_current_track);
        m_map.invalidate();
    }

    private void initFromFile(File initial_file)
    {
        try (ITrackReader reader = TrackReaderFactory.getReader(initial_file))
        {
            ITrackRecord record = null;
            while ((record = reader.read()) != null)
            {
                switch (record.Type())
                {
                    case TrackRecorder.RECORD_POSITION:
                        RecordPosition rp = (RecordPosition)record;
                        m_current_track.addPoint(new GeoPoint(rp.lat, rp.lon));
                        break;
                    case TrackRecorder.RECORD_STOP:
                        RecordStop stop = (RecordStop)record;
                        GeoPoint startPoint = new GeoPoint(stop.lat, stop.lon);
                        Marker stopMarker = new Marker(m_map);
                        stopMarker.setPosition(startPoint);
                        stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        stopMarker.setIcon(ContextCompat.getDrawable(m_parent, R.drawable.ic_stop));
                        m_map.getOverlays().add(stopMarker);
                        break;
                }
            }
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
/*
        try (FileInputStream fs = new FileInputStream(initial_file))
        {
            DataInputStream ds = new DataInputStream(fs);
            byte[] signature = new byte[4];
            ds.read(signature, 0, signature.length);
            if (!Arrays.equals(signature, TrackRecorder.FILE_SIGNATURE))
                throw new Exception("invalid file");
            byte version = ds.readByte();
            if (version != TrackRecorder.FORMAT_VERSION)
                throw new Exception("invalid file");
            while (ds.available() > 0)
            {
                byte record_type = ds.readByte();
                switch (record_type)
                {
                    case TrackRecorder.RECORD_POSITION:
                        {
                            double alt = ds.readDouble();
                            float speed = ds.readFloat();
                            double lat = ds.readDouble();
                            double lon = ds.readDouble();
                            long time = ds.readLong();
                            m_current_track.addPoint(new GeoPoint(lat, lon));
                        }
                        break;
                    default:
                        throw new Exception("invalid file");
                }
            }
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
 */
    }
}
