package ru.grigorash.stepinfo.track;

import java.io.IOException;

public interface ITrackReader extends AutoCloseable
{
    ITrackRecord read() throws IOException;
}
