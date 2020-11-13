package ru.grigorash.stepinfo.ui.tracksviewer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.TracksListActivity;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

public class TracksViewFragment  extends Fragment
{
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private final static int SELECT_TRACK_REQUEST_CODE = 222;

    private MapView              m_map;
    private SettingsViewModel    m_settings;
    private MyLocationNewOverlay m_myLocationOverlay;
    private FloatingActionButton m_btnSelectTrack;
    private TextView             m_text_info;

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
        return root;
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
        Intent intent = new Intent(getActivity(), TracksListActivity.class);
        startActivityForResult(intent, SELECT_TRACK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        getActivity();
        if (requestCode == SELECT_TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            //some code
        }
    }
}
