
package com.netflix.conductor.core.instrumentation;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.springframework.stereotype.Component;

@Component
public class InstrumentationUtil {


    public InstrumentationUtil() {
    }

    public static Counter queryTotal = Counter.build()
            .name("ff_flo_query_total")
            .labelNames("query", "response", "exception")
            .help("Query Total").register();

    public static Histogram dbQueryLatency = Histogram.build()
            .name("ff_flo_query_latency")
            .labelNames("query")
            .help("Query Latency").register();

    public static Gauge dbQueryInProgress = Gauge.build()
            .name("ff_flo_queries_in_progress")
            .labelNames("query")
            .help("In progress query count").register();


    public enum Status {INVOCATION, SUCCESS, FAILURE}

}
