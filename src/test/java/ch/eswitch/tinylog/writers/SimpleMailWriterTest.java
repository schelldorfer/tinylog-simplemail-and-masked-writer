package ch.eswitch.tinylog.writers;

import jakarta.mail.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.simplejavamail.config.ConfigLoader;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleMailWriterTest
{
    /**
     * define how long logging messages should be generated
     */
    private final static int TEST_DURATION_IN_SECONDS = 50;

    private final String uuid = UUID.randomUUID().toString();

    /**
     * list of all logged error message Ids
     */
    private final ArrayList<String> loggerIds = new ArrayList<>();

    private int errorCount = 0;

    @Test
    public void testMail()
    {
        sendMails();

        // wait some time until all mails have been sent and arrived in Inbox
        try
        {
            Thread.sleep(30000);
        }
        catch (InterruptedException e)
        {
        }

        receiveMails();
    }

    private void sendMails()
    {
        Logger.info("start logging, UUID: " + uuid);
        logErrors();
        logErrors();
        Logger.error(new IOException("test"));

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this::logErrors, 0, 5, TimeUnit.SECONDS);

        // sleep and let scheduledExecutorService run for some time
        Logger.info("sleep");
        try
        {
            Thread.sleep(TEST_DURATION_IN_SECONDS * 1000);
        }
        catch (InterruptedException e)
        {
        }

        Logger.info("shutdown");

        scheduledExecutorService.shutdown();

        Logger.info("end logging");
    }

    private void logErrors()
    {
        logError("A");
        logError("B");
        errorCount++;
    }

    private void logError(String suffix)
    {
        String loggerId = "error " + uuid + "#" + (errorCount + 1);

        if (suffix == null)
        {
            errorCount++;
        }
        else
        {
            loggerId = loggerId + "." + suffix;
        }

        loggerIds.add(loggerId);

        Logger.error(loggerId);
    }

    private void receiveMails()
    {
        Logger.info("start receiving mails, size of loggerIds: " + loggerIds.size());

        Folder folder = null;
        Store store = null;
        try
        {
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imap");
            props.setProperty("mail.imap.ssl.enable", "true");

            Session session = Session.getDefaultInstance(props, null);
            // session.setDebug(true);
            store = session.getStore("imap");

            String username = System.getenv(ConfigLoader.Property.SMTP_USERNAME.key().toUpperCase().replaceAll("\\.", "_"));
            String password = System.getenv(ConfigLoader.Property.SMTP_PASSWORD.key().toUpperCase().replaceAll("\\.", "_"));
            String host = System.getenv("SIMPLEJAVAMAIL_IMAP_HOST");

            Assertions.assertNotNull(username, "environment variable '" + ConfigLoader.Property.SMTP_USERNAME.key() + "' missing");
            Assertions.assertNotNull(password, "environment variable '" + ConfigLoader.Property.SMTP_PASSWORD.key() + "' missing");
            Assertions.assertNotNull(host, "environment variable 'simplejavamail.imap.host' missing");

            Logger.info("use " + host + ": " + username + " / " + password.replaceAll(".", "*"));

            store.connect(host, username, password);
            folder = store.getFolder("Inbox");

            folder.open(Folder.READ_WRITE);
            Message messages[] = folder.getMessages();
            Logger.info("No of Messages : " + folder.getMessageCount());
            Logger.info("No of Unread Messages : " + folder.getUnreadMessageCount());
            for (int i = 0; i < messages.length; ++i)
            {
                Message msg = messages[i];
                String from = "unknown";
                if (msg.getReplyTo().length >= 1)
                {
                    from = msg.getReplyTo()[0].toString();
                }
                else if (msg.getFrom().length >= 1)
                {
                    from = msg.getFrom()[0].toString();
                }
                String subject = msg.getSubject();

                Logger.info("MESSAGE #" + (i + 1) + ": " + subject + ", from " + from);

                if (!msg.isSet(Flags.Flag.SEEN) && subject.equals("SimpleMail Writer Test"))
                {
                    msg.setFlag(Flags.Flag.SEEN, true);

                    try
                    {
                        Object content = msg.getContent();
                        Logger.debug(content);
                        if (content instanceof String)
                        {
                            int pos;
                            int lastPos = Integer.MAX_VALUE;
                            do
                            {
                                String loggedId = loggerIds.get(0);
                                pos = ((String) content).indexOf(loggedId);
                                if (pos >= 0 && pos < lastPos)
                                {
                                    loggerIds.remove(loggedId);
                                    lastPos = pos;
                                    Logger.trace("'" + loggedId + "' found at position " + pos);
                                }
                            } while (pos >= 0 && loggerIds.size() > 0);
                        }
                    }
                    catch (IOException e)
                    {
                        Logger.error(e);
                        e.printStackTrace();
                    }
                }

                Logger.info("size of loggerIds: " + loggerIds.size());

                // delete the message
                // msg.setFlag(Flags.Flag.DELETED, true);

            }
        }
        catch (MessagingException e)
        {
            Logger.error(e);
            e.printStackTrace();
        }

        finally
        {
            if (folder != null)
            {
                try
                {
                    folder.close(true);
                }
                catch (MessagingException e)
                {
                    Logger.error(e);
                    e.printStackTrace();
                }
            }
            if (store != null)
            {
                try
                {
                    store.close();
                }
                catch (MessagingException e)
                {
                    Logger.error(e);
                    e.printStackTrace();
                }
            }
        }

        Assertions.assertEquals(0, loggerIds.size(), "not all loggerIds found in received emails");
    }
}
