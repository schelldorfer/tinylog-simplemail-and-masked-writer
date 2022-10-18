package ch.eswitch.tinylog.writers;

import java.util.Map;
import java.util.Properties;

import org.simplejavamail.MailException;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.config.ConfigLoader;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.provider.InternalLogger;
import org.tinylog.writers.AbstractFormatPatternWriter;

/**
 * tinylog 2 Email Writer based on Simple Java Mail<br/>
 * This is a <a href="https://tinylog.org/v2/extending/#custom-writer">custom writer</a> for
 * <a href="https://tinylog.org/v2/">tinylog 2</a> logging framework to send emails. Emails are sent with
 * <a href="https://www.simplejavamail.org/">Simple Java Mail</a> which is based on Jakarta Mail 2 library.
 * 
 * @author Martin Schelldorfer, 2022
 */
public class SimpleMailWriter extends AbstractFormatPatternWriter
{
    /**
     * writer property prefix for all Simple Java Mail properties<br/>
     * see <a href=
     * "https://www.simplejavamail.org/configuration.html#section-available-properties">https://www.simplejavamail.org/configuration.html#section-available-properties</a>
     * for all available properties
     */
    private final String PROPERTY_SIMPLEMAIL = "simplejavamail.";

    private Mailer mailer;
    private EmailPopulatingBuilder emailBuilder;

    /**
     *
     * @param properties Configuration for writer
     */
    public SimpleMailWriter(Map<String, String> properties)
    {
        super(properties);

        Properties smp = new Properties();
        properties.forEach((key, value) -> {
            if (key.startsWith(PROPERTY_SIMPLEMAIL))
                smp.put(key, value);
        });

        try
        {
            ConfigLoader.loadProperties(smp, false);
            mailer = MailerBuilder.buildMailer();

            mailer.testConnection();

            emailBuilder = EmailBuilder.startingBlank();
        } catch (MailException e)
        {
            InternalLogger.log(Level.ERROR, e);
            throw e;
        }

    }

    @Override
    public void write(LogEntry logEntry) throws Exception
    {
        try
        {
            emailBuilder.withPlainText(render(logEntry));

            Email email = emailBuilder.buildEmail();

            mailer.validate(email);

            mailer.sendMail(email);

        } catch (MailException e)
        {
            InternalLogger.log(Level.ERROR, e);
            throw e;
        }
    }

    @Override
    public void flush() throws Exception
    {

    }

    @Override
    public void close() throws Exception
    {
    }
}
