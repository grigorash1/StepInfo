package ru.grigorash.stepinfo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import ru.grigorash.stepinfo.service.SensorListenerSvc;

public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Build.VERSION.SDK_INT >= 26)
            context.startForegroundService(new Intent(context, SensorListenerSvc.class));
        else
            context.startService(new Intent(context, SensorListenerSvc.class));
    }
}
