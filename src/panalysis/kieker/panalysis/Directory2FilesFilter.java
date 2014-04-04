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

import java.io.File;
import java.io.FileFilter;

import kieker.common.util.filesystem.BinaryCompressionMethod;
import kieker.common.util.filesystem.FSUtil;
import kieker.panalysis.base.AbstractSource;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class Directory2FilesFilter extends AbstractSource<Directory2FilesFilter.OUTPUT_PORT> {

	public static enum OUTPUT_PORT { // NOCS
		FILE
	}

	private final File inputDirectory;

	public Directory2FilesFilter(final File inputDir) {
		super(OUTPUT_PORT.class);
		this.inputDirectory = inputDir;
	}

	public void execute() {
		final File inputDir = this.inputDirectory;

		final File[] inputFiles = inputDir.listFiles(new FileFilter() {
			public boolean accept(final File pathname) {
				final String name = pathname.getName();
				return pathname.isFile()
						&& name.startsWith(FSUtil.FILE_PREFIX)
						&& (name.endsWith(FSUtil.NORMAL_FILE_EXTENSION) || BinaryCompressionMethod.hasValidFileExtension(name));
			}
		});

		for (final File file : inputFiles) {
			this.put(OUTPUT_PORT.FILE, file);
		}
	}

}
