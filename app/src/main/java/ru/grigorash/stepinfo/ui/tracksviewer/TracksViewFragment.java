package ru.grigorash.stepinfo.ui.tracksviewer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.balsikandar.crashreporter.CrashReporter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.lang3.StringUtils;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.TracksListActivity;
import ru.grigorash.stepinfo.track.ITrackReader;
import ru.grigorash.stepinfo.track.TrackReaderFactory;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;
import ru.grigorash.stepinfo.ui.tracker.TrackRenderer;

import static ru.grigorash.stepinfo.track.TrackReaderFactory.*;
import static ru.grigorash.stepinfo.utils.CommonUtils.getBitmapFromVectorDrawable;
import static ru.grigorash.stepinfo.utils.CommonUtils.setMapCenter;

public class TracksViewFragment extends Fragment
{
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private final static int SELECT_TRACK_REQUEST_CODE = 222;

    private MapView              m_map;
    private SettingsViewModel    m_settings;
    private MyLocationNewOverlay m_myLocationOverlay;
    private FloatingActionButton m_btnSelectTrack;
    private TextView             m_text_info;
    private TrackRenderer        m_track_renderer;
    private Marker               m_finish_marker;
    private String               m_track_file;
    private Handler              m_handler;
    private Runnable             m_rotate_finish_marker;

    public TracksViewFragment()
    {
        super();
        m_handler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_settings = new SettingsViewModel(getActivity());

        View root = inflater.inflate(R.layout.fragment_tracks_viewer, container, false);
        Configuration.getInstance().load(getActivity().getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()));
        m_map = root.findViewById(R.id.map);
        m_map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        m_map.setMultiTouchControls(true);
        m_map.setFlingEnabled(true);
        //Add my location overlay
        m_myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getActivity()), m_map);
        m_myLocationOverlay.enableMyLocation();

        m_map.getOverlays().add(m_myLocationOverlay);
        m_map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        m_btnSelectTrack = root.findViewById(R.id.btnSelectTrack);
        m_btnSelectTrack.setOnClickListener(v -> onSelectTrackClick());
        m_text_info = root.findViewById(R.id.txtInfo);

        requestPermissionsIfNecessary(new String[]{
                // current location
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        if (savedInstanceState != null)
            m_track_file = savedInstanceState.getString("TrackFile");
        return root;
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("TrackFile", m_track_file);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setMapCenter(getActivity(), m_map, m_settings);
        m_map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        if (!StringUtils.isEmpty(m_track_file))
        {
            if ((m_map.getWidth() != 0) && (m_map.getHeight() != 0))
                initTrackFromFile(m_track_file);
            else
                new Handler().postDelayed(() -> initTrackFromFile(m_track_file), 100);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        m_map.onPause();
    }

    private void clearMap()
    {
        if (m_rotate_finish_marker != null)
        {
            m_handler.removeCallbacks(m_rotate_finish_marker);
            m_rotate_finish_marker = null;
        }
        if (m_finish_marker != null)
        {
            m_map.getOverlays().remove(m_finish_marker);
            m_finish_marker = null;
        }
        if (m_track_renderer != null)
        {
            m_track_renderer.removeTrack();
            m_track_renderer = null;
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions)
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                // Permission is not granted
                permissionsToRequest.add(permission);
        if (permissionsToRequest.size() > 0)
            ActivityCompat.requestPermissions(getActivity(), permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void onSelectTrackClick()
    {
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(m_btnSelectTrack.getContext(),  R.anim.pulse);
        m_btnSelectTrack.startAnimation(anim);
        Intent intent = new Intent(getActivity(), TracksListActivity.class);
        startActivityForResult(intent, SELECT_TRACK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            m_track_file = data.getStringExtra("result");
    }

    private void initTrackFromFile(String track_file)
    {
        clearMap();
        m_track_renderer = new TrackRenderer(getActivity(), m_map, null, new File(track_file), Color.GRAY);
        List<GeoPoint> points = m_track_renderer.polyline().getActualPoints();
        addStopMarker(points.get(points.size() - 1));
        BoundingBox boundingBox = m_track_renderer.polyline().getBounds();
        m_map.zoomToBoundingBox(boundingBox, true, 32);
    }

    private void addStopMarker(GeoPoint point)
    {
        m_finish_marker = new Marker(m_map);
        m_finish_marker.setPosition(point);

        m_finish_marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        Drawable d = getBitmapFromVectorDrawable(getActivity(), R.drawable.ic_race, 64, 64);
        m_finish_marker.setIcon(d);
        m_finish_marker.setId("finish_sign");
        m_finish_marker.setInfoWindow(null);
        m_map.getOverlays().add(m_finish_marker);
        animateMarker();
    }

    private void animateMarker()
    {
        final double[] angle = new double[1];
        angle[0] = 0.0;
        m_rotate_finish_marker = new Runnable()
        {
            @Override
            public void run()
            {
                if (m_finish_marker == null)
                    return;
                angle[0]++;
                m_finish_marker.setRotation((float)angle[0]);
                m_handler.postDelayed(this, 15);
                if (angle[0] >= 360.0)
                    angle[0] =-1;
                m_map.postInvalidate();
            }
        };
        m_handler.post(m_rotate_finish_marker);
    }
}
