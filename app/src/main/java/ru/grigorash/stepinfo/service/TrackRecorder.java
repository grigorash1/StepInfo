package ru.grigorash.stepinfo.service;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TrackRecorder
{
    private static final String PREFIX = "track";
    private static final String EXT = "crd";

    private final Context m_context;
    private File m_track_file;
    private FileOutputStream m_out;

    public TrackRecorder(Context context)
    {
        m_context = context;
        m_track_file = createTempFile();
        if (m_track_file != null)
            m_out = getOutputSteram(m_track_file);
    }

    private FileOutputStream getOutputSteram(File file)
    {
        try
        {
            return new FileOutputStream(m_track_file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private File createTempFile()
    {
        File outputDir = m_context.getCacheDir();
        try
        {
            return File.createTempFile(PREFIX, "crd", outputDir);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
