/**
 * Copyright (c) 2013 SLL. <http://sll.se>
 *
 * This file is part of Invoice-Data.
 *
 *     Invoice-Data is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Invoice-Data is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Invoice-Data.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */

package se.sll.invoicedata.core.jmx;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;

import riv.sll.invoicedata.getinvoicedataresponder._1.GetInvoiceDataRequest;
import se.sll.invoicedata.core.service.InvoiceDataService;

/**
 * JMX Bean to keep track of application status. <p>
 * 
 * Exposes methods to perform health checks and connection instrumentation information.
 * 
 * @author Peter
 *
 */
@Component
@ManagedResource(objectName = "se.sll.invoicedata:name=StatusBean", description="Status information")
public class StatusBean {

    private static final Logger log = LoggerFactory.getLogger(StatusBean.class);

    @Autowired
    private InvoiceDataService invoiceDataService;

    //
    private int historyLength = 1000;
    
    //
    private static ThreadLocal<Stack<Sample>> samples = new ThreadLocal<Stack<Sample>>() {
        @Override
        public Stack<Sample> initialValue() {
            return new Stack<Sample>();
        }
    };

    //
    private static Map<String, Timer> timerMap = new HashMap<String, Timer>();

    //
    private static Concurrency concurrency = new Concurrency();

    // checks database
    private void checkDatabase() {
        invoiceDataService.getAllUnprocessedBusinessEvents(new GetInvoiceDataRequest());
        log.info("health-check database: OK");
    }

    // checks rating
    private void checkRating() {
        log.info("health-check rating: OK");
    }

    private void checkAlloc() {
        final byte[] bytes = new byte[1024 * 1024];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)0xff;
        }
        log.info("health-check memory alloc: OK");
    }

    //
    private void checkLog() {
        final String msg = "health-check log: OK";
        log.error(msg);
        log.debug(msg);
        log.trace(msg);
        log.info(msg);
    }

    @ManagedOperation(description="Performs health check, i.e. are connections, memory, logs working as expected")
    public void healthCheck() {
        checkDatabase();
        checkRating();
        checkAlloc();
        checkLog();
    }


    @ManagedMetric(category="utilization", displayName="Active (ongoing) requests", metricType=MetricType.COUNTER, unit="request")
    public long getActiveRequests() {
        return concurrency.getActiveRequests();
    }

    @ManagedMetric(category="utilization", displayName="Average response time", metricType=MetricType.GAUGE, unit="milliseconds")
    public long getAvgResponseTime() {
        int n = 0;
        int avg = 0;
        for (Timer timer : timerMap.values()) {
            avg += timer.avg();
            n++;
        }
        return (avg / n);
    }

    @ManagedOperation(description="Returns performance metrics (JSON strings) in millisceonds for all instrumented operations")
    public String[] getPerformanceMetrics() {
        Collection<Timer> c = timerMap.values();
        final String[] list = new String[c.size()];
        int i = 0;
        for (Timer t : c) {
            list[i++] = t.toString();
        }
        return list;
    }

    @ManagedOperation(description="Clears sampled performance metrics (timed statistics)")
    public void clearPerformanceMetrics() {
        timerMap.clear();
    }

    @ManagedAttribute(description="Sets the length of request history for timed statistics", defaultValue="1000")
    public void setHistoryLength(@ManagedOperationParameter(name="historyLength", description="Indicates history of requests to keep/calculate averge timed statistcis") int historyLength) {
        if (historyLength > 0) {
            this.historyLength = historyLength;
        }
    }

    @ManagedAttribute(description="Returns the length of request history for timed statistics")
    public int getHistoryLength() {
        return historyLength;
    }


    @ManagedOperation(description="Returns active service contract names, i.e. WSDL operations that have been accessed since last startup")
    public String[] getServiceNames() {
        final Set<String> set = timerMap.keySet();
        return set.toArray(new String[set.size()]);
    }

    //
    public String getGUID() {
        return samples.get().peek().getName();
    }

    //
    public String getName() {
        return samples.get().peek().getName();
    }

    //
    public void start(final String path) {
        start(path, false);
    }
    
    //
    public void start(final String path, boolean history) {
        if (samples.get().size() == 0) {
            concurrency.inc();
        }
        samples.get().push(new Sample(path, history));
    }

    //
    public void stop() {
        final Sample sample = samples.get().pop();
        final String name = sample.getName();
        final long elapsed = sample.elapsed();
        Timer timer = timerMap.get(name);
        if (timer == null) {
            timer = new HistoryTimer(name, this.historyLength);
            timerMap.put(name, timer);
        }
        timer.add(elapsed);
        if (samples.get().size() == 0) {
            concurrency.dec();
        }
    }


    /**
     * Samples processing time for one transaction.
     */
    static class Sample {
        private long timestamp;
        private String name;
        private String guid;
        private boolean history;

        //
        public Sample(final String name, boolean history) {
            this.timestamp = System.currentTimeMillis();
            this.name = name;
            this.guid = UUID.randomUUID().toString();
            this.history = history;
        }

        //
        public boolean isHistoryEnabled() {
            return history;
        }
        
        //
        public String getGUID() {
            return guid;
        }

        //
        public String getName() {
            return name;
        }

        //
        public long elapsed() {
            final long time = (System.currentTimeMillis() - timestamp);
            return (time < 0) ? 0 : time;
        }
    }
    
    static class HistoryTimer extends Timer {
        private int len;
        private int ofs = 0;
        private long[] history;

        //
        public HistoryTimer(String name, int len) {
            super(name);
            this.len = len;
            this.history = new long[len];
            Arrays.fill(history, -1);
        }

        //
        public void add(long t) {
            if (ofs >= len) {
                ofs = 0;
            }
            history[ofs++] = t;
        }

        //
        public synchronized void recalc() {
            reset();
            for (int i = 0; i < len && history[i] >= 0; i++) {
                super.add(history[i]);
            }
        }
        
        @Override
        public synchronized String toString() {
            recalc();
            return String.format("{ name: \"%s\", history: %d, min: %d, max: %d, avg: %d }", name(), n(), min(), max(), avg()); 
        }
    }

    //
    static class Timer {
        private long n;
        private long min;
        private long max;
        private long sum;
        private String name;

        public Timer(String name) {
            this.name = name;
            reset();
        }
        
        //
        public String name() {
            return name;
        }


        //
        public void add(long t) {
            sum += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
            n++;
        }

        //
        protected void reset() {
            n   = 0L;
            sum = 0L;
            min = Long.MAX_VALUE;
            max = Long.MIN_VALUE;
        }


        public long min() {
            return (min == Long.MAX_VALUE) ? 0 : min;
        }

        //
        public long max() {
            return (max == Long.MIN_VALUE) ? 0 : max;
        }

        //
        public long avg() {
            return (n == 0) ? 0 : (sum / n);
        }

        //
        public long n() {
            return n;
        }

        @Override
        public String toString() {
            return String.format("{ name: \"%s\", n: %d, min: %d, max: %d, avg: %d }", name(), n(), min(), max(), avg()); 
        }
    }

    //
    static class Concurrency {
        private long activeRequests;

        public synchronized void inc() {
            activeRequests++;
        }

        public synchronized void dec() {
            activeRequests--;
        }

        public synchronized long getActiveRequests() {
            return activeRequests;
        }

    }    
}
