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

package kieker.test.analysis.junit.plugin.reader.tcp.newversion;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Christian Wulf
 *
 * @since 1.11
 */
public class MockedSocketChannel extends EmptySocketChannelImpl {

	private final ByteBuffer buffer;

	public MockedSocketChannel(final ByteBuffer buffer) {
		super();
		this.buffer = buffer;
	}

	@Override
	public int read(final ByteBuffer dst) throws IOException {
		if (this.buffer.hasRemaining()) {
			dst.put(this.buffer);
			return dst.position();
		}
		return -1;
	}
}
