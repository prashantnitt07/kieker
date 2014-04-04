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

package kieker.panalysis;

import kieker.panalysis.base.AbstractFilter;

/**
 * @author Jan Waller, Nils Christian Ehmke
 * 
 * @since 1.10
 */
public class CountingFilter extends AbstractFilter<CountingFilter.INPUT_PORT, CountingFilter.OUTPUT_PORT> {

	public static enum INPUT_PORT { // NOCS
		INPUT_OBJECT, CURRENT_COUNT
	}

	public static enum OUTPUT_PORT { // NOCS
		RELAYED_OBJECT, CURRENT_COUNT
	}

	public CountingFilter() {
		super(INPUT_PORT.class, OUTPUT_PORT.class);
	}

	public void execute() {
		final Object inputObject = super.take(INPUT_PORT.INPUT_OBJECT);
		final long count = (Long) super.take(INPUT_PORT.CURRENT_COUNT) + 1;

		super.put(OUTPUT_PORT.CURRENT_COUNT, count);
		super.put(OUTPUT_PORT.RELAYED_OBJECT, inputObject);
	}

}
