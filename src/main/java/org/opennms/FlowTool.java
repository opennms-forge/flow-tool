package org.opennms;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.google.common.base.Strings;

public class FlowTool {
    @Option(name = "-host", usage = "elastic search host")
    private String host = "localhost";

    @Option(name = "-port", usage = "elastic search port")
    private int port = 9200;

    @Option(name = "-start", usage = "search for flows newer than start")
    private Long start;

    @Option(name = "-end", usage = "search for flows older than end")
    private Long end;

    @Option(name = "-duration", usage = "duration to search for")
    private Long duration;

    @Option(name = "-delay", usage = "delay between search attempts")
    private Long delay = 250L;

    @Option(name = "-fields", usage = "fields to be displayed")
    private String fields = "first_switched, last_switched, src_addr, src_port, dst_addr, dst_port, direction, bytes, packets";

    @Option(name = "-delimiter", usage = "delimiter separating columns")
    private String delimiter = null;

    @Option(name = "-help", usage = "show this help")
    private Boolean help = false;

    @Option(name = "-filter", usage = "filter for flows")
    private String filter = null;

    private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();

    private boolean iterative = false;

    private FlowTool() {
    }

    private void checkArguments() {
        if (start != null && end == null && duration != null) {
            // start + duration
            end = start + duration;
            return;
        }

        if (start == null && end != null && duration != null) {
            // end - duration
            start = end - duration;
            return;
        }

        if (start != null && end != null && duration == null) {
            // start / end
            return;
        }

        if (start != null && end == null && duration == null) {
            iterative = true;
            return;
        }

        if (start == null && end == null) {
            // duration
            if (duration == null) {
                duration = 1L;
            }

            start = System.currentTimeMillis() - duration;
            iterative = true;
            return;
        }

        System.err.println("Error evaluating start/end/duration arguments. Valid combinations are:");
        System.err.println("  start/end set      : from start to end");
        System.err.println("  start/duration set : from start to (start + duration)");
        System.err.println("  end/duration set   : from (end - duration) to end");
        System.err.println("  start set          : from start to now, polling for more");
        System.err.println("  duration set       : from (now - duration), polling for more");
        System.err.println("  nothing set        : from now, polling for more");
        System.exit(1);
    }

    private void sleep() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {
        checkArguments();

        Long timestamp = start;
        final String[] columnNames = fields.split("[, \n]+");
        final Map<String, Integer> columnWidths = new TreeMap<>();

        final JexlExpression jexlExpression;

        if (!Strings.isNullOrEmpty(filter)) {
            jexlExpression = jexl.createExpression(filter);
        } else {
            jexlExpression = null;
        }

        do {
            final FlowSearch flowSearch = new FlowSearch(host, port, timestamp, end);
            final List<Flow> flows = flowSearch.getFlows();
            if (flows.size() > 0) {
                for (final Flow flow : flows) {
                    if (jexlExpression != null) {
                        final JexlContext context = new MapContext(flow.getFlowData());
                        final Boolean matchesFilter = (Boolean) jexlExpression.evaluate(context);
                        if (!matchesFilter) {
                            continue;
                        }
                    }
                    if (delimiter != null) {
                        System.out.println(Arrays.stream(columnNames).map(c -> !Strings.isNullOrEmpty(flow.getString(c)) ? flow.getString(c) : "-").collect(Collectors.joining(delimiter)));
                    } else {
                        boolean first = true;
                        for (final String column : columnNames) {
                            final String value = !Strings.isNullOrEmpty(flow.getString(column)) ? flow.getString(column) : "-";
                            final int newWidth = Math.max(value.length(), columnWidths.computeIfAbsent(column, k -> value.length()));
                            columnWidths.put(column, newWidth);
                            if (!first) {
                                System.out.printf(" | ");
                            }
                            first = false;
                            System.out.printf("%-" + newWidth + "s", value);
                        }
                        System.out.println();
                    }
                    if (iterative) {
                        timestamp = Math.max(timestamp, flow.getLong("timestamp"));
                    }
                }
            }
            if (iterative) {
                sleep();
            }
        } while (iterative);
    }

    public static void main(String[] args) {
        final FlowTool flowTool = new FlowTool();
        final CmdLineParser parser = new CmdLineParser(flowTool);
        try {
            parser.parseArgument(args);
            if (flowTool.help) {
                parser.printUsage(System.err);
                System.exit(0);
            }
            flowTool.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
        }
    }
}
