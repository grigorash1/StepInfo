package ru.grigorash.stepinfo.track;

import java.io.DataInputStream;
import java.io.IOException;

public class RecordStop implements ITrackRecord
{
    public final double alt;
    public final double lat;
    public final double lon;
    public final long   time;

    public RecordStop (DataInputStream input_stream, int format_cersion) throws IOException
    {
        alt = input_stream.readDouble();
        lat = input_stream.readDouble();
        lon = input_stream.readDouble();
        time = input_stream.readLong();
    }

    @Override
    public int Type()
    {
        return TrackRecorder.RECORD_STOP;
    }
}
