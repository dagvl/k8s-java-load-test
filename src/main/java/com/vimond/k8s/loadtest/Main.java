package com.vimond.k8s.loadtest;

import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.MetricsServlet;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;


public class Main {
    static final Summary rpcDurationSeconds = Summary.build()
            .name("rpc_duration_seconds")
            .help("Duration of rpc calls")
            .quantile(.5, .05)
            .quantile(.9, .01)
            .quantile(.99, .001)
            .labelNames("function")
            .register();

    static final Counter errorCount = Counter.build()
            .name("rpc_error_count")
            .help("Number of calls ending with errors")
            .labelNames("function", "reason")
            .register();

    private Server server;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt("sts-sleep-ms")
                .hasArg()
                .build());

        options.addOption(Option.builder()
                .longOpt("metadata-sleep-ms")
                .hasArg()
                .build());

        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        CommandLineParser parser = new DefaultParser();


        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }


        int stsSleepMs = Integer.parseInt(cmd.getOptionValue("sts-sleep-ms"));
        int metadataSleepMs = Integer.parseInt(cmd.getOptionValue("metadata-sleep-ms"));
        main.Run(stsSleepMs, metadataSleepMs);
    }


    private void Run(int metadataDelay, int stsDelay) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(OkServlet.class, "/healthcheck");
        handler.addServletWithMapping(MetricsServlet.class, "/metrics");

        server.setConnectors(new Connector[]{connector});
        server.setHandler(handler);

        server.start();
        //server.join();


        Thread stsThread = new StsThread(stsDelay);
        stsThread.start();

        Thread metadataThread = new MetadataThread(metadataDelay);
        metadataThread.start();

        System.out.println("enter to quit");
        System.in.read();

        stsThread.stop();
        metadataThread.stop();
        server.stop();
    }
}
