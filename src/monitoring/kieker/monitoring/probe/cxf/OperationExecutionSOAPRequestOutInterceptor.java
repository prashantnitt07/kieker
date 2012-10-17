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

package kieker.monitoring.probe.cxf;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapHeaderOutFilterInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.ControlFlowRegistry;
import kieker.monitoring.core.registry.SessionRegistry;
import kieker.monitoring.probe.IMonitoringProbe;
import kieker.monitoring.timer.ITimeSource;

/**
 * CXF OutInterceptor to set the sessionIdentifier header for an outgoing soap message.
 *   
 * Look here how to add it to your client config: http://cwiki.apache.org/CXF20DOC/interceptors.html
 * 
 * Setting the soap header with jaxb or aegis databinding didn't work yet:
 * http://www.nabble.com/Add-%22out-of-band%22-soap-header-using-simple-frontend-td19380093.html
 */
/**
 * 
 * @author Dennis Kieselhorst, Andre van Hoorn
 */
public class OperationExecutionSOAPRequestOutInterceptor extends SoapHeaderOutFilterInterceptor implements IMonitoringProbe {

	public static final String SESSION_ID_ASYNC_TRACE = "NOSESSION-ASYNCOUT";

	protected static final ControlFlowRegistry CF_REGISTRY = ControlFlowRegistry.INSTANCE;
	protected static final SessionRegistry SESSION_REGISTRY = SessionRegistry.INSTANCE;
	protected static final SOAPTraceRegistry SOAP_REGISTRY = SOAPTraceRegistry.getInstance();

	/**
	 * Note we are using this IMonitoringController only to access ITimeSource which
	 * is configured for the singleton instance, as this is the instance used by the
	 * corresponding other CXF probes. Depending on the configuration, the time may
	 * differ from Kieker's default timer (SystemNanoTimer).
	 */
	protected final IMonitoringController monitoringController;
	protected final ITimeSource timeSource;

	public OperationExecutionSOAPRequestOutInterceptor() {
		this(MonitoringController.getInstance());
	}

	public OperationExecutionSOAPRequestOutInterceptor(final IMonitoringController monitoringCtrl) {
		this.monitoringController = monitoringCtrl;
		this.timeSource = this.monitoringController.getTimeSource();
	}

	@Override
	public void handleMessage(final SoapMessage msg) throws Fault {
		if (!this.monitoringController.isProbeActivated(OperationExecutionSOAPResponseInInterceptor.SIGNATURE)) {
			return;
		}

		String sessionID = SESSION_REGISTRY.recallThreadLocalSessionId();

		long traceId = CF_REGISTRY.recallThreadLocalTraceId();
		int eoi;
		int ess;

		/*
		 * Store entry time tin for this trace.
		 * This value will be used by the corresponding invocation of the
		 * ResponseOutProbe.
		 */
		final long tin = this.timeSource.getTime();
		boolean isEntryCall = false; // set true below if is entry call

		if (traceId == -1) {
			/*
			 * traceId has not been registered before.
			 * This might be caused by a thread which has been spawned
			 * asynchronously. We will now acquire a thread id and store it
			 * in the thread local variable.
			 */
			traceId = CF_REGISTRY.getAndStoreUniqueThreadLocalTraceId();
			eoi = 0; // eoi of this execution
			CF_REGISTRY.storeThreadLocalEOI(eoi);
			ess = 0; // ess of this execution
			CF_REGISTRY.storeThreadLocalESS(ess);
			isEntryCall = true;
			if (sessionID == null) {
				sessionID = SESSION_ID_ASYNC_TRACE;
				SESSION_REGISTRY.storeThreadLocalSessionId(sessionID);
			}
		} else {
			/* thread-local traceId exists: eoi and ess should have been registered before */
			eoi = CF_REGISTRY.incrementAndRecallThreadLocalEOI();
			ess = CF_REGISTRY.recallThreadLocalESS(); // do not increment in this case!
			if (sessionID == null) {
				sessionID = OperationExecutionRecord.NO_SESSION_ID;
			}
		}

		SOAP_REGISTRY.storeThreadLocalOutRequestIsEntryCall(isEntryCall);
		SOAP_REGISTRY.storeThreadLocalOutRequestTin(tin);

		final Document d = DOMUtils.createDocument();
		Element e;
		Header hdr;
		/* Add sessionId to header */
		e = d.createElementNS(SOAPHeaderConstants.NAMESPACE_URI, SOAPHeaderConstants.SESSION_QUALIFIED_NAME);
		e.setTextContent(sessionID);
		hdr = new Header(SOAPHeaderConstants.SESSION_IDENTIFIER_QNAME, e);
		msg.getHeaders().add(hdr);
		/* Add traceId to header */
		e = d.createElementNS(SOAPHeaderConstants.NAMESPACE_URI, SOAPHeaderConstants.TRACE_QUALIFIED_NAME);
		e.setTextContent(Long.toString(traceId));
		hdr = new Header(SOAPHeaderConstants.TRACE_IDENTIFIER_QNAME, e);
		msg.getHeaders().add(hdr);
		/* Add eoi to header */
		e = d.createElementNS(SOAPHeaderConstants.NAMESPACE_URI, SOAPHeaderConstants.EOI_QUALIFIED_NAME);
		e.setTextContent(Integer.toString(eoi));
		hdr = new Header(SOAPHeaderConstants.EOI_IDENTIFIER_QNAME, e);
		msg.getHeaders().add(hdr);
		/* Add ess to header */
		e = d.createElementNS(SOAPHeaderConstants.NAMESPACE_URI, SOAPHeaderConstants.ESS_QUALIFIED_NAME);
		e.setTextContent(Integer.toString(ess + 1));
		hdr = new Header(SOAPHeaderConstants.ESS_IDENTIFIER_QNAME, e);
		msg.getHeaders().add(hdr);
	}
}
