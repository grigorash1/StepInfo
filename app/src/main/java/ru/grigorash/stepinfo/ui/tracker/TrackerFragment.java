package ru.grigorash.stepinfo.ui.tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

public class TrackerFragment extends Fragment
{
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private MapView m_map;
    private SettingsViewModel m_settings;
    private MyLocationNewOverlay m_MyLocationOverlay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_settings = new SettingsViewModel(getActivity());

        View root = inflater.inflate(R.layout.fragment_tracker, container, false);
        Configuration.getInstance().load(getActivity().getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()));
        m_map = (MapView) root.findViewById(R.id.map);
        m_map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        m_map.setMultiTouchControls(true);
        //Add my location overlay
        m_MyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getActivity()), m_map);
        m_MyLocationOverlay.enableMyLocation();
        m_map.getOverlays().add(m_MyLocationOverlay);
        m_map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        IGeoPoint center = m_settings.getMapCenter();
        float scale = m_settings.getMapScale();
        if (false) //(center != null)
        {
            IMapController controller = m_map.getController();
            controller.setCenter(center);
            controller.setZoom(scale);
        }
        else
        {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), location ->
                    {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null)
                        {
                            IMapController controller = m_map.getController();
                            controller.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()), (double)scale, (long)1000);
                            //controller.setZoom(scale);
                        }
                    });
        }
        m_map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause()
    {
        m_settings.setMapCenter((GeoPoint) m_map.getMapCenter());
        m_settings.setMapScale((float)m_map.getZoomLevelDouble());
        super.onPause();
        m_map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions)
    {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions)
        {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
            {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0)
        {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
