package com.github.schelldorfer.tinylog.writers;

import org.tinylog.core.LogEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Parts of log message between {@value PROPERTY_FILTER_PREFIX} and {@value PROPERTY_FILTER_SUFFIX} are masked
 * (replaced) with "***". This allows to hide parts of a log message.<br/>
 * Define properties {@value PROPERTY_FILTER_PREFIX} and {@value PROPERTY_FILTER_SUFFIX} in tinylog configuration file.<br/>
 * Multiple filters can be configured by appending an extension to {@value PROPERTY_FILTER_PREFIX} and
 * {@value PROPERTY_FILTER_SUFFIX}, e.g. <code>{@value PROPERTY_FILTER_PREFIX}1</code> and
 * <code>{@value PROPERTY_FILTER_SUFFIX}1</code>.
 */
public class MaskedWriterUtil
{
    static final String PROPERTY_FILTER = "filter.";
    /**
     * Prefix property name
     */
    static final String PROPERTY_FILTER_PREFIX = PROPERTY_FILTER + "prefix";
    /**
     * Suffix property name
     */
    static final String PROPERTY_FILTER_SUFFIX = PROPERTY_FILTER + "suffix";

    /**
     * create filter list based on tinylog configuration properties
     *
     * @param properties configuration properties
     * @return filter list
     */
    static ArrayList<Filter> createFilter(Map<String, String> properties)
    {
        ArrayList<Filter> filters = new ArrayList<>();

        properties.forEach((keyPrefix, valuePrefix) -> {
            if (keyPrefix.startsWith(PROPERTY_FILTER_PREFIX) && valuePrefix != null && valuePrefix.length() > 0)
            {
                String keyExtension = keyPrefix.substring(PROPERTY_FILTER_PREFIX.length());
                String valueSuffix = properties.get(PROPERTY_FILTER_SUFFIX + keyExtension);
                if (valueSuffix != null && valueSuffix.length() > 0)
                    filters.add(new Filter(valuePrefix, valueSuffix));
            }
        });

        return filters;
    }

    /**
     * mask message of logEntry
     *
     * @param logEntry logEntry to be masked
     * @param filters filter list
     * @return logEntry with masked message
     */
    static LogEntry mask(LogEntry logEntry, ArrayList<Filter> filters)
    {
        String message = logEntry.getMessage();

        if (message == null || message.length() == 0 || filters.size() == 0)
            return logEntry;

        boolean masked = false;
        Iterator<Filter> itFilter = filters.iterator();
        while (itFilter.hasNext())
        {
            Filter f = itFilter.next();
            int posPrefix = message.indexOf(f.prefix);
            if (posPrefix >= 0)
            {
                int posSuffix = message.indexOf(f.suffix, posPrefix + f.prefix.length());
                if (posSuffix > 0)
                {
                    // filter matches, mask message
                    message = message.substring(0, posPrefix + f.prefix.length()) + "***" + message.substring(posSuffix);
                    masked = true;
                }
            }
        }

        if (masked)
        {
            // create new LogEntry with masked message
            logEntry = new LogEntry(logEntry.getTimestamp(), logEntry.getThread(), logEntry.getContext(), logEntry.getClassName(), logEntry.getMethodName(), logEntry.getFileName(), logEntry
                    .getLineNumber(), logEntry.getTag(), logEntry.getLevel(), message, logEntry.getException());
        }

        return logEntry;
    }
}
