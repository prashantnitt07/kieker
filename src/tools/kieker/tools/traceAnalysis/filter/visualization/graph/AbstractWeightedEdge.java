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

package kieker.tools.traceAnalysis.filter.visualization.graph;

import kieker.common.util.MutableInteger;

/**
 * Abstract superclass for weighted edges in the visualization package. This class provides weights for the
 * edge itself and both source and target ends.
 * 
 * @author Holger Knoche
 * 
 * @param <V>
 *            The type of the graph's vertices
 * @param <E>
 *            The type of the graph's edges
 * @param <O>
 *            The type of object from which the graph's elements originate
 */
public abstract class AbstractWeightedEdge<V extends AbstractVertex<V, E, O>, E extends AbstractEdge<V, E, O>, O> extends AbstractEdge<V, E, O> {

	private final MutableInteger sourceWeight = new MutableInteger();
	private final MutableInteger targetWeight = new MutableInteger();
	private final MutableInteger weight = new MutableInteger();

	/**
	 * Creates a new weighted edge between the given vertices.
	 * 
	 * @param source
	 *            The source vertex of the edge
	 * @param target
	 *            The target vertex of the edge
	 */
	public AbstractWeightedEdge(final V source, final V target, final O origin) {
		super(source, target, origin);
	}

	/**
	 * Return this edge's source weight.
	 * 
	 * @return See above
	 */
	public MutableInteger getSourceWeight() {
		return this.sourceWeight;
	}

	/**
	 * Return this edge's target weight.
	 * 
	 * @return See above
	 */
	public MutableInteger getTargetWeight() {
		return this.targetWeight;
	}

	/**
	 * Returns this edge's weight.
	 * 
	 * @return See above
	 */
	public MutableInteger getWeight() {
		return this.weight;
	}

}
