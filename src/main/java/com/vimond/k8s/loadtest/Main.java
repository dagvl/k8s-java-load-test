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

        options.addOption(Option.builder()
                .longOpt("listen-address")
                .hasArg()
                .build());

        options.addOption(Option.builder()
                .longOpt("die-after-ms")
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


        int stsSleepMs = 5000;
        if(cmd.hasOption("sts-sleep-ms")) {
            stsSleepMs = Integer.parseInt(cmd.getOptionValue("sts-sleep-ms"));
        }

        int metadataSleepMs = 5000;
        if(cmd.hasOption("metadata-sleep-ms")) {
            Integer.parseInt(cmd.getOptionValue("metadata-sleep-ms"));
        }

        int listenAddress = 8080;
        if(cmd.hasOption("listen-address")) {
            listenAddress = Integer.parseInt(cmd.getOptionValue("listen-address"));
        }


        int dieAfterMs = 180000;
        if(cmd.hasOption("die-after-ms")) {
            dieAfterMs = Integer.parseInt(cmd.getOptionValue("die-after-ms"));
        }

        main.Run(stsSleepMs, metadataSleepMs, listenAddress, dieAfterMs);
    }


    private void Run(int metadataDelay, int stsDelay, int listenAddress, int dieAfterMs) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(listenAddress);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(OkServlet.class, "/healthcheck");
        handler.addServletWithMapping(MetricsServlet.class, "/metrics");

        server.setConnectors(new Connector[]{connector});
        server.setHandler(handler);

        server.start();

        Thread stsThread = new StsThread(stsDelay);
        stsThread.start();

        Thread metadataThread = new MetadataThread(metadataDelay);
        metadataThread.start();

        QuitThread quitThread = new QuitThread(dieAfterMs);
        quitThread.start();

        server.join();

        stsThread.stop();
        metadataThread.stop();
        quitThread.stop();
    }
}
