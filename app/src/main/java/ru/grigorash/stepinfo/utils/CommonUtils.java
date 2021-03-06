package ru.grigorash.stepinfo.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;

import ru.grigorash.stepinfo.track.ITrackReader;
import ru.grigorash.stepinfo.ui.settings.SettingsViewModel;

public final class CommonUtils
{
    // Get Current Day in Milliseconds
    public static long getCurrentDate()
    {
        Calendar cal = Calendar.getInstance();
        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date  = cal.get(Calendar.DATE);
        cal.clear();
        cal.set(year, month, date);
        return cal.getTimeInMillis();
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void startForegroundService(final Context context, final Intent intent)
    {
        context.startForegroundService(intent);
    }

    public static Bitmap getBitmap(Drawable drawable)
    {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static boolean isGPSEnabled(Context context)
    {
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isGpsEnabled || isNetworkEnabled;
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2)
    {
        final double R = 6371.0; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000.0; // convert to meters

        distance = Math.pow(distance, 2);
        return Math.sqrt(distance);
    }

    public static void copy(File src, File dst) throws IOException
    {
        try (InputStream in = new FileInputStream(src))
        {
            try (OutputStream out = new FileOutputStream(dst))
            {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
            }
        }
    }

    public static void setMapCenter(final Activity parent, final MapView map, final SettingsViewModel settings)
    {
        IGeoPoint center = settings.getMapCenter();
        float scale = settings.getMapScale();
        if (center != null)
        {
            IMapController controller = map.getController();
            controller.setCenter(center);
            controller.setZoom(scale);
        }
        else
        {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(parent);
            if (ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(parent, location ->
                    {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null)
                        {
                            IMapController controller = map.getController();
                            controller.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()), (double)scale, (long)1000);
                        }
                    });
        }
    }

    public static Activity getActivity()
    {
        try
        {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values())
            {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord))
                {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return null;
    }

    public static Drawable getBitmapFromVectorDrawable(Context context, int drawableId, int width, int height)
    {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width - 1, height - 1);
        drawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
        //return bitmap;
    }
}
