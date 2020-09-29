package ru.grigorash.stepinfo.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class TrackRecorderSvc extends Service implements LocationListener
{
    // This is the object that receives interactions from clients.
    private final IBinder m_binder;

    @Override
    public void onLocationChanged(Location location)
    {

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

    public class LocalBinder extends Binder
    {
        TrackRecorderSvc getService()
        {
            return TrackRecorderSvc.this;
        }
    }

    public TrackRecorderSvc()
    {
        m_binder = new LocalBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return m_binder;
    }

    public void startRecord()
    {

    }
}
