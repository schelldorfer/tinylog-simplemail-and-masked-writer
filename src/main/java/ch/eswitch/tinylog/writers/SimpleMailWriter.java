package ch.eswitch.tinylog.writers;

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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    private static final String PROPERTY_SIMPLEMAIL = "simplejavamail.";

    /**
     * buffer all log entries which occur withing this interval and send one combined email<br/>
     * see {@link java.time.Duration#parse(CharSequence)} for supported values
     */
    private static final String PROPERTY_SEND_INTERVAL = "sendinterval";

    /**
     * buffered {@link LogEntry} which should be sent in next mail
     */
    private final ArrayList<LogEntry> bufferedLogEntries = new ArrayList<>();

    /**
     * timestamp to indicate when next mail can be sent
     */
    private Instant nextMessage = null;

    private Mailer mailer;
    private EmailPopulatingBuilder emailBuilder;

    /**
     * Duration of {@link SimpleMailWriter#PROPERTY_SEND_INTERVAL}
     */
    private Duration sendInterval = null;

    /**
     * {@link ScheduledExecutorService} to create schedule next mail after interval time
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * next scheduled executor to send next mail
     */
    private ScheduledFuture<?> scheduledExecutor;

    /**
     * cached thread pool to process {@link LogEntry}
     */
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    /**
     * @param properties Configuration for writer
     */
    public SimpleMailWriter(Map<String, String> properties)
    {
        super(properties);

        InternalLogger.log(Level.INFO, "SimpleMailWriter.<init>");

        Properties smp = new Properties();
        properties.forEach((key, value) -> {
            if (key.startsWith(PROPERTY_SIMPLEMAIL))
            {
                smp.put(key, value);

                if (key.equalsIgnoreCase(ConfigLoader.Property.SMTP_PASSWORD.key()) || key.equalsIgnoreCase(ConfigLoader.Property.PROXY_PASSWORD.key()) || key.equalsIgnoreCase(
                        ConfigLoader.Property.SMIME_SIGNING_KEYSTORE_PASSWORD.key()) || key.equalsIgnoreCase(ConfigLoader.Property.SMIME_SIGNING_KEY_PASSWORD.key()))
                {
                    value = "***";
                }

                InternalLogger.log(Level.TRACE, "property '" + key + "': " + value);
            }
            else if (key.equals(PROPERTY_SEND_INTERVAL))
            {
                try
                {
                    // remove comment
                    int posComment = value.indexOf('#');
                    if (posComment > -1)
                    {
                        value = value.substring(0, posComment);
                    }

                    // initialize send interval
                    sendInterval = Duration.parse(value.trim());

                    String msg = String.format("set '%s': %s (%d seconds)",
                                               PROPERTY_SEND_INTERVAL,
                                               sendInterval.toString(),
                                               sendInterval.getSeconds());
                    InternalLogger.log(Level.TRACE, msg);

                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
                catch (Exception e)
                {
                    InternalLogger.log(Level.ERROR, e);
                    throw e;
                }
            }
        });

        try
        {
            ConfigLoader.loadProperties(smp, false);
            mailer = MailerBuilder.buildMailer();

            mailer.testConnection();

            emailBuilder = EmailBuilder.startingBlank();
        }
        catch (MailException e)
        {
            InternalLogger.log(Level.ERROR, e);
            throw e;
        }

    }

    @Override
    public void write(final LogEntry logEntry)
    {
        InternalLogger.log(Level.TRACE, String.format("%s: write", Instant.now()));

        int size;
        synchronized (bufferedLogEntries)
        {
            bufferedLogEntries.add(0, logEntry);
            size = bufferedLogEntries.size();
        }

        if (size > 1)
        {
            InternalLogger.log(Level.TRACE, String.format("%s: processEntry: size of bufferedLogEntries: %d", Instant.now(), size));
        }

        cachedThreadPool.execute(this::processBufferedLogEntries);
    }

    /**
     * process {@link SimpleMailWriter#bufferedLogEntries} and check if mail should be sent or scheduled
     */
    private void processBufferedLogEntries()
    {
        if (sendInterval == null)
        {
            // always send immediately
            sendMail();
        }
        else
        {
            if (nextMessage == null)
            {
                // send first mail
                sendMail();
            }
            else
            {
                Instant now = Instant.now();
                if (scheduledExecutor == null && now.isAfter(nextMessage))
                {
                    // interval has passed since last mail was sent
                    InternalLogger.log(Level.TRACE, String.format("%s: now is after nextMessage, nextMessage: %s ", now, nextMessage));
                    sendMail();
                }
                else
                {
                    Duration delay = null;
                    synchronized (scheduledExecutorService)
                    {
                        if (scheduledExecutor == null)
                        {
                            delay = Duration.between(now, nextMessage);
                            scheduledExecutor = scheduledExecutorService.schedule(() -> sendMail(), delay.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    }

                    if (delay != null)
                    {
                        InternalLogger.log(Level.TRACE, String.format("%s: schedule to send next mail in %d s", Instant.now(), delay.toSeconds()));
                    }
                    else
                    {
                        InternalLogger.log(Level.TRACE,
                                           String.format("%s: scheduledExecutor is scheduled in %d s",
                                                         Instant.now(),
                                                         scheduledExecutor.getDelay(TimeUnit.SECONDS)));
                    }
                }
            }
        }
    }

    @Override
    public void flush()
    {
        InternalLogger.log(Level.TRACE, String.format("%s: flush", Instant.now()));
        sendMail();
    }

    @Override
    public void close() throws Exception
    {
        InternalLogger.log(Level.TRACE, String.format("%s: close", Instant.now()));
        cachedThreadPool.awaitTermination(30, TimeUnit.SECONDS);
        InternalLogger.log(Level.TRACE, String.format("%s: cachedThreadPool terminated", Instant.now()));
        flush();

        scheduledExecutorService.shutdown();
    }

    /**
     * send one mail with all {@link LogEntry} in {@link SimpleMailWriter#bufferedLogEntries}
     */
    private void sendMail()
    {
        InternalLogger.log(Level.DEBUG, String.format("%s: start sending mail", Instant.now()));

        if (sendInterval != null)
        {
            Instant now = Instant.now();
            nextMessage = now.plus(sendInterval);
            InternalLogger.log(Level.TRACE, String.format("%s: set nextMessage: %s", now, nextMessage.toString()));
            synchronized (scheduledExecutorService)
            {
                if (scheduledExecutor != null && !scheduledExecutor.isDone())
                {
                    scheduledExecutor.cancel(false);
                }

                scheduledExecutor = null;
            }
        }

        String msgText;
        synchronized (bufferedLogEntries)
        {
            InternalLogger.log(Level.TRACE, String.format("%s: sendMail: size of bufferedLogEntries: %d", Instant.now(), bufferedLogEntries.size()));

            if (bufferedLogEntries.size() == 0)
            {
                return;
            }

            // build mail message body text with all buffered LogEntries
            msgText = bufferedLogEntries.stream()
                    .map(e -> render(e))
                    .collect(Collectors.joining());

            bufferedLogEntries.clear();
        }

        try
        {
            emailBuilder.withPlainText(msgText);

            Email email = emailBuilder.buildEmail();

            try
            {
                mailer.validate(email);
            }
            catch (MailException e)
            {
                InternalLogger.log(Level.WARN, e);
            }

            mailer.sendMail(email);

            String id = email.getId();

            if (id != null && id.length() > 0)
            {
                InternalLogger.log(Level.DEBUG, String.format("%s: mail sent, id: %s", Instant.now(), id));
            }
            else
            {
                InternalLogger.log(Level.ERROR, String.format("%s: mail id is empty, mail not sent", Instant.now()));
            }
        }
        catch (MailException e)
        {
            InternalLogger.log(Level.ERROR, e);
            throw e;
        }
    }

}
