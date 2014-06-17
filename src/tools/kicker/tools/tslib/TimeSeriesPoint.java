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

package kicker.tools.tslib;

/**
 * @author Andre van Hoorn
 * 
 * @since 1.9
 * 
 * @param <T>
 *            The type of the point.
 */
public class TimeSeriesPoint<T> implements ITimeSeriesPoint<T> {

	private final long time;
	private final T value;

	public TimeSeriesPoint(final long time, final T value) {
		this.time = time;
		this.value = value;
	}

	@Override
	public long getTime() {
		return this.time;
	}

	@Override
	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "[" + this.getTime() + "=" + this.getValue() + "]";
	}

}