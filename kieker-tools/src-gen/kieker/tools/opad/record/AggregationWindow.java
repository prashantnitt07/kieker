/***************************************************************************
 * Copyright 2017 Kieker Project (http://kieker-monitoring.net)
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
package kieker.tools.opad.record;

import java.nio.BufferOverflowException;

import kieker.common.record.AbstractMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.io.IValueDeserializer;
import kieker.common.record.io.IValueSerializer;
import kieker.common.util.registry.IRegistry;


/**
 * @author Thomas Duellmann
 * API compatibility: Kieker 1.13.0
 * 
 * @since 1.10
 */
public class AggregationWindow extends AbstractMonitoringRecord implements IMonitoringRecord.Factory, IMonitoringRecord.BinaryFactory {
	private static final long serialVersionUID = -6015104562956593414L;

	/** Descriptive definition of the serialization size of the record. */
	public static final int SIZE = TYPE_SIZE_LONG // AggregationWindow.windowStart
			 + TYPE_SIZE_LONG // AggregationWindow.windowEnd
	;
	
	public static final Class<?>[] TYPES = {
		long.class, // AggregationWindow.windowStart
		long.class, // AggregationWindow.windowEnd
	};
	
	
	
	/** property name array. */
	private static final String[] PROPERTY_NAMES = {
		"windowStart",
		"windowEnd",
	};
	
	/** property declarations. */
	private final long windowStart;
	private final long windowEnd;
	
	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param windowStart
	 *            windowStart
	 * @param windowEnd
	 *            windowEnd
	 */
	public AggregationWindow(final long windowStart, final long windowEnd) {
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
	}

	/**
	 * This constructor converts the given array into a record.
	 * It is recommended to use the array which is the result of a call to {@link #toArray()}.
	 * 
	 * @param values
	 *            The values for the record.
	 *
	 * @deprecated since 1.13. Use {@link #AggregationWindow(IValueDeserializer)} instead.
	 */
	@Deprecated
	public AggregationWindow(final Object[] values) { // NOPMD (direct store of values)
		AbstractMonitoringRecord.checkArray(values, TYPES);
		this.windowStart = (Long) values[0];
		this.windowEnd = (Long) values[1];
	}

	/**
	 * This constructor uses the given array to initialize the fields of this record.
	 * 
	 * @param values
	 *            The values for the record.
	 * @param valueTypes
	 *            The types of the elements in the first array.
	 *
	 * @deprecated since 1.13. Use {@link #AggregationWindow(IValueDeserializer)} instead.
	 */
	@Deprecated
	protected AggregationWindow(final Object[] values, final Class<?>[] valueTypes) { // NOPMD (values stored directly)
		AbstractMonitoringRecord.checkArray(values, valueTypes);
		this.windowStart = (Long) values[0];
		this.windowEnd = (Long) values[1];
	}

	
	/**
	 * @param deserializer
	 *            The deserializer to use
	 */
	public AggregationWindow(final IValueDeserializer deserializer) {
		this.windowStart = deserializer.getLong();
		this.windowEnd = deserializer.getLong();
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @deprecated since 1.13. Use {@link #serialize(IValueSerializer)} with an array serializer instead.
	 */
	@Override
	@Deprecated
	public Object[] toArray() {
		return new Object[] {
			this.getWindowStart(),
			this.getWindowEnd()
		};
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerStrings(final IRegistry<String> stringRegistry) {	// NOPMD (generated code)
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(final IValueSerializer serializer) throws BufferOverflowException {
		//super.serialize(serializer);
		serializer.putLong(this.getWindowStart());
		serializer.putLong(this.getWindowEnd());
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] getValueTypes() {
		return TYPES; // NOPMD
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getValueNames() {
		return PROPERTY_NAMES; // NOPMD
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() {
		return SIZE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated This record uses the {@link kieker.common.record.IMonitoringRecord.Factory} mechanism. Hence, this method is not implemented.
	 */
	@Override
	@Deprecated
	public void initFromArray(final Object[] values) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj.getClass() != this.getClass()) return false;
		
		final AggregationWindow castedRecord = (AggregationWindow) obj;
		if (this.getLoggingTimestamp() != castedRecord.getLoggingTimestamp()) return false;
		if (this.getWindowStart() != castedRecord.getWindowStart()) return false;
		if (this.getWindowEnd() != castedRecord.getWindowEnd()) return false;
		return true;
	}
	
	public final long getWindowStart() {
		return this.windowStart;
	}
	
	
	public final long getWindowEnd() {
		return this.windowEnd;
	}
	
}
