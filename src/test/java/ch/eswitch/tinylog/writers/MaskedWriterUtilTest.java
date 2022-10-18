package ch.eswitch.tinylog.writers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.runtime.PreciseTimestamp;
import java.util.HashMap;
import java.util.Map;

public class MaskedWriterUtilTest
{

    @Test
    void suffixPropertyTest()
    {
        Map<String, String> p = new HashMap<>();

        p.put(MaskedWriterUtil.PROPERTY_FILTER_PREFIX, "<ele>");
        p.put(MaskedWriterUtil.PROPERTY_FILTER_SUFFIX, "<ele/>");

        MaskedWriterUtil maskedWriterUtil = new MaskedWriterUtil(p);

        maskMessage(maskedWriterUtil, "abcdefbghijlkmnop");
        maskMessage(maskedWriterUtil, "abc");
        maskMessage(maskedWriterUtil, "");
        maskMessage(maskedWriterUtil, "<ele>");

        maskMessage(maskedWriterUtil, "abc<ele>123456");

        maskMessage(maskedWriterUtil, "abc<ele>123456<ele>xyz");
        maskMessage(maskedWriterUtil, "abc<ele/>123456<ele/>xyz");

        maskMessage(maskedWriterUtil, "<ele>123456<ele>xyz");
        maskMessage(maskedWriterUtil, "<ele/>123456<ele/>xyz");

        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>xyz", "abc<ele>******<ele/>xyz");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>", "abc<ele>******<ele/>");
        maskMessage(maskedWriterUtil, "<ele>123456<ele/>xyz", "<ele>******<ele/>xyz");
    }

    @Test
    void replaceCharacterPropertyTest()
    {
        Map<String, String> p = new HashMap<>();

        p.put(MaskedWriterUtil.PROPERTY_FILTER_PREFIX, "<ele>");
        p.put(MaskedWriterUtil.PROPERTY_FILTER_SUFFIX, "<ele/>");
        p.put(MaskedWriterUtil.PROPERTY_REPLACE_CHARACTER, "x");

        MaskedWriterUtil maskedWriterUtil = new MaskedWriterUtil(p);

        maskMessage(maskedWriterUtil, "<ele>123456<ele>xyz");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>xyz", "abc<ele>xxxxxx<ele/>xyz");
    }

    @Test
    void searchLengthPropertyTest()
    {
        Map<String, String> p = new HashMap<>();

        p.put(MaskedWriterUtil.PROPERTY_FILTER_PREFIX, "<ele>");
        p.put(MaskedWriterUtil.PROPERTY_FILTER_SUFFIX, "<ele/>");
        p.put(MaskedWriterUtil.PROPERTY_SEARCH_LENGTH, "20");

        MaskedWriterUtil maskedWriterUtil = new MaskedWriterUtil(p);

        maskMessage(maskedWriterUtil, "<ele>123456<ele>xyz");
        maskMessage(maskedWriterUtil, "ab<ele>123456<ele/>xyz", "ab<ele>******<ele/>xyz");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>", "abc<ele>******<ele/>");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/><ele>0<ele/>", "abc<ele>******<ele/><ele>0<ele/>");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>xyz<ele>0<ele/>", "abc<ele>******<ele/>xyz<ele>0<ele/>");
    }

    @Test
    void fixedLengthPropertyTest()
    {
        Map<String, String> p = new HashMap<>();

        p.put(MaskedWriterUtil.PROPERTY_FILTER_PREFIX, "<ele>");
        p.put(MaskedWriterUtil.PROPERTY_FILTER_FIXED_LENGTH, "3");

        MaskedWriterUtil maskedWriterUtil = new MaskedWriterUtil(p);

        maskMessage(maskedWriterUtil, "<ele>123456<ele>xyz", "<ele>***456<ele>***");
        maskMessage(maskedWriterUtil, "<ele>123456<ele>wxyz", "<ele>***456<ele>***z");
        maskMessage(maskedWriterUtil, "abc<ele>123456<ele/>xyz", "abc<ele>***456<ele/>xyz");
        maskMessage(maskedWriterUtil, "<ele>12", "<ele>**");
    }

    private static void maskMessage(MaskedWriterUtil maskedWriterUtil, String message)
    {
        maskMessage(maskedWriterUtil, message, message);
    }

    private static void maskMessage(MaskedWriterUtil maskedWriterUtil, String message, String expectedMessage)
    {
        LogEntry logEntry = maskedWriterUtil.mask(newLogEntry(message));
        Assertions.assertEquals(expectedMessage, logEntry.getMessage(), "original message: " + message);
    }

    private static LogEntry newLogEntry(String message)
    {
        return new LogEntry(new PreciseTimestamp(), Thread.currentThread(), null, MaskedWriterUtilTest.class.getName(), "suffixPropertyTest", null, 0, null, Level.TRACE, message, null);
    }
}
