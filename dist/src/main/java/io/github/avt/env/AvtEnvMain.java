package io.github.avt.env;

import io.github.avt.env.daemon.AVTService;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvtEnvMain {
  private static final Logger log = LoggerFactory.getLogger(AvtEnvMain.class);

  public static void main(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption("p", "port", true, "The port to start env on");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    if (cmd.hasOption("p")) {
      int port = Integer.parseInt(cmd.getOptionValue("p"));
      startOnPort(port);
    } else {
      startOnPort(AVTService.DEFAULT_PORT);
    }
  }

  private static void startOnPort(int port) {
    log.info("Starting env on port: '{}'", port);
    VertxOptions vertxOptions = new VertxOptions();
    Vertx vertx = Vertx.vertx(vertxOptions);
    vertx.deployVerticle(new AVTService(port));
  }
}
