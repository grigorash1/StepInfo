package ru.grigorash.stepinfo.track;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.balsikandar.crashreporter.CrashReporter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import ru.grigorash.stepinfo.R;
import ru.grigorash.stepinfo.utils.CommonUtils;
import static ru.grigorash.stepinfo.utils.CommonUtils.distance;

public class TrackRecorder implements LocationListener
{
    public final static String TRACK_FILE_NAME = "current_track.bin";
    public final static String TRACKS_DIR = "tracks";
    public final static String ACTION_RECORDING_STATUS_CHANGED = "ru.StepInfo.RECORDING_STATUS_CHANGED";
    public final static String ACTION_ON_NEW_POSITION = "ru.StepInfo.ACTION_ON_NEW_POSITION";
    public final static String ACTION_ON_BAD_POSITION = "ru.StepInfo.ACTION_ON_BAD_POSITION";
    public final static String ACTION_ON_STOP = "ru.StepInfo.ACTION_ON_STOP";
    public final static String NEW_STATUS = "ru.StepInfo.NEW_STATUS";

    public final static byte[] FILE_SIGNATURE = {0x5, 0x1, 0xF, 0x0};
    public final static byte FORMAT_VERSION = 1;
    public final static byte RECORD_POSITION = 1;
    public final static byte RECORD_STOP = 2;
    public final static double MINIMUM_DISTANCE = 5;

    private final Context m_svc_context;

    private boolean  m_recording;
    private Location m_last_location;
    private double   m_total_distance;

    public TrackRecorder(Context context)
    {
        m_svc_context = context;
        LocationManager locationManager = (LocationManager)m_svc_context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(m_svc_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(m_svc_context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 3, this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (!filter(location))
            return;
        writeLocation(location);
        m_last_location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    public void Start()
    {
        m_total_distance = 0.0;
        m_recording = true;
        // continue or start?
        if (m_last_location == null)
            writeHeader();
        sendBroadcastMsg("recording");
    }

    public void Stop()
    {
        LocationManager locationManager = (LocationManager)m_svc_context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        m_recording = false;
        m_last_location = null;
        writeFooter();
        sendBroadcastMsg("stopped");
        showSaveTrackDlg();
    }

    public void Pause()
    {
        m_recording = false;
    }

    public boolean isRecording() { return m_recording; }

    public static File getCurrentTrackFile(final Context context)
    {
        File appFilesDirectory = context.getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
        return new File(appFilesDirectory, TRACK_FILE_NAME);
    }

    public static File getTracksDir(final Context context)
    {
        File appFilesDirectory = context.getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
        return new File(appFilesDirectory, TRACKS_DIR);
    }

    private void sendBroadcastMsg(String new_status)
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_RECORDING_STATUS_CHANGED);
        intent.putExtra(NEW_STATUS, new_status);
        m_svc_context.sendBroadcast(intent);
    }
    private void writeHeader()
    {
        File file = getCurrentTrackFile(m_svc_context);
        try
        {
            file.createNewFile();
            file.setWritable(true);
            file.setReadable(true);
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
        try (FileOutputStream fs = new FileOutputStream(file))
        {
            DataOutputStream ds = new DataOutputStream(fs);
            ds.write(FILE_SIGNATURE);
            ds.write(FORMAT_VERSION);
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    private void writeLocation(Location location)
    {
        try (FileOutputStream fs = new FileOutputStream(getCurrentTrackFile(m_svc_context), true))
        {
            DataOutputStream ds = new DataOutputStream(fs);
            ds.write(RECORD_POSITION);
            ds.writeDouble(location.getAltitude());
            ds.writeFloat(location.getSpeed());
            ds.writeDouble(location.getLatitude());
            ds.writeDouble(location.getLongitude());
            ds.writeLong(location.getTime());
            sendNewPositionEvent(location);
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    private void writeStop(Location location)
    {
        try (FileOutputStream fs = new FileOutputStream(getCurrentTrackFile(m_svc_context), true))
        {
            DataOutputStream ds = new DataOutputStream(fs);
            ds.write(RECORD_STOP);
            ds.writeDouble(location.getAltitude());
            ds.writeDouble(location.getLatitude());
            ds.writeDouble(location.getLongitude());
            ds.writeLong(location.getTime());
            sendStopEvent(location);
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    private void sendNewPositionEvent(Location location)
    {
        Intent intent = new Intent(ACTION_ON_NEW_POSITION);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lon", location.getLongitude());
        intent.putExtra("speed", location.getSpeed());
        intent.putExtra("alt", location.getAltitude());
        m_svc_context.sendBroadcast(intent);
    }

    private void sendStopEvent(Location location)
    {
        Intent intent = new Intent(ACTION_ON_STOP);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lon", location.getLongitude());
        m_svc_context.sendBroadcast(intent);
    }

    private void sendBadSignalEvent(Location location)
    {
        Intent intent = new Intent(ACTION_ON_BAD_POSITION);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lon", location.getLongitude());
        intent.putExtra("speed", location.getSpeed());
        intent.putExtra("accuracy", location.getAccuracy());
        m_svc_context.sendBroadcast(intent);
    }

    private void writeFooter()
    {

    }

    private void writeLog(String info)
    {
        try
        {
            File log_dir = m_svc_context.getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
            File log = new File(log_dir, "log.txt");
            if (!log.exists())
            {
                log.createNewFile();
                log.setWritable(true);
                log.setReadable(true);
            }
            try (FileOutputStream fs = new FileOutputStream(log, true))
            {
                DataOutputStream ds = new DataOutputStream(fs);
                ds.writeBytes(info + "\r\n");
            }
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }

    // check that user has walked less than 10 meters in 15 seconds
    private void checkStop(Location location)
    {
        if (m_last_location == null)
            return;
        long last_loc_time = m_last_location.getTime();
        double dist = distance(m_last_location.getLatitude(), location.getLatitude(),
                               m_last_location.getLongitude(), location.getLongitude());
        if ((dist <= MINIMUM_DISTANCE * 2) && ((location.getTime() - last_loc_time) > 15000))
        {
            Location loc = new Location(location);
            loc.setTime(last_loc_time);
            writeStop(loc);
        }
    }

    private boolean filter(Location location)
    {
        if (!m_recording)
            return false;

        if (location.getAccuracy() > (MINIMUM_DISTANCE * 3))
        {
            sendBadSignalEvent(location);
            return false;
        }

        if (m_last_location == null)
            return true;

        double distance = CommonUtils.distance(m_last_location.getLatitude(), location.getLatitude(), m_last_location.getLongitude(), location.getLongitude());
        if (distance > MINIMUM_DISTANCE)
        {
            m_total_distance += distance;
            checkStop(location);
            return true;
        }
        else
            return false;
    }

    private void showSaveTrackDlg()
    {
        Activity activity = CommonUtils.getActivity();
        if (activity == null)
            return;
        if (m_total_distance < 100.0)
            return;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        alertDialog.setTitle(R.string.save_track_title);
        alertDialog.setMessage(R.string.save_track_message);

        final EditText input = new EditText(m_svc_context);
        input.setText(android.text.format.DateFormat.format("yyyy-MM-dd", new java.util.Date()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton(R.string.yes,
                (dialog, which) ->
                {
                    String file_name = input.getText().toString();
                    if (!TextUtils.isEmpty(file_name))
                    {
                        File current_track = getCurrentTrackFile(m_svc_context);
                        File tracks_dir = getTracksDir(m_svc_context);
                        if (!tracks_dir.exists())
                            if (!tracks_dir.mkdir())
                                return;
                        File saved_track = new File(tracks_dir, file_name + ".bin");
                        try
                        {
                            CommonUtils.copy(current_track, saved_track);
                        }
                        catch (Exception e)
                        {
                            CrashReporter.logException(e);
                            AlertDialog.Builder errorDialog = new AlertDialog.Builder(m_svc_context);
                            alertDialog.setTitle("ERROR");
                            alertDialog.setMessage(e.getMessage());
                        }
                    }
                });

        alertDialog.setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }
}
