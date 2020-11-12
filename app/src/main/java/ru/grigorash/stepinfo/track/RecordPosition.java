package ru.grigorash.stepinfo.track;

import java.io.DataInputStream;
import java.io.IOException;

public class RecordPosition implements ITrackRecord
{
    public final double alt;
    public final float  speed;
    public final double lat;
    public final double lon;
    public final long   time;

    public RecordPosition(DataInputStream input_stream, int format_cersion) throws IOException
    {
         alt = input_stream.readDouble();
         speed = input_stream.readFloat();
         lat = input_stream.readDouble();
         lon = input_stream.readDouble();
         time = input_stream.readLong();
    }

    @Override
    public int Type()
    {
        return TrackRecorder.RECORD_POSITION;
    }
}
