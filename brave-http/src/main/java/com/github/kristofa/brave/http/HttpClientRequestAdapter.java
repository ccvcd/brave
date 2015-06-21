package com.github.kristofa.brave.http;

import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.internal.Nullable;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Abstract ClientRequestAdapter that is to be used with http clients.
 *
 */
public class HttpClientRequestAdapter implements ClientRequestAdapter {

    private final HttpClientRequest request;
    private final ServiceNameProvider serviceNameProvider;
    private final SpanNameProvider spanNameProvider;

    public HttpClientRequestAdapter(HttpClientRequest request, ServiceNameProvider serviceNameProvider, SpanNameProvider spanNameProvider) {
        this.request = request;
        this.serviceNameProvider = serviceNameProvider;
        this.spanNameProvider = spanNameProvider;
    }


    @Override
    public String getSpanName() {
        return spanNameProvider.spanName();
    }

    @Override
    public void addSpanIdToRequest(@Nullable SpanId spanId) {
        if (spanId == null)
        {
            request.addHeader(BraveHttpHeaders.Sampled.getName(), "false");
        }
        else
        {
            request.addHeader(BraveHttpHeaders.Sampled.getName(), "true");
            request.addHeader(BraveHttpHeaders.TraceId.getName(), String.valueOf(spanId.getTraceId()));
            request.addHeader(BraveHttpHeaders.SpanId.getName(), String.valueOf(spanId.getSpanId()));
            if (spanId.getParentSpanId() != null) {
                request.addHeader(BraveHttpHeaders.ParentSpanId.getName(), String.valueOf(spanId.getParentSpanId()));
            }
        }
    }


    @Override
    public Collection<KeyValueAnnotation> requestAnnotations() {
        try {
            URI uri = request.getUri();
            KeyValueAnnotation annotation = KeyValueAnnotation.create("http.uri", uri.toString());
            return Arrays.asList(annotation);
        }
        catch (Exception e){
            return Collections.EMPTY_LIST;
        }
    }


    @Override
    public String getClientServiceName() {
        return serviceNameProvider.serviceName();
    }

}
