/***************************************************************************
 * Copyright 2011 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
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

package kieker.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows spwan the execution of an {@link AnalysisController} into a separate
 * {@link Thread}. The thread with the {@link AnalysisController} instance
 * provided in the constructor
 * {@link #AnalysisControllerThread(AnalysisController)} is started by calling
 * the {@link #start()} method. The analysis can be terminated by calling the
 * {@link #terminate()} method which delegates the call to the
 * {@link kieker.analysis.AnalysisController#terminate()} method.
 * 
 * @author Andre van Hoorn
 * 
 */
public class AnalysisControllerThread extends Thread {
	private static final Log log = LogFactory.getLog(AnalysisController.class);

	private final AnalysisController analysisInstance;

	@SuppressWarnings("unused")
	/** Must not be used for construction. */
	private AnalysisControllerThread() {
		this.analysisInstance = null;
	}

	public AnalysisControllerThread(final AnalysisController analysisController) {
		this.analysisInstance = analysisController;
	}

	@Override
	public void run() {
		if (!this.analysisInstance.run()) {
			AnalysisControllerThread.log
					.error("Analysis returned with error");
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		try {
			// wait until AnalysisController is initialized
			this.analysisInstance.getInitializationLatch().await();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			// See ticket http://samoa.informatik.uni-kiel.de:8000/kieker/ticket/176
			e.printStackTrace();
		}
	}
	
	/**
	 * Initiates a termination of the executed {@link AnalysisController}.
	 */
	public void terminate() {
		/* terminate the analysis instance */
		this.analysisInstance.terminate();
	}
}
