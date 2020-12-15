package ru.grigorash.stepinfo.ui.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.TracksListActivity;
import ru.grigorash.stepinfo.service.SensorListenerSvc;
import ru.grigorash.stepinfo.track.RecordPosition;
import ru.grigorash.stepinfo.track.RivalWatcher;
import ru.grigorash.stepinfo.track.TrackRecorder;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

import static android.content.Context.BIND_AUTO_CREATE;
import static ru.grigorash.stepinfo.utils.CommonUtils.setMapCenter;

public class TrackerFragment extends Fragment
{
    public final static String ACTION_ON_SELECT_RIVAL_TRACK = "ru.StepInfo.ACTION_ON_SELECT_RIVAL_TRACK";

    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private final static int SELECT_TRACK_REQUEST_CODE = 223;
    private final static String RIVAL_MARKER_ID = "RIVAL_MARKER";
    private MapView              m_map;
    private SettingsViewModel    m_settings;
    private MyLocationNewOverlay m_myLocationOverlay;
    private FloatingActionButton m_btnRecordTrack;
    private FloatingActionButton m_btnGotoMyPosition;
    private FloatingActionButton m_btnSelectRival;
    private ServiceConnection    m_connection;
    private SensorListenerSvc    m_service;
    private BroadcastReceiver    m_broadcastReceiver;
    private TrackRenderer        m_current_track_renderer;
    private TrackRenderer        m_rival_track_renderer;
    private TextView             m_text_info;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_settings = new SettingsViewModel(getActivity());

        View root = inflater.inflate(R.layout.fragment_tracker, container, false);
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
        m_btnRecordTrack = root.findViewById(R.id.btnRecordTrack);
        m_btnRecordTrack.setOnClickListener(v -> onRecordTrackClick());
        m_btnGotoMyPosition = root.findViewById(R.id.btnGotoMyLocation);
        m_btnGotoMyPosition.setOnClickListener(v -> onGotoMyPositionClick());
        m_btnSelectRival = root.findViewById(R.id.btnSelectRival);
        m_btnSelectRival.setOnClickListener(v -> onSelectRivalClick());
        m_btnSelectRival.setOnLongClickListener(v -> onLongRivalClick());
        m_text_info = root.findViewById(R.id.txtInfo);

        requestPermissionsIfNecessary(new String[]{
                // current location
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        return root;
    }

    private void initServiceConnection()
    {
        m_connection = new ServiceConnection()
        {
            @Override
            public void onServiceDisconnected(ComponentName name)
            {
                m_service = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                SensorListenerSvc.LocalBinder localBinder = (SensorListenerSvc.LocalBinder)service;
                m_service = localBinder.getServerInstance();
                setRecordingTrackButtonImage();
                initMyTrackRenderer();
            }
        };
        Intent intent = new Intent(getActivity(), SensorListenerSvc.class);
        getActivity().bindService(intent, m_connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setMapCenter(getActivity(), m_map, m_settings);
        m_map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        initServiceConnection();
        initBroadcastReceiver();
        //setRecordingTrackButtonImage();
        //initMyTrackRenderer();
    }

    @Override
    public void onPause()
    {
        m_settings.setMapCenter((GeoPoint)m_map.getMapCenter());
        m_settings.setMapScale((float)m_map.getZoomLevelDouble());
        super.onPause();
        m_map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (m_service != null)
        {
            getActivity().unbindService(m_connection);
            m_service = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++)
            permissionsToRequest.add(permissions[i]);
        if (permissionsToRequest.size() > 0)
            ActivityCompat.requestPermissions(getActivity(), permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
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

    private void initBroadcastReceiver()
    {
        m_broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (TrackRecorder.ACTION_RECORDING_STATUS_CHANGED.equals(intent.getAction()))
                {
                    m_text_info.setText("");
                    setRecordingTrackButtonImage();
                    initMyTrackRenderer();
                }
                else if (TrackRecorder.ACTION_ON_NEW_POSITION.equals(intent.getAction()))
                {
                    double speed = intent.getFloatExtra("speed", -100.0f);
                    m_text_info.setText(String.format("speed: %.2f", speed * 3.6));
                    Animation anim = android.view.animation.AnimationUtils.loadAnimation(m_btnRecordTrack.getContext(),  R.anim.shake);
                    m_btnRecordTrack.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_stop_record));
                    m_btnRecordTrack.startAnimation(anim);
                }
                else if (TrackRecorder.ACTION_ON_STOP.equals(intent.getAction()))
                {
                    GeoPoint startPoint = new GeoPoint(intent.getDoubleExtra("lat", 0), intent.getDoubleExtra("lon", 0));
                    TrackRenderer.addStopMarker(getActivity(), m_map, startPoint, 0);
                }
                else if (TrackRecorder.ACTION_ON_BAD_POSITION.equals(intent.getAction()))
                {
                    float speed = intent.getFloatExtra("speed", -100.0f);
                    float accuracy = intent.getFloatExtra("accuracy", -100.0f);
                    m_text_info.setText(String.format("speed: %.2f acc: %.2f ", speed  * 3.6, accuracy));
                    m_btnRecordTrack.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_no_network));
                }
                else if (RivalWatcher.ACTION_RIVAL_MOVE.equals(intent.getAction()))
                {
                    Marker rival = null;
                    for (Overlay o : m_map.getOverlays())
                    {
                        if (o instanceof Marker)
                        {
                            Marker m = (Marker)o;
                            if (m.getId().equals(RIVAL_MARKER_ID))
                            {
                                rival = m;
                                break;
                            }
                        }
                    }
                    if (rival == null)
                    {
                        rival = new Marker(m_map);
                        rival.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_rival));
                        rival.setId(RIVAL_MARKER_ID);
                        m_map.getOverlays().add(rival);
                    }
                    rival.setPosition(new GeoPoint(intent.getDoubleExtra("lat", 0), intent.getDoubleExtra("lon", 0)));
                    rival.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    m_map.invalidate();
                }
            }
        };
        IntentFilter filter = new IntentFilter(TrackRecorder.ACTION_RECORDING_STATUS_CHANGED);
        filter.addAction(TrackRecorder.ACTION_ON_NEW_POSITION);
        filter.addAction(TrackRecorder.ACTION_ON_BAD_POSITION);
        filter.addAction(TrackRecorder.ACTION_ON_STOP);
        filter.addAction(RivalWatcher.ACTION_RIVAL_MOVE);
        getActivity().registerReceiver(m_broadcastReceiver, filter);
    }

    private void removeRivalMarker()
    {
        for (Overlay o : m_map.getOverlays())
        {
            if (o instanceof Marker)
            {
                Marker m = (Marker)o;
                if (m.getId().equals(RIVAL_MARKER_ID))
                {
                    m.remove(m_map);
                    break;
                }
            }
        }
    }

    private void onGotoMyPositionClick()
    {
        GeoPoint my_position = m_myLocationOverlay.getMyLocation();
        if (my_position == null)
        {
            Animation anim = android.view.animation.AnimationUtils.loadAnimation(m_btnGotoMyPosition.getContext(),  R.anim.shake);
            m_btnGotoMyPosition.startAnimation(anim);
            return;
        }
        double current_zoom = m_map.getZoomLevelDouble();
        if (current_zoom < 17.0)
            current_zoom = 17.0;
        m_map.getController().animateTo(my_position, current_zoom, 1000L);
    }

    private void onRecordTrackClick()
    {
        if (m_service == null)
            return;
        Intent intent = new Intent(m_service.isRecordingTrack() ? SensorListenerSvc.ACTION_TRACK_STOP_REC : SensorListenerSvc.ACTION_TRACK_START_REC);
        getActivity().sendBroadcast(intent);
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(m_btnRecordTrack.getContext(),  R.anim.pulse);
        m_btnRecordTrack.startAnimation(anim);
    }

    private boolean onLongRivalClick()
    {
        if ((m_service == null) || (m_rival_track_renderer == null))
            return false;
        AlertDialog alert = new AlertDialog.Builder(getActivity()).setTitle(R.string.remove_rival_track_caption)
                .setMessage(R.string.remove_rival_track_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                {
                    Intent intent = new Intent(ACTION_ON_SELECT_RIVAL_TRACK);
                    intent.putExtra("track_file", (String)null);
                    getActivity().sendBroadcast(intent);
                    dialog.dismiss();
                    if (m_rival_track_renderer != null)
                        m_rival_track_renderer.removeTrack();
                    removeRivalMarker();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        alert.show();
        return true;
    }

    private void onSelectRivalClick()
    {
        if (m_service == null)
            return;
        Animation anim = android.view.animation.AnimationUtils.loadAnimation(m_btnSelectRival.getContext(), R.anim.pulse);
        m_btnSelectRival.startAnimation(anim);
        Intent intent = new Intent(getActivity(), TracksListActivity.class);
        startActivityForResult(intent, SELECT_TRACK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            Intent intent = new Intent(ACTION_ON_SELECT_RIVAL_TRACK);
            intent.putExtra("track_file", data.getStringExtra("result"));
            getActivity().sendBroadcast(intent);
        }
        else if (requestCode == SELECT_TRACK_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED)
        {
            Intent intent = new Intent(ACTION_ON_SELECT_RIVAL_TRACK);
            intent.putExtra("track_file", (String)null);
            getActivity().sendBroadcast(intent);
            removeRivalMarker();
        }
    }

    private void initRivalTrack()
    {
        if (m_rival_track_renderer != null)
            m_rival_track_renderer.removeTrack();

        RivalWatcher rival_watcher = m_service.getRivalWatcher();
        if (rival_watcher == null)
            return;
        List<RecordPosition> rival_track = rival_watcher.getTrackData();
        m_rival_track_renderer = new TrackRenderer(getActivity(), m_map, rival_track, Color.GRAY);
    }

    private void setRecordingTrackButtonImage()
    {
        if (!isAdded())
            return;
        if ((m_btnRecordTrack == null) || (m_service == null))
            return;
        int res_id = (m_service.isRecordingTrack()) ? R.drawable.ic_stop_record : R.drawable.ic_start_record;
        m_btnRecordTrack.setImageDrawable(ContextCompat.getDrawable(getActivity(), res_id));
    }

    private void initMyTrackRenderer()
    {
        if ((m_service == null) || !isAdded())
            return;
        if (m_current_track_renderer != null)
            m_current_track_renderer.removeTrack();
        if (m_service.isRecordingTrack())
            m_current_track_renderer = new TrackRenderer(getActivity(), m_map, TrackRecorder.ACTION_ON_NEW_POSITION, TrackRecorder.getCurrentTrackFile(getActivity()), Color.GREEN);
        initRivalTrack();
    }
}
