package ru.grigorash.stepinfo.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.grigorash.stepinfo.utils.CommonUtils.getCurrentDate;

public class StatisticDatabase extends SQLiteOpenHelper
{
    private final static String DB_NAME = "steps_statistic";
    private final static int DB_VERSION = 1;

    private final static String DATE_COLUMN = "date";
    private final static String STEPS_COLUMN = "steps";

    private static StatisticDatabase s_instance;

    private StatisticDatabase(final Context context)
    {
        super(context, /*Environment.getExternalStorageDirectory*/context.getExternalFilesDir("database") + File.separator + DB_NAME, null, DB_VERSION);
    }

    public static synchronized StatisticDatabase getInstance(final Context c)
    {
        if (s_instance == null)
        {
            s_instance = new StatisticDatabase(c.getApplicationContext());
            s_instance.getWritableDatabase(); // force to call onCreate
        }
        return s_instance;
    }

    @Override
    public void close()
    {
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //db.execSQL("CREATE TABLE statistic(_id INTEGER PRIMARY KEY AUTOINCREMENT, " + DATE_COLUMN + " INTEGER, "+ STEPS_COLUMN + " INTEGER);");
        db.execSQL("CREATE TABLE "+ DB_NAME + "(" + DATE_COLUMN + " INTEGER PRIMARY KEY, " + STEPS_COLUMN + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {
        // nothing to do
    }

    public int updateSteps(long date, int steps)
    {
        getWritableDatabase().beginTransaction();
        try
        {
            ContentValues values = new ContentValues();
            values.put(STEPS_COLUMN, steps);
            int num_rows = getWritableDatabase().update(DB_NAME, values, "date = ?", new String[]{String.valueOf(date)});
            if (num_rows == 0)
            {
                values.put(DATE_COLUMN, date);
                getWritableDatabase().insert(DB_NAME, null, values);
            }
            getWritableDatabase().setTransactionSuccessful();
        }
        finally
        {
            getWritableDatabase().endTransaction();
        }
        return steps;
    }

    public int updateTodaySteps(int steps)
    {
        return updateSteps(getCurrentDate(), steps);
    }

    public int getTodaySteps()
    {
        long date = getCurrentDate();
        Cursor c = getReadableDatabase().query(DB_NAME, new String[]{STEPS_COLUMN}, DATE_COLUMN + " = ?", new String[]{String.valueOf(date)}, null, null, null);
        if (c.getCount() == 0)
            return 0;
        else
        {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    // Gets the last num entries in descending order of date (newest first)
    public List<DayStatistic> getLastEntries(int num)
    {
        Cursor c = getReadableDatabase().query(DB_NAME, new String[]{DATE_COLUMN, STEPS_COLUMN}, DATE_COLUMN + " > 0", null, null, null, DATE_COLUMN + " DESC", String.valueOf(num));
        int max = c.getCount();
        List<DayStatistic> result = new ArrayList<>(max);
        if (c.moveToFirst())
            do
                result.add(new DayStatistic(c.getInt(1), c.getLong(0)));
            while (c.moveToNext());
        return result;
    }

}
