package com.github.schelldorfer.tinylog.writers;

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

public class SimpleMailWriter extends AbstractFormatPatternWriter
{
    private Mailer mailer;
    private EmailPopulatingBuilder emailBuilder;

    public SimpleMailWriter(Map<String, String> properties)
    {
        super(properties);

        Properties smp = new Properties();
        properties.forEach((key, value) -> {
            if (key.startsWith("simplejavamail."))
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
