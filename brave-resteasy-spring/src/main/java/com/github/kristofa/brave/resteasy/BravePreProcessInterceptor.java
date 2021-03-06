package com.github.kristofa.brave.resteasy;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.kristofa.brave.BraveHttpHeaders;
import com.github.kristofa.brave.EndpointSubmitter;
import com.github.kristofa.brave.IdConversion;
import com.github.kristofa.brave.ServerTracer;
import com.twitter.zipkin.gen.Endpoint;

import static com.github.kristofa.brave.internal.Util.checkNotNull;
import static java.lang.String.format;

/**
 * Rest Easy {@link PreProcessInterceptor} that will:
 * <ol>
 * <li>Set {@link Endpoint} information for our service in case it is not set yet.</li>
 * <li>Get trace data (trace id, span id, parent span id) from http headers and initialize state for request + submit 'server
 * received' for request.</li>
 * <li>If no trace information is submitted we will start a new span. In that case it means client does not support tracing
 * and should be adapted.</li>
 * </ol>
 *
 * @author kristof
 */
@Component
@Provider
@ServerInterceptor
public class BravePreProcessInterceptor implements PreProcessInterceptor {

    private final static Logger LOGGER = Logger.getLogger(BravePreProcessInterceptor.class.getName());

    private final EndpointSubmitter endpointSubmitter;
    private final ServerTracer serverTracer;

    @Context
    HttpServletRequest servletRequest;

    /**
     * Creates a new instance.
     *
     * @param endpointSubmitter {@link EndpointSubmitter}. Should not be <code>null</code>.
     * @param serverTracer {@link ServerTracer}. Should not be <code>null</code>.
     */
    @Autowired
    public BravePreProcessInterceptor(final EndpointSubmitter endpointSubmitter, final ServerTracer serverTracer) {
        this.endpointSubmitter = checkNotNull(endpointSubmitter, "Null endpointSubmitter");
        this.serverTracer = checkNotNull(serverTracer, "Null serverTracer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerResponse preProcess(final HttpRequest request, final ResourceMethod method) throws Failure,
        WebApplicationException {

        submitEndpoint();

        serverTracer.clearCurrentSpan();
        final TraceData traceData = getTraceData(request);

        if (Boolean.FALSE.equals(traceData.shouldBeTraced())) {
            serverTracer.setStateNoTracing();
            LOGGER.fine("Received indication that we should NOT trace.");
        } else {
            final String spanName = getSpanName(request, traceData);
            if (traceData.getTraceId() != null && traceData.getSpanId() != null) {

                LOGGER.fine("Received span information as part of request.");
                serverTracer.setStateCurrentTrace(traceData.getTraceId(), traceData.getSpanId(),
                    traceData.getParentSpanId(), spanName);
            } else {
                LOGGER.fine("Received no span state.");
                serverTracer.setStateUnknown(spanName);
            }
            serverTracer.setServerReceived();
        }
        return null;
    }

    private String getSpanName(final HttpRequest request, final TraceData traceData) throws WebApplicationException {
        if (traceData.getSpanName() != null && !"".equals(traceData.getSpanName().trim())) {
            return traceData.getSpanName();
        } else {
            return request.getPreprocessedPath();
        }
    }

    private void submitEndpoint() {
        if (!endpointSubmitter.endpointSubmitted()) {
            final String localAddr = servletRequest.getLocalAddr();
            final int localPort = servletRequest.getLocalPort();
            final String contextPath = getContextPathWithoutFirstSlash();
            LOGGER.fine(format("Setting endpoint: addr: %s, port: %s, contextpath: %s", localAddr, localPort, contextPath));
            endpointSubmitter.submit(localAddr, localPort, contextPath);
        }
    }

    private String getContextPathWithoutFirstSlash() {
        final String contextPath = servletRequest.getContextPath();
        if (contextPath.startsWith("/")) {
            return contextPath.substring(1);
        } else {
            return contextPath;
        }
    }

    private TraceData getTraceData(final HttpRequest request) {
        final HttpHeaders httpHeaders = request.getHttpHeaders();
        final MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();

        final TraceData traceData = new TraceData();

        for (final Entry<String, List<String>> headerEntry : requestHeaders.entrySet()) {
            LOGGER.fine(format("%s=%s", headerEntry.getKey(), headerEntry.getValue()));
            if (BraveHttpHeaders.TraceId.getName().equalsIgnoreCase(headerEntry.getKey())) {
                traceData.setTraceId(getFirstLongValueFor(headerEntry));
            } else if (BraveHttpHeaders.SpanId.getName().equalsIgnoreCase(headerEntry.getKey())) {
                traceData.setSpanId(getFirstLongValueFor(headerEntry));
            } else if (BraveHttpHeaders.ParentSpanId.getName().equalsIgnoreCase(headerEntry.getKey())) {
                traceData.setParentSpanId(getFirstLongValueFor(headerEntry));
            } else if (BraveHttpHeaders.Sampled.getName().equalsIgnoreCase(headerEntry.getKey())) {
                traceData.setShouldBeSampled(getFirstBooleanValueFor(headerEntry));
            } else if (BraveHttpHeaders.SpanName.getName().equalsIgnoreCase(headerEntry.getKey())) {
                traceData.setSpanName(getFirstStringValueFor(headerEntry));
            }
        }
        return traceData;
    }

    private Long getFirstLongValueFor(final Entry<String, List<String>> headerEntry) {

        final String firstStringValueFor = getFirstStringValueFor(headerEntry);
        return firstStringValueFor == null ? null : IdConversion.convertToLong(firstStringValueFor);

    }

    private Boolean getFirstBooleanValueFor(final Entry<String, List<String>> headerEntry) {
        final String firstStringValueFor = getFirstStringValueFor(headerEntry);
        return firstStringValueFor == null ? null : Boolean.valueOf(firstStringValueFor);
    }

    private String getFirstStringValueFor(final Entry<String, List<String>> headerEntry) {
        final List<String> values = headerEntry.getValue();
        if (values != null && values.size() > 0) {
            return headerEntry.getValue().get(0);
        }
        return null;
    }

}
