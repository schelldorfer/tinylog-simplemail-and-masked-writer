package com.github.schelldorfer.tinylog.writers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.tinylog.core.LogEntry;
import org.tinylog.writers.AbstractFormatPatternWriter;
import org.tinylog.writers.raw.ByteArrayWriter;

/**
 * tinylog 2 Masked {@link org.tinylog.writers.FileWriter}<br/>
 * This is a <a href="https://tinylog.org/v2/extending/#custom-writer">custom writer</a> for
 * <a href="https://tinylog.org/v2/">tinylog 2</a> logging framework to mask parts of log message.<br/>
 * see {@link MaskedWriterUtil} for description, configuration and usage
 *
 * @author Martin Schelldorfer, 2022
 */
public final class MaskedFileWriter extends AbstractFormatPatternWriter
{
    private final Charset charset;
    private final ByteArrayWriter writer;

    private final MaskedWriterUtil maskedWriter;

    /**
     * @throws IOException
     *             Log file cannot be opened for write access
     * @throws IllegalArgumentException
     *             Log file is not defined in configuration
     */
    public MaskedFileWriter() throws IOException {
        this(Collections.<String, String>emptyMap());
    }

    /**
     * @param properties
     *            Configuration for writer
     *
     * @throws IOException
     *             Log file cannot be opened for write access
     * @throws IllegalArgumentException
     *             Log file is not defined in configuration
     */
    public MaskedFileWriter(final Map<String, String> properties) throws IOException {
        super(properties);

        String fileName = getFileName();
        boolean append = getBooleanValue("append");
        boolean buffered = getBooleanValue("buffered");
        boolean writingThread = getBooleanValue("writingthread");

        charset = getCharset();
        writer = createByteArrayWriter(fileName, append, buffered, !writingThread, false, charset);

        maskedWriter = new MaskedWriterUtil(properties);
    }

    @Override
    public void write(LogEntry logEntry) throws IOException {
        logEntry = maskedWriter.mask(logEntry);

        byte[] data = render(logEntry).getBytes(charset);
        writer.write(data, 0, data.length);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
