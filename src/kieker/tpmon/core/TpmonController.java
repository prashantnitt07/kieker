package kieker.tpmon.core;

import kieker.common.record.AbstractMonitoringRecord;

import kieker.tpmon.writer.util.async.TpmonShutdownHook;
import kieker.tpmon.writer.util.async.AbstractWorkerThread;
import kieker.tpmon.writer.IMonitoringLogWriter;
import kieker.tpmon.writer.database.SyncDbConnector;
import kieker.tpmon.writer.filesystem.SyncFsWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import kieker.common.record.DummyMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.tpmon.writer.database.AsyncDbConnector;
import kieker.tpmon.writer.filesystem.AsyncFsConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2009 Kieker Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================================
 */

/**
 * Simple class to store monitoroing data in the file system. Although a
 * buffered writer is used, outliers (delays of 1000 ms) occur from time
 * to time if many monitoring events have to be writen. We believe that
 * outliers result from a flush on the buffer of the writer. 
 * 
 * A more sophisticated writer to store data in the file system is 
 * the AsyncFsWriter. This does not introduce the outliers that result
 * from flushing the writing buffer, since provides an asynchronous 
 * insertMonitoringData method. However, the AsyncFsWriter introduces
 * a little more overhead because a writing queue is required and it isn't
 * tested as much as the FileSystenWriter.
 * 
 * The AsyncFsWriter should usually be used instead of this class to avoid 
 * the outliers described above.
 * 
 * @author Matthias Rohr, Andre van Hoorn
 * 
 * History:
 * 2008/09/01: Removed a lot "synchronized" from the Aspects
 * 2008/08/29: Controller now singleton class
 *             Many (performance) improvements to synchronization
 * 2008/08/06: Using tpmon.properties instead of dbconnector.properties and support
 *             of using java.io.tmpdir as file system storage directory. The storage
 *             directory may be set via the properties file, or (higher priority)
 *             via a java command line parameter 
 *             (-Dtpmon.storeInJavaIoTmpdir=false -Dtpmon.customStoragePath=/var/log/)
 * 2008/07/07: New feature to encode method and component names
 *             before making data persistent. This speeds up storage
 *             and saves space.
 * 2008/05/29: Changed vmid to vmname (defaults to hostname), 
 *             which may be changed during runtime
 * 2008/01/04: Refactoring for the first release of 
 *             Kieker and publication under an open source licence
 * 2007/03/13: Refactoring
 * 2006/12/20: Initial Prototype
 */
public final class TpmonController {

    private static final Log log = LogFactory.getLog(TpmonController.class);
    public final static String WRITER_SYNCDB = "SyncDB";
    public final static String WRITER_ASYNCDB = "AsyncDB";
    public final static String WRITER_SYNCFS = "SyncFS";
    public final static String WRITER_ASYNCFS = "AsyncFS";
    private String monitoringDataWriterClassname = null;
    private String monitoringDataWriterInitString = null;
    private IMonitoringLogWriter monitoringDataWriter = null;
    private String vmname = "unknown";    // the following configuration values are overwritten by tpmonLTW.properties in tpmonLTW.jar
    private String dbDriverClassname = "com.mysql.jdbc.Driver";
    private String dbConnectionAddress = "jdbc:mysql://HOSTNAME/DATABASENAME?user=DBUSER&password=DBPASS";
    private String dbTableName = "turbomon10";
    private boolean debug = false;
    private String filenamePrefix = ""; // e.g. path "/tmp/"
    private boolean storeInJavaIoTmpdir = true;
    private String customStoragePath = "/tmp"; // only used as default if storeInJavaIoTmpdir == false
    private boolean logMonitoringRecordTypeIds = false; // eventually, true should become default
    private int asyncRecordQueueSize = 8000;
    // database only configuration configuration values that are overwritten by tpmon.properties included in the tpmon library
    private boolean setInitialExperimentIdBasedOnLastId = false;    // only use the asyncDbconnector in server environments, that do not directly terminate after the executions, or some 
    private TpmonShutdownHook shutdownhook = null;
    private static TpmonController ctrlInst = new TpmonController();

    //marks the end of monitoring to the writer threads
    public static final AbstractMonitoringRecord END_OF_MONITORING_MARKER = new DummyMonitoringRecord();

    
    public final static TpmonController getInstance() {
        return TpmonController.ctrlInst;
    }

    private TpmonController() {
        try {
            vmname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
        } // nothing to do -- vmname will be "unknown"

        log.info(">Kieker-Tpmon: The VM has the name " + vmname + " Thread:" +
                Thread.currentThread().getId());
        log.info(">Kieker-Tpmon: Virtual Machine start time " +
                ManagementFactory.getRuntimeMXBean().getStartTime());

        shutdownhook = new TpmonShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownhook);

        loadPropertiesFile();

        /* We will now determine and load the monitoring Writer to use */
        try {
            if (this.monitoringDataWriterClassname == null || this.monitoringDataWriterClassname.length() == 0) {
                throw new Exception("Property monitoringDataWriter not set");
            } else if (this.monitoringDataWriterClassname.equals(WRITER_SYNCFS)) {
                String filenameBase = filenamePrefix;
                this.monitoringDataWriter = new SyncFsWriter(filenameBase);
            } else if (this.monitoringDataWriterClassname.equals(WRITER_ASYNCFS)) {
                String filenameBase = filenamePrefix;
                this.monitoringDataWriter = new AsyncFsConnector(filenameBase, asyncRecordQueueSize);
            } else if (this.monitoringDataWriterClassname.equals(WRITER_SYNCDB)) {
                this.monitoringDataWriter = new SyncDbConnector(
                        dbDriverClassname, dbConnectionAddress,
                        dbTableName,
                        setInitialExperimentIdBasedOnLastId);
            } else if (this.monitoringDataWriterClassname.equals(WRITER_ASYNCDB)) {
                this.monitoringDataWriter = new AsyncDbConnector(
                        dbDriverClassname, dbConnectionAddress,
                        dbTableName,
                        setInitialExperimentIdBasedOnLastId, asyncRecordQueueSize);
            } else {
                /* try to load the class by name */
                this.monitoringDataWriter = (IMonitoringLogWriter) Class.forName(this.monitoringDataWriterClassname).newInstance();
                //add asyncRecordQueueSize
                monitoringDataWriterInitString += " | asyncRecordQueueSize="+asyncRecordQueueSize;
                if (!this.monitoringDataWriter.init(monitoringDataWriterInitString)) {
                    this.monitoringDataWriter = null;
                    throw new Exception("Initialization of writer failed!");
                }

            }
            Vector<AbstractWorkerThread> worker = this.monitoringDataWriter.getWorkers(); // may be null
            if (worker != null) {
                for (AbstractWorkerThread w : worker) {
                    this.registerWorker(w);
                }
            }
            // TODO: we should add a getter to all writers like isInitialized.
            //       right now, the following even appears in case init failed.
            //       Or can we simply throw an exception from within the constructors
            log.info(">Kieker-Tpmon: Initialization completed.\n Connector Info: " + this.getConnectorInfo());
        } catch (Exception exc) {
            log.error(">Kieker-Tpmon: Disabling monitoring", exc);
            this.terminateMonitoring();
        }
    }

    /**
     * The vmname which defaults to the hostname, and may be set by tpmon-control-servlet.
     * The vmname will be part of the monitoring data and allows to assing observations
     * in cases where the software system is deployed on more than one host.
     * 
     * When you want to distinguish multiple Virtual Machines on one host,
     * you have to set the vmname manually (e.g., via the tpmon-control-servlet, 
     * or by directly implementing a call to TpmonController.setVmname(...).
     */
    
    public final String getVmname() {
        return this.vmname;
    }

    /**
     * Allows to set an own vmname, a field in the monitoring data to distinguish
     * multiple hosts / vms in a system. This method is for instance used by
     * the tpmon control servlet. 
     * 
     * The vmname defaults to the hostname.
     * 
     * When you want to distinguish multiple Virtual Machines on one host,
     * you have to set the vmname manually (e.g., via the tpmon-control-servlet, 
     * or by directly implementing a call to TpmonController.setVmname(...).
     * 
     * @param newVmname
     */
    
    public final void setVmname(String newVmname) {
        log.info(">Kieker-Tpmon: The VM has the NEW name " + newVmname +
                " Thread:" + Thread.currentThread().getId());
        this.vmname = newVmname;
    }

    /**
     * See TpmonShutdownHook.registerWorker
     * @param newWorker
     */
    
    private void registerWorker(AbstractWorkerThread newWorker) {
        this.shutdownhook.registerWorker(newWorker);
    }
    private AtomicLong numberOfInserts = new AtomicLong(0);
    // private Date startDate = new Date(initializationTime);
    // TODO: should be volatile? -> more overhead, but correct!
    private boolean monitoringEnabled = true;
    // if monitoring terminated, it is not allowed to enable monitoring afterwards
    private boolean monitoringPermanentlyTerminated = false;

    
    public final boolean isDebug() {
        return debug;
    }

    /**
     * Shows how many inserts have been performed since last restart of the execution
     * environment.
     */
    
    public long getNumberOfInserts() {
        return numberOfInserts.longValue();
    }

    
    public final boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    
    public final boolean isMonitoringPermanentlyTerminated() {
        return monitoringPermanentlyTerminated;
    }
    private static final int STANDARDEXPERIMENTID = 0;
    // we do not use AtomicInteger since we only rarely 
    // set the value (common case -- getting -- faster now).
    // instead, we decided to provide an "expensive" increment method.
    private int experimentId = STANDARDEXPERIMENTID;

    
    public final int getExperimentId() {
        return this.experimentId;
    }

    
    public synchronized int incExperimentId() {
        return this.experimentId++;
    }

    
    public void setExperimentId(int newExperimentID) {
        this.experimentId = newExperimentID;
    }

    /**
     * Enables monitoring.
     */
    
    public final void enableMonitoring() {
        log.info("Enabling monitoring");
        if (this.monitoringPermanentlyTerminated) {
            log.error("Refused to enable monitoring because monitoring has been permanently terminated before");
        } else {
            this.monitoringEnabled = true;
        }
    }

    /**
     * Disables to store monitoring data.
     * Monitoring may be enabled again by calling enableMonitoring().
     */
    
    public final void disableMonitoring() {
        log.info("Disabling monitoring");
        this.monitoringEnabled = false;
    }

    /**
     * Permanently terminates monitoring (e.g., due to a failure).
     * Subsequent tries to enable monitoring will be refused.
     */
    
    public final synchronized void terminateMonitoring() {
        log.info("Permanently terminating monitoring");
        if (this.monitoringDataWriter != null) {
            /* if the initialization of the writer failed, it is set to null*/
            this.monitoringDataWriter.writeMonitoringRecord(END_OF_MONITORING_MARKER);
        }
        this.disableMonitoring();
        this.monitoringPermanentlyTerminated = true;
    }
    /**
     * If true, the loggingTimestamp is not set by the logMonitoringRecord
     * method. This is required to replay recorded traces with the
     * original timestamps.
     */
    private boolean replayMode = false;

    public final void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode;
    }

    
    public final boolean logMonitoringRecord(IMonitoringRecord monitoringRecord) {
        if (!this.monitoringEnabled) {
            return false;
        }

        numberOfInserts.incrementAndGet();
        // now it fails fast, it disables monitoring when a queue once is full
        if (!this.replayMode) {
            monitoringRecord.setLoggingTimestamp(this.getTime());
        }
        if (!this.monitoringDataWriter.writeMonitoringRecord(monitoringRecord)) {
            log.fatal("Error writing the monitoring data. Will terminate monitoring!");
            this.terminateMonitoring();
            return false;
        }

        return true;
    }
    private static final long offsetA = System.currentTimeMillis() * 1000000 - System.nanoTime();

    /**
     * This method can used by the probes to get the time stamps. It uses nano seconds as precision.
     *
     * In contrast to System.nanoTime(), it gives the nano seconds between the current time and midnight, January 1, 1970 UTC.
     * (The value returned by System.nanoTime() only represents nanoseconds since *some* fixed but arbitrary time.)
     */
    
    public final long getTime() {
        return System.nanoTime() + offsetA;
    }

    /**    
     * Loads configuration values from the file
     * tpmonLTW.jar/META-INF/dbconnector.properties or another
     * tpmon configuration file specified by the JVM parameter
     * tpmon.configuration.
     *
     * If it fails, it uses hard-coded standard values.    
     */
    
    private void loadPropertiesFile() {
        String configurationFile = "META-INF/tpmon.properties";
        InputStream is = null;
        Properties prop = new Properties();

        try {
            if (System.getProperty("tpmon.configuration") != null) { // we use the present virtual machine parameter value
                configurationFile = System.getProperty("tpmon.configuration");
                log.info("Tpmon: Loading properties JVM-specified path '" + configurationFile + "'");
                is = new FileInputStream(configurationFile);
            } else {
                log.info("Tpmon: Loading properties from tpmon library jar/" + configurationFile);
                log.info("You can specify an alternative properties file using the property 'tpmon.configuration'");
                is = TpmonController.class.getClassLoader().getResourceAsStream(configurationFile);
            }
            // TODO: the fall-back file in the tpmon library should be renamed to
            //       META-INF/tpmon.properties.default or alike, in order to
            //       avoid strange behavior caused by the order of jars being
            //       being loaded by the classloader.
            prop.load(is);
        } catch (Exception ex) {
            log.error("Error loading tpmon.properties file '" + configurationFile + "'", ex);
            // TODO: introduce static variable 'terminated' or alike
        } finally {
            try {
                is.close();
            } catch (Exception ex) { /* nothing we can do */ }
        }

        // load property monitoringDataWriter
        monitoringDataWriterClassname = prop.getProperty("monitoringDataWriter");
        monitoringDataWriterInitString = prop.getProperty("monitoringDataWriterInitString");

        String dbDriverClassnameProperty;
        if (System.getProperty("tpmon.dbConnectionAddress") != null) { // we use the present virtual machine parameter value
            dbDriverClassnameProperty = System.getProperty("tpmon.dbDriverClassname");
        } else { // we use the parameter in the properties file
            dbDriverClassnameProperty = prop.getProperty("dbDriverClassname");
        }
        if (dbDriverClassnameProperty != null && dbDriverClassnameProperty.length() != 0) {
            dbDriverClassname = dbDriverClassnameProperty;
        } else {
            log.info("No dbDriverClassname parameter found in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + dbDriverClassname + ".");
        }

        // load property "dbConnectionAddress"
        String dbConnectionAddressProperty;
        if (System.getProperty("tpmon.dbConnectionAddress") != null) { // we use the present virtual machine parameter value
            dbConnectionAddressProperty = System.getProperty("tpmon.dbConnectionAddress");
        } else { // we use the parameter in the properties file
            dbConnectionAddressProperty = prop.getProperty("dbConnectionAddress");
        }
        if (dbConnectionAddressProperty != null && dbConnectionAddressProperty.length() != 0) {
            dbConnectionAddress = dbConnectionAddressProperty;
        } else {
            log.warn("No dbConnectionAddress parameter found in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + dbConnectionAddress + ".");
        }

// the filenamePrefix (folder where tpmon stores its data) 
// for monitoring data depends on the properties tpmon.storeInJavaIoTmpdir 
// and tpmon.customStoragePath         
// these both parameters may be provided (with higher priority) as java command line parameters as well (example in the properties file)
        String storeInJavaIoTmpdirProperty;
        if (System.getProperty("tpmon.storeInJavaIoTmpdir") != null) { // we use the present virtual machine parameter value
            storeInJavaIoTmpdirProperty = System.getProperty("tpmon.storeInJavaIoTmpdir");
        } else { // we use the parameter in the properties file
            storeInJavaIoTmpdirProperty = prop.getProperty("tpmon.storeInJavaIoTmpdir");
        }

        if (storeInJavaIoTmpdirProperty != null && storeInJavaIoTmpdirProperty.length() != 0) {
            if (storeInJavaIoTmpdirProperty.toLowerCase().equals("true") || storeInJavaIoTmpdirProperty.toLowerCase().equals("false")) {
                storeInJavaIoTmpdir = storeInJavaIoTmpdirProperty.toLowerCase().equals("true");
            } else {
                log.warn("Bad value for tpmon.storeInJavaIoTmpdir (or provided via command line) parameter (" + storeInJavaIoTmpdirProperty + ") in tpmonLTW.jar/" + configurationFile +
                        ". Using default value " + storeInJavaIoTmpdir);
            }
        } else {
            log.warn("No tpmon.storeInJavaIoTmpdir parameter found in tpmonLTW.jar/" + configurationFile +
                    " (or provided via command line). Using default value '" + storeInJavaIoTmpdir + "'.");
        }

        if (storeInJavaIoTmpdir) {
            filenamePrefix = System.getProperty("java.io.tmpdir");
        } else { // only now we consider tpmon.customStoragePath
            String customStoragePathProperty;
            if (System.getProperty("tpmon.customStoragePath") != null) { // we use the present virtual machine parameter value
                customStoragePathProperty = System.getProperty("tpmon.customStoragePath");
            } else { // we use the parameter in the properties file
                customStoragePathProperty = prop.getProperty("tpmon.customStoragePath");
            }

            if (customStoragePathProperty != null && customStoragePathProperty.length() != 0) {
                filenamePrefix = customStoragePathProperty;
            } else {
                log.warn("No tpmon.customStoragePath parameter found in tpmonLTW.jar/" + configurationFile +
                        " (or provided via command line). Using default value '" + customStoragePath + "'.");
                filenamePrefix =
                        customStoragePath;
            }
        }

        // load property "dbTableNameProperty"
        String dbTableNameProperty;
        if (System.getProperty("tpmon.dbTableName") != null) { // we use the present virtual machine parameter value
            dbTableNameProperty = System.getProperty("tpmon.dbTableName");
        } else { // we use the parameter in the properties file
            dbTableNameProperty = prop.getProperty("dbTableName");
        }
        if (dbTableNameProperty != null && dbTableNameProperty.length() != 0) {
            dbTableName = dbTableNameProperty;
        } else {
            log.warn("No dbTableName  parameter found in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + dbTableName + ".");
        }

        // load property "debug"
        String debugProperty = prop.getProperty("debug");
        if (debugProperty != null && debugProperty.length() != 0) {
            if (debugProperty.toLowerCase().equals("true") || debugProperty.toLowerCase().equals("false")) {
                debug = debugProperty.toLowerCase().equals("true");
            } else {
                log.warn("Bad value for debug parameter (" + debugProperty + ") in tpmonLTW.jar/" + configurationFile +
                        ". Using default value " + debug);
            }
        } else {
            log.warn("Could not find debug parameter in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + debug);
        }

        // load property "setInitialExperimentIdBasedOnLastId"
        String setInitialExperimentIdBasedOnLastIdProperty = prop.getProperty("setInitialExperimentIdBasedOnLastId");
        if (setInitialExperimentIdBasedOnLastIdProperty != null && setInitialExperimentIdBasedOnLastIdProperty.length() != 0) {
            if (setInitialExperimentIdBasedOnLastIdProperty.toLowerCase().equals("true") || setInitialExperimentIdBasedOnLastIdProperty.toLowerCase().equals("false")) {
                setInitialExperimentIdBasedOnLastId = setInitialExperimentIdBasedOnLastIdProperty.toLowerCase().equals("true");
            } else {
                log.warn("Bad value for setInitialExperimentIdBasedOnLastId parameter (" + setInitialExperimentIdBasedOnLastIdProperty + ") in tpmonLTW.jar/" + configurationFile +
                        ". Using default value " + setInitialExperimentIdBasedOnLastId);
            }
        } else {
            log.warn("Could not find setInitialExperimentIdBasedOnLastId parameter in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + setInitialExperimentIdBasedOnLastId);
        }

        // load property "asyncRecordQueueSize"
        String asyncRecordQueueSizeProperty = null;
        if (System.getProperty("tpmon.asyncRecordQueueSize") != null) { // we use the present virtual machine parameter value
            asyncRecordQueueSizeProperty = System.getProperty("tpmon.asyncRecordQueueSize");
        } else { // we use the parameter in the properties file
            asyncRecordQueueSizeProperty = prop.getProperty("asyncRecordQueueSize");
        }
        if (asyncRecordQueueSizeProperty != null && asyncRecordQueueSizeProperty.length() != 0) {
            int asyncRecordQueueSizeValue = -1;
            try {
                asyncRecordQueueSizeValue = Integer.parseInt(asyncRecordQueueSizeProperty);
            } catch (NumberFormatException ex) {
            }
            if (asyncRecordQueueSizeValue >= 0) {
                asyncRecordQueueSize = asyncRecordQueueSizeValue;
            } else {
                log.warn("Bad value for asyncRecordQueueSize parameter (" + asyncRecordQueueSizeProperty + ") in tpmonLTW.jar/" + configurationFile +
                        ". Using default value " + asyncRecordQueueSize);
            }
        } else {
            log.warn("Could not find asyncRecordQueueSize parameter in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + asyncRecordQueueSize);
        }

        String monitoringEnabledProperty = prop.getProperty("monitoringEnabled");
        if (monitoringEnabledProperty != null && monitoringEnabledProperty.length() != 0) {
            if (monitoringEnabledProperty.toLowerCase().equals("true") || monitoringEnabledProperty.toLowerCase().equals("false")) {
                monitoringEnabled = monitoringEnabledProperty.toLowerCase().equals("true");
            } else {
                log.warn("Bad value for monitoringEnabled parameter (" + monitoringEnabledProperty + ") in tpmonLTW.jar/" + configurationFile +
                        ". Using default value " + monitoringEnabled);
            }

        } else {
            log.warn("Could not find monitoringEnabled parameter in tpmonLTW.jar/" + configurationFile +
                    ". Using default value " + monitoringEnabled);
        }

        if (monitoringEnabled == false) {
            log.info(">Kieker-Tpmon: Notice, monitoring is deactived (monitoringEnables=false in dbconnector.properties within tpmonLTW.jar)");
        }

        if (debug) {
            log.info(getConnectorInfo());
        }
    }

    
    public String getConnectorInfo() {
        StringBuilder strB = new StringBuilder();

        strB.append("monitoringDataWriter : " + this.monitoringDataWriter.getClass().getCanonicalName());
        strB.append(",");
        strB.append(" monitoringDataWriter config : (below), " + this.monitoringDataWriter.getInfoString());
        strB.append(",");
        strB.append(" version :" + this.getVersion() + ", debug :" + debug + ", enabled :" + isMonitoringEnabled() + ", terminated :" + isMonitoringPermanentlyTerminated() + ", experimentID :" + getExperimentId() + ", vmname :" + getVmname());

        return strB.toString();
    }

    
    public String getDateString() {
        return java.util.Calendar.getInstance().getTime().toString();
    }

    
    public String getVersion() {
        return TpmonVersion.getVERSION();
    }

    
    public final void setDebug(boolean debug) {
        this.debug = debug;
    }
}