package io.dapr.examples.configuration.http;

import io.dapr.examples.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class ConfigurationSubscriber {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addRequiredOption("p", "port", true, "The port this app will listen on");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // If port string is not valid, it will throw an exception.
        int port = Integer.parseInt(cmd.getOptionValue("port"));

        // Start Dapr's callback endpoint.
        DaprApplication.start(port);
    }
}
