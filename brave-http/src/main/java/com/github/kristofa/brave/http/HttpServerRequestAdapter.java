package com.github.kristofa.brave.http;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.TraceData;

import java.util.Collection;

/**
 * Created by Krisoft on 6/20/15.
 */
public class HttpServerRequestAdapter implements ServerRequestAdapter {

    @Override
    public TraceData getTraceData() {
        return null;
    }

    @Override
    public String getSpanName() {
        return null;
    }

    @Override
    public Collection<KeyValueAnnotation> requestAnnotations() {
        return null;
    }
}
