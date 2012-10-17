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

package kieker.tools.traceAnalysis.filter.visualization.dependencyGraph;

import kieker.tools.traceAnalysis.systemModel.AbstractMessage;

/**
 * Decorator to attach response time data to graph nodes.
 * 
 * @author Holger Knoche
 * 
 */
public class ResponseTimeNodeDecorator extends AbstractNodeDecorator {

	/**
	 * Creates a new response time decorator.
	 */
	public ResponseTimeNodeDecorator() {
		// empty default constructor
	}

	@Override
	public void processMessage(final AbstractMessage message, final DependencyGraphNode<?> sourceNode, final DependencyGraphNode<?> targetNode) {
		// Ignore internal executions
		if (sourceNode.equals(targetNode)) {
			return;
		}

		ResponseTimeDecoration timeDecoration = targetNode.getDecoration(ResponseTimeDecoration.class);

		if (timeDecoration == null) {
			timeDecoration = new ResponseTimeDecoration();
			targetNode.addDecoration(timeDecoration);
		}

		timeDecoration.registerExecution(message.getReceivingExecution());
	}

}
