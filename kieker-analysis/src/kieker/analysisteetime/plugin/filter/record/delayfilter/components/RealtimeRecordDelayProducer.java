/***************************************************************************
 * Copyright 2015 Kieker Project (http://kieker-monitoring.net)
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

package kieker.analysisteetime.plugin.filter.record.delayfilter.components;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import kieker.common.record.IMonitoringRecord;

import teetime.framework.AbstractProducerStage;

/**
 * Gets records from a queue, calculates the delay for each records and forwards the records after the delay. As this stage extends {@link AbstractProducerStage} it
 * is always declared as active.
 *
 * @author Andre van Hoorn, Robert von Massow, Jan Waller, Lars Bluemke
 *
 * @since 1.13
 */
public class RealtimeRecordDelayProducer extends AbstractProducerStage<IMonitoringRecord> {

	public static final double ACCELERATION_FACTOR_DEFAULT = 1;

	private final LinkedBlockingQueue<Object> recordQueue;
	private final Object endToken;

	private final TimeUnit timeunit;
	private final TimerWithPrecision timer;
	private final double accelerationFactor;
	private final long warnOnNegativeSchedTime;

	private volatile long startTime = -1;
	private volatile long firstLoggingTimestamp;

	/**
	 * Creates a new instance of this class using the given parameters.
	 *
	 * @param recordQueue
	 *            Queue to pass records from {@link RealtimeRecordDelayConsumer} to {@link RealtimeRecordDelayProducer}.
	 * @param endToken
	 *            Simple Object to indicate that no more records are received and the stage can terminate.
	 * @param timeunit
	 *            The time unit to be used.
	 * @param accelerationFactor
	 *            Determines the replay speed.
	 * @param warnOnNegativeSchedTime
	 *            A time bound to configure a warning when a record is forwarded too late.
	 */
	public RealtimeRecordDelayProducer(final LinkedBlockingQueue<Object> recordQueue, final Object endToken, final TimeUnit timeunit,
			final double accelerationFactor, final long warnOnNegativeSchedTime) {

		this.recordQueue = recordQueue;
		this.endToken = endToken;
		this.timeunit = timeunit;

		TimerWithPrecision tmpTimer;
		try {
			tmpTimer = TimerWithPrecision.valueOf(this.timeunit.toString());
		} catch (final IllegalArgumentException ex) {
			this.logger.warn(this.timeunit.toString() + " is no valid timer precision! Using MILLISECONDS instead.");
			tmpTimer = TimerWithPrecision.MILLISECONDS;
		}
		this.timer = tmpTimer;

		if (accelerationFactor <= 0.0) {
			this.logger.warn("Acceleration factor must be > 0. Using default: " + ACCELERATION_FACTOR_DEFAULT);
			this.accelerationFactor = 1;
		} else {
			this.accelerationFactor = accelerationFactor;
		}

		this.warnOnNegativeSchedTime = warnOnNegativeSchedTime;
	}

	@Override
	protected void execute() {
		try {
			// System.out.println("RRDF producer waiting for records");
			final Object element = this.recordQueue.take();
			if (element instanceof IMonitoringRecord) {
				// System.out.println("RRDF producer took " + ((IMonitoringRecord) element).getLoggingTimestamp() + " from the queue");
			}

			if (element == this.endToken) {
				// System.out.println("RRDF producer terminating");
				this.terminateStage();
			} else if (element instanceof IMonitoringRecord) {
				final IMonitoringRecord monitoringRecord = (IMonitoringRecord) element;

				final long currentTime = this.timer.getCurrentTime(this.timeunit);

				if (this.startTime == -1) { // init on first record
					this.firstLoggingTimestamp = monitoringRecord.getLoggingTimestamp();
					this.startTime = currentTime;
				}

				// System.out.println("timestamp " + monitoringRecord.getLoggingTimestamp());
				// System.out.println("first timestamp " + this.firstLoggingTimestamp);
				// System.out.println("current time " + currentTime);
				// System.out.println("start time " + this.startTime);

				// Compute scheduling time (without acceleration)
				long schedTimeFromNow = (monitoringRecord.getLoggingTimestamp() - this.firstLoggingTimestamp) // relative to 1st record
						- (currentTime - this.startTime); // subtract elapsed time
				schedTimeFromNow /= this.accelerationFactor;
				if (schedTimeFromNow < -this.warnOnNegativeSchedTime) {
					final long schedTimeSeconds = TimeUnit.SECONDS.convert(schedTimeFromNow, this.timeunit);
					this.logger.warn("negative scheduling time: " + schedTimeFromNow + " (" + this.timeunit.toString() + ") / " + schedTimeSeconds
							+ " (seconds)-> scheduling with a delay of 0");
				}
				if (schedTimeFromNow < 0) {
					schedTimeFromNow = 0; // i.e., schedule immediately
				}

				// System.out.println("RRDF producer going to sleep for " + TimeUnit.MILLISECONDS.convert(schedTimeFromNow, this.timeunit));
				Thread.sleep(TimeUnit.MILLISECONDS.convert(schedTimeFromNow, this.timeunit));
				// System.out.println("RRDF producer woke up");
				this.outputPort.send(monitoringRecord);
				System.out.println("RRDF producer sent record " + monitoringRecord);

			}

		} catch (final InterruptedException e) {
			this.logger.warn("Interrupted while waiting for next record.");
		}

	}

	@Override
	public void onTerminating() throws Exception { // NOPMD
		System.out.println("RRDF producer terminating");
		super.onTerminating();
	}

	/**
	 * @author Jan Waller
	 */
	private static enum TimerWithPrecision {
		MILLISECONDS {
			@Override
			public long getCurrentTime(final TimeUnit timeunit) {
				return timeunit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			}

		},
		NANOSECONDS {
			@Override
			public long getCurrentTime(final TimeUnit timeunit) {
				return timeunit.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
			}
		};

		public abstract long getCurrentTime(TimeUnit timeunit);
	}
}
