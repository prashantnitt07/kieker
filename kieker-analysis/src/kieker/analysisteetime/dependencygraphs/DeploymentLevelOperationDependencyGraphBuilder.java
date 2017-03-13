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

package kieker.analysisteetime.dependencygraphs;

import kieker.analysisteetime.model.analysismodel.deployment.DeployedComponent;
import kieker.analysisteetime.model.analysismodel.deployment.DeployedOperation;
import kieker.analysisteetime.model.analysismodel.deployment.DeploymentContext;
import kieker.analysisteetime.util.graph.Graph;
import kieker.analysisteetime.util.graph.Vertex;

/**
 * Dependency graph builder for <strong>operation</strong> dependency graphs
 * at the <strong>deployment level</strong>.
 *
 * @author S�ren Henning
 *
 * @since 1.13
 */
public class DeploymentLevelOperationDependencyGraphBuilder extends AbstractDependencyGraphBuilder {

	public DeploymentLevelOperationDependencyGraphBuilder() {
		super();
	}

	@Override
	protected Vertex addVertex(final DeployedOperation deployedOperation) {
		final DeployedOperation operation = deployedOperation;
		final DeployedComponent component = operation.getComponent();
		final DeploymentContext context = component.getDeploymentContext();

		final int contextId = this.identifierRegistry.getIdentifier(context);
		final Vertex contextVertex = this.graph.addVertexIfAbsent(contextId);
		contextVertex.setPropertyIfAbsent("type", DependencyGraphVertexType.DEPLOYMENT_CONTEXT); // TODO move to constant
		contextVertex.setPropertyIfAbsent("name", context.getName()); // TODO move to constant

		final Graph contextSubgraph = contextVertex.addChildGraphIfAbsent();
		final int componentId = this.identifierRegistry.getIdentifier(component);
		final Vertex componentVertex = contextSubgraph.addVertexIfAbsent(componentId);
		componentVertex.setPropertyIfAbsent("type", DependencyGraphVertexType.DEPLOYED_COMPONENT); // TODO move to constant
		componentVertex.setPropertyIfAbsent("name", component.getAssemblyComponent().getComponentType().getName()); // TODO move to constant

		final Graph componentSubgraph = componentVertex.addChildGraphIfAbsent();
		final int operationId = this.identifierRegistry.getIdentifier(operation);
		final Vertex operationVertex = componentSubgraph.addVertexIfAbsent(operationId);
		operationVertex.setPropertyIfAbsent("type", DependencyGraphVertexType.DEPLOYED_OPERATION); // TODO move to constant
		operationVertex.setPropertyIfAbsent("name", operation.getAssemblyOperation().getOperationType().getName()); // TODO move to constant

		return operationVertex;
	}

}
