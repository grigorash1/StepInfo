package ru.grigorash.stepinfo.track;

import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.balsikandar.crashreporter.CrashReporter;

import static ru.grigorash.stepinfo.utils.CommonUtils.*;

import java.io.File;
import java.io.IOException;

import static ru.grigorash.stepinfo.track.TrackReaderFactory.getReader;

public class TrackInfo
{
    private final String m_track_file;

    private Double m_total_length;
    private Double m_avg_speed;

    public TrackInfo(String file_uri)
    {
        m_track_file = file_uri;
    }

    public String name()
    {
        int pos = m_track_file.lastIndexOf("/");
        return m_track_file.substring(pos + 1, m_track_file.length() - 4);
    }
    public Double length()   { return m_total_length; }
    public Double avgSpeed() { return m_avg_speed; }
    public String fileUri() { return m_track_file; }

    public void asyncGetInfo(final Consumer<TrackInfo> on_complete)
    {
        if ((m_total_length != null) && (m_avg_speed != null))
            on_complete.accept(this);
        else
            new Thread(() -> evalTrackParams(on_complete)).start();
    }

    private void evalTrackParams(final Consumer<TrackInfo> on_complete)
    {
        try (ITrackReader reader = getReader(new File(m_track_file)))
        {
            int counter = 0;
            m_avg_speed = 0.0;
            m_total_length = 0.0;
            RecordPosition prev_pos = null;
            ITrackRecord record = null;
            while ((record = reader.read()) != null)
            {
                switch (record.Type())
                {
                    case TrackRecorder.RECORD_POSITION:
                        RecordPosition pos = (RecordPosition)record;
                        m_avg_speed += pos.speed;
                        if (counter > 0)
                            m_total_length += distance(prev_pos.lat, pos.lat, prev_pos.lon, pos.lon);
                        prev_pos = pos;
                        counter++;
                }
            }
            m_avg_speed /= counter;
            m_avg_speed *= 3.6;
            m_total_length /= 1000;

            on_complete.accept(this);
        }
        catch (Exception e)
        {
            CrashReporter.logException(e);
        }
    }
}
