package ch.eswitch.tinylog.writers;

import org.tinylog.core.LogEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class with all functionality for Masked Writers<br/>
 * Parts of log message between {@value PROPERTY_FILTER_PREFIX} and {@value PROPERTY_FILTER_SUFFIX} are masked
 * (replaced) with {@link #replaceCharacter}. This allows to hide parts of a log message.<br/>
 * Define properties {@value PROPERTY_FILTER_PREFIX} and {@value PROPERTY_FILTER_SUFFIX} in tinylog configuration
 * file.<br/>
 * Instead of {@value PROPERTY_FILTER_SUFFIX} property {@value PROPERTY_FILTER_FIXED_LENGTH} can be set, to mask a fix
 * length of characters.<br/>
 * Multiple filters can be configured by appending an extension to {@value PROPERTY_FILTER_PREFIX} and
 * {@value PROPERTY_FILTER_SUFFIX}, e.g. <code>{@value PROPERTY_FILTER_PREFIX}1</code> and
 * <code>{@value PROPERTY_FILTER_SUFFIX}1</code>.<br/>
 * {@link #replaceCharacter} can be configured by {@value PROPERTY_REPLACE_CHARACTER} property in tinylog configuration
 * file.<br/>
 * If {@value PROPERTY_REPLACE_CHARACTER} property is not set, {@value DEFAULT_REPLACE_CHARACTER} is used.
 */
class MaskedWriterUtil
{
    private static final String PROPERTY_FILTER = "filter.";
    /**
     * Name of Prefix property in tinylog configuration file
     */
    static final String PROPERTY_FILTER_PREFIX = PROPERTY_FILTER + "prefix";
    /**
     * Name of Suffix property in tinylog configuration file
     */
    static final String PROPERTY_FILTER_SUFFIX = PROPERTY_FILTER + "suffix";

    /**
     * Name of property in tinylog configuration file to define a fixed length property for masking the log message<br/>
     * Instead of {@value PROPERTY_FILTER_SUFFIX} this property can be set, to mask (replace) a fix length of
     * characters.<br/>
     * The number of characters specified by this property are masked (replaced) after {@value PROPERTY_FILTER_PREFIX}
     */
    static final String PROPERTY_FILTER_FIXED_LENGTH = PROPERTY_FILTER + "fixedlength";

    /**
     * Name of property in tinylog configuration file to limit the number of characters to search in log message<br/>
     * If this property is set, only the first number of characters defined by this property are used to search for
     * {@value PROPERTY_FILTER_PREFIX} and {@value PROPERTY_FILTER_SUFFIX} in log message.
     */
    static final String PROPERTY_SEARCH_LENGTH = "searchlength";

    /**
     * name of Replace Character property in tinylog configuration file
     */
    static final String PROPERTY_REPLACE_CHARACTER = "replacecharacter";

    /**
     * default replace character
     */
    private static final char DEFAULT_REPLACE_CHARACTER = '*';

    /**
     * number of characters used from log message to search for {@value PROPERTY_FILTER_PREFIX} and
     * {@value PROPERTY_FILTER_SUFFIX}
     */
    private final int searchLength;

    /**
     * replace character which will be used for masking the log message
     */
    private final char replaceCharacter;

    /**
     * list with all filters
     */
    private final ArrayList<MaskingFilter> filters;

    /**
     *
     * @param properties tinylog configuration properties
     */
    public MaskedWriterUtil(Map<String, String> properties)
    {
        // initialize filter list
        filters = createFilter(properties);

        // initialize replaceCharacter field
        String propertyReplaceCharacter = properties.get(PROPERTY_REPLACE_CHARACTER);
        if (propertyReplaceCharacter != null && propertyReplaceCharacter.length() > 0)
            replaceCharacter = propertyReplaceCharacter.charAt(0);
        else
            replaceCharacter = DEFAULT_REPLACE_CHARACTER;

        // initialize searchLength field
        int searchLengthParsed = -1;
        String propertySearchLength = properties.get(PROPERTY_SEARCH_LENGTH);
        if (propertySearchLength != null && propertySearchLength.length() > 0)
        {
            try
            {
                searchLengthParsed = Integer.parseInt(propertySearchLength.trim());
            } catch (NumberFormatException e)
            {
            }
        }
        searchLength = searchLengthParsed;

    }

    /**
     * create filter list based on tinylog configuration properties
     *
     * @param properties configuration properties
     * @return filter list
     */
    private static ArrayList<MaskingFilter> createFilter(Map<String, String> properties)
    {
        ArrayList<MaskingFilter> filters = new ArrayList<>();

        properties.forEach((keyPrefix, valuePrefix) -> {
            if (keyPrefix.startsWith(PROPERTY_FILTER_PREFIX) && valuePrefix != null && valuePrefix.length() > 0)
            {
                String keyExtension = keyPrefix.substring(PROPERTY_FILTER_PREFIX.length());
                String valueSuffix = properties.get(PROPERTY_FILTER_SUFFIX + keyExtension);
                String valueFixedLength = properties.get(PROPERTY_FILTER_FIXED_LENGTH + keyExtension);
                if ((valueSuffix != null && valueSuffix.length() > 0) || (valueFixedLength != null && valueFixedLength.length() > 0))
                    filters.add(new MaskingFilter(valuePrefix, valueSuffix, valueFixedLength));
            }
        });

        return filters;
    }

    /**
     * mask message of logEntry
     *
     * @param logEntry logEntry to be masked
     * @return logEntry with masked message
     */
    public LogEntry mask(LogEntry logEntry)
    {
        String message = logEntry.getMessage();

        if (message == null || message.length() == 0 || filters.size() == 0)
            return logEntry;

        boolean masked = false;

        // limit log message length
        if(searchLength > 0)
            message = message.substring(0, Math.min(searchLength, message.length()));

        StringBuffer sbMessage = new StringBuffer(message);
        Iterator<MaskingFilter> itFilter = filters.iterator();
        while (itFilter.hasNext())
        {
            MaskingFilter f = itFilter.next();

            int posPrefix = -1;
            while ((posPrefix = sbMessage.indexOf(f.prefix, posPrefix+1)) >= 0)
            {
                if (f.suffix != null)
                {
                    int posSuffix = sbMessage.indexOf(f.suffix, posPrefix + f.prefix.length());
                    if (posSuffix > 0)
                    {
                        // filter matches, mask message
                        for (int i = posPrefix + f.prefix.length(); i < posSuffix; i++)
                            sbMessage.setCharAt(i, replaceCharacter);

                        masked = true;
                    }
                }
                else if (f.fixedLength > 0)
                {
                    final int start = posPrefix + f.prefix.length();
                    for (int i = start; i < Math.min(start + f.fixedLength, sbMessage.length()); i++)
                        sbMessage.setCharAt(i, replaceCharacter);

                    masked = true;
                }
            }
        }

        if (masked)
        {
            if(searchLength > 0 && message.length() < logEntry.getMessage().length())
                sbMessage.append(logEntry.getMessage().substring(searchLength));

            // create new LogEntry with masked message
            logEntry = new LogEntry(logEntry.getTimestamp(), logEntry.getThread(), logEntry.getContext(), logEntry.getClassName(), logEntry.getMethodName(), logEntry.getFileName(), logEntry
                    .getLineNumber(), logEntry.getTag(), logEntry.getLevel(), sbMessage.toString(), logEntry.getException());
        }

        return logEntry;
    }
}
