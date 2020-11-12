package ru.grigorash.stepinfo.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TrackReaderV1Impl implements ITrackReader
{
    private InputStream     m_fs;
    private DataInputStream m_ds;

    public TrackReaderV1Impl(InputStream fs)
    {
        m_fs = fs;
        m_ds = new DataInputStream(fs);
    }

    @Override
    public ITrackRecord read() throws IOException
    {
        if (m_ds.available() <= 0)
            return null;
        byte record_type = m_ds.readByte();
        switch (record_type)
        {
            case TrackRecorder.RECORD_POSITION:
                return new RecordPosition(m_ds, 1);
            default:
                throw new IOException(String.format("Unsupported record type: %d", record_type));
        }
    }

    @Override
    public void close() throws Exception
    {
        if (m_fs != null)
        {
            m_fs.close();
            m_fs = null;
        }
    }
}
