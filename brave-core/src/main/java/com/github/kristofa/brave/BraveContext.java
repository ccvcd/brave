package com.github.kristofa.brave;

import com.github.kristofa.brave.SpanAndEndpoint.ServerSpanAndEndpoint;

import java.util.List;
import java.util.Random;

/**
 * This is an alternative public api that can be used instead of {@link Brave}. </p> This api should be used in case the
 * {@link ThreadLocal} approach of {@link Brave} is not working for you and you need to have full control over the Brave
 * context. <a href="https://github.com/pettyjamesm">pettyjamesm</a> indicated he needed this to get Brave working with the
 * <a href="http://www.playframework.com">Play framework</a>. </p> You <b>can't</b> use this api in combination with
 * {@link BraveCallable}, {@link BraveRunnable} and {@link BraveExecutorService}.
 */
public class BraveContext {

    private final SimpleServerAndClientSpanState spanState;

    private final Random randomGenerator;

    private final EndpointSubmitter endpointSubmitter;

    private final AnnotationSubmitter annotationSubmitter;

    /**
     * Initializes brave context.
     */
    public BraveContext() {
        spanState = new SimpleServerAndClientSpanState();
        randomGenerator = new Random();
        endpointSubmitter = new EndpointSubmitter(spanState);
        annotationSubmitter = AnnotationSubmitter.create(ServerSpanAndEndpoint.create(spanState));
    }

    /**
     * Gets {@link EndpointSubmitter}.
     * 
     * @return {@link EndpointSubmitter}.
     */
    public EndpointSubmitter getEndpointSubmitter() {
        return endpointSubmitter;
    }

    /**
     * Get {@link ClientTracer}
     * 
     * @param collector {@link SpanCollector} to use for submitting spans.
     * @param traceFilters {@link TraceFilter Trace filters} to use. Trace filters let you control which requests are traced,
     *            which not.
     * @return ClientTracer.
     */
    public ClientTracer getClientTracer(final SpanCollector collector, final List<TraceFilter> traceFilters) {
        return ClientTracer.builder()
            .state(spanState)
            .randomGenerator(randomGenerator)
            .spanCollector(collector)
            .traceFilters(traceFilters)
            .build();
    }

    /**
     * Gets {@link ServerTracer}
     * 
     * @param collector {@link SpanCollector} to use for submitting spans.
     * @param traceFilters {@link TraceFilter Trace filters} to use. Trace filters let you control which requests are traced,
     *            which not.
     * @return ServerTracer.
     */
    public ServerTracer getServerTracer(final SpanCollector collector, final List<TraceFilter> traceFilters) {
        return ServerTracer.builder()
            .state(spanState)
            .randomGenerator(randomGenerator)
            .spanCollector(collector)
            .traceFilters(traceFilters)
            .build();
    }

    /**
     * Gets {@link AnnotationSubmitter}.
     * 
     * @return {@link AnnotationSubmitter}, used to submit custom application specific annotations.
     */
    public AnnotationSubmitter getServerSpanAnnotationSubmitter() {
        return annotationSubmitter;
    }
}
