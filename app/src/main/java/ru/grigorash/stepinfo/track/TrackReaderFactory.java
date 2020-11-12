package ru.grigorash.stepinfo.track;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class TrackReaderFactory
{
    public static ITrackReader getReader(File file) throws IOException
    {
        FileInputStream fs = new FileInputStream(file);
        DataInputStream ds = new DataInputStream(fs);
        byte[] signature = new byte[4];
        ds.read(signature, 0, signature.length);
        if (!Arrays.equals(signature, TrackRecorder.FILE_SIGNATURE))
            throw new IOException("invalid file format");
        byte version = ds.readByte();
        switch (version)
        {
            case 1:
                return new TrackReaderV1Impl(fs);
            default:
                throw new IOException(String.format("invalid file format version - %d", version));
        }
    }
}
