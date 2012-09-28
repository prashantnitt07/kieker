/***************************************************************************
 * Copyright 2012 Kieker Project (http://kieker-monitoring.net)
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

package kieker.monitoring.core.signaturePattern;

/**
 * @author Bjoern Weissenfels, Jan Waller
 */
public class InvalidPatternException extends RuntimeException {
	private static final long serialVersionUID = 7568907124941706485L;

	public InvalidPatternException(final String reason) {
		super(reason);
	}

	public InvalidPatternException(final String reason, final Throwable cause) {
		super(reason, cause);
	}
}
