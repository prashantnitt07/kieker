package kieker.tpan.logReader;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import kieker.common.logReader.AbstractKiekerMonitoringLogReader;
import java.util.Hashtable;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import kieker.common.logReader.LogReaderExecutionException;
import kieker.tpmon.monitoringRecord.AbstractKiekerMonitoringRecord;
import kieker.tpmon.writer.jmsAsync.MonitoringRecordTypeClassnameMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads tpmon messages from a (remote or local) JMS queue and processes them in tpan.
 *
 *
 * @author Andre van Hoorn, Matthias Rohr
 * History
 * 2009-07-01 (AvH) Initial version
 * 2009-07-25 (MR)
 */
public class JMSReader extends AbstractKiekerMonitoringLogReader {

    private static final Log log = LogFactory.getLog(JMSReader.class);
    private String jmsProviderUrl = null;
    private String jmsDestination = null;

    /**
     * @param jmsServerLocation = for instance "tcp://127.0.0.1:3035/"
     * @param jmsDestination = for instance "queue1"
     * @return
     */
    public JMSReader(String jmsProviderUrl, String jmsDestination) {
        this.jmsProviderUrl = jmsProviderUrl;
        this.jmsDestination = jmsDestination;
        if (jmsProviderUrl == null && jmsDestination == null) {
            String errormessage = "JMSReader has not sufficient parameters. jmsProviderUrl or jmsDestination is null";
            log.error(errormessage);
        }

        if (jmsProviderUrl.equals("") && jmsDestination.equals("")) {
            String errormessage = "JMSReader has not sufficient parameters. jmsProviderUrl or jmsDestination is empty";
            log.error(errormessage);
        }
    }

    /**
     * A call to this method is a blocking call.
     */
    public boolean execute() throws LogReaderExecutionException {


        boolean retVal = false;
        try {
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.exolab.jms.jndi.InitialContextFactory");
            //properties.put(Context.PROVIDER_URL, "tcp://pc-rohr.informatik.uni-oldenburg.de:3035/");

            // JMS initialization
            properties.put(Context.PROVIDER_URL, jmsProviderUrl);
            Context context = new InitialContext(properties);
            ConnectionFactory factory =
                    (ConnectionFactory) context.lookup("ConnectionFactory");
            Connection connection = factory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = (Destination) context.lookup(jmsDestination);
            log.info("\n\n***\nListening to destination:" + destination + " at " + jmsProviderUrl + " !\n***\n\n");
            MessageConsumer receiver = session.createConsumer(destination);
            receiver.setMessageListener(new MessageListener() {
                // the MessageListener will execute onMessage each time a message comes in
                public void onMessage(Message jmsMessage) {
                    if (jmsMessage instanceof TextMessage) {
                        TextMessage text = (TextMessage) jmsMessage;
                        log.info("Received text message: " + text);

                    } else {
                        ObjectMessage om = (ObjectMessage) jmsMessage;
                        //System.out.println("Received object message: " + om.toString());
                        try {
                            Serializable omo = om.getObject();
                            if (omo instanceof AbstractKiekerMonitoringRecord) {
                                AbstractKiekerMonitoringRecord rec =
                                        (AbstractKiekerMonitoringRecord) omo;
                                Class<? extends AbstractKiekerMonitoringRecord> clazz = fetchClassForRecordTypeId(rec.getRecordTypeId());
                                if (clazz == null) {
                                    // TODO: we could retry it in a couple of seconds
                                    log.error("Found no mapping for record type id " + rec.getRecordTypeId());
                                } else {
                                    //log.info("New monitoring record of type " + clazz.getName() + " (id:" + rec.getRecordTypeId() + ")");
                                    deliverRecordToConsumers(rec);
                                }
                            } else if (omo instanceof MonitoringRecordTypeClassnameMapping) {
                                MonitoringRecordTypeClassnameMapping m =
                                        (MonitoringRecordTypeClassnameMapping) omo;
                                registerRecordTypeIdMapping(m.typeId, m.classname);
                                log.info("Received mapping " + m.typeId + "/" + m.classname);
                            } else {
                                log.info("Unknown type of message " + om);
                            }
                        } catch (MessageFormatException em) {
                            log.fatal("MessageFormatException:", em);
                        } catch (JMSException ex) {
                            log.fatal("JMSException ", ex);
                        } catch (LogReaderExecutionException ex) {
                            log.error("LogReaderExecutionException", ex);
                        } catch (Exception ex) {
                            log.error("Exception", ex);
                        }
                    }
                }
            });

            // start the connection to enable message delivery
            connection.start();

            System.out.println("JMSTestListener1 is started and waits for incomming monitoring events!");
            final Object blockingObj = new Object();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    synchronized (blockingObj) {
                        blockingObj.notifyAll();
                    }
                }
            }));
            synchronized (blockingObj) {
                blockingObj.wait();
            }
            log.info("Woke up by shutdown hook");
        } catch (Exception ex) {
            System.out.println("" + JMSReader.class.getName() + " " + ex.getMessage());
            ex.printStackTrace();
            retVal = false;
        } finally {
            super.terminate();
        }
        return retVal;
    }
}
