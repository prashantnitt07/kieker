/***************************************************************************
 * Copyright 2014 Kicker Project (http://kicker-monitoring.net)
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
 ***************************************************************************/

package kicker.test.tools.junit.writeRead.filesystem;

import kicker.common.configuration.Configuration;
import kicker.monitoring.writer.IMonitoringWriter;
import kicker.monitoring.writer.filesystem.SyncFsWriter;

/**
 * 
 * @author Andre van Hoorn
 * 
 * @since 1.5
 */
public class BasicSyncFSWriterReaderTest extends AbstractTestFSWriterReader { // NOPMD (TestClassWithoutTestCases) // NOCS (MissingCtorCheck)

	private static final boolean FLUSH = true;

	@Override
	protected Class<? extends IMonitoringWriter> getTestedWriterClazz() {
		return SyncFsWriter.class;
	}

	@Override
	protected boolean terminateBeforeLogInspection() {
		return !BasicSyncFSWriterReaderTest.FLUSH;
	}

	@Override
	protected void refineWriterConfiguration(final Configuration config, final int numRecordsWritten) {
		config.setProperty(SyncFsWriter.CONFIG_FLUSH, Boolean.toString(BasicSyncFSWriterReaderTest.FLUSH));
	}

	@Override
	protected void doSomethingBeforeReading(final String[] monitoringLogs) {
		// we'll keep the log untouched
	}

	@Override
	protected void refineFSReaderConfiguration(final Configuration config) {
		// no need to refine
	}
}