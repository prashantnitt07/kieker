/***************************************************************************
 * Copyright 2014 Kieker Project (http://kieker-monitoring.net)
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

package kieker.common.util;

import java.nio.ByteBuffer;

import kieker.common.record.IMonitoringRecord;
import kieker.common.record.misc.RegistryRecord;
import kieker.common.util.registry.IRegistry;
import kieker.monitoring.writer.tcp.NewTcpWriter;

/**
 * @author Christian Wulf
 *
 * @since 1.11
 */
public final class RecordSerializer {

	private final IRegistry<String> stringRegistry;

	public RecordSerializer(final IRegistry<String> stringRegistry) {
		super();
		this.stringRegistry = stringRegistry;
	}

	public final void serialize(final IMonitoringRecord record, final ByteBuffer buffer) {
		record.registerStrings(this.stringRegistry);

		final int recordClassId;
		if (record instanceof RegistryRecord) {
			recordClassId = NewTcpWriter.REGISTRY_RECORD_CLASS_ID;
		} else {
			// recordClassId = monitoringController.getUniqueIdForString(record.getClass().getName());
			recordClassId = this.stringRegistry.get(record.getClass().getName());
			// getUniqueIdForString delegates to stringRegistry.get(String)
		}

		buffer.putInt(recordClassId);
		buffer.putLong(record.getLoggingTimestamp());
		record.writeBytes(buffer, this.stringRegistry);
		// System.out.println("SERIALIZED: " + record.getClass().getName());
	}
}
