package ru.grigorash.stepinfo.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

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

}
