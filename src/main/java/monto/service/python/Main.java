package monto.service.python;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.zeromq.ZContext;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.resources.ResourceServer;

public class Main {

  private static ResourceServer resourceServer;

  public static void main(String[] args) throws Exception {
    ZContext context = new ZContext(1);
    List<MontoService> services = new ArrayList<>();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                System.out.println("terminating...");
                try {
                  for (MontoService service : services) {
                    service.stop();
                  }
                  resourceServer.stop();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                context.destroy();
                System.out.println("everything terminated, good bye");
              }
            });

    Options options = new Options();
    options
        .addOption("tokenizer", false, "enable Python tokenizer")
        .addOption("parser", false, "enable Python parser")
        .addOption("outliner", false, "enable Python outliner")
        .addOption("identifierfinder", false, "enable Python identifier finder")
        .addOption("codecompletioner", false, "enable Python code completioner")
        .addOption("address", true, "address of services")
        .addOption("registration", true, "address of broker registration")
        .addOption("resources", true, "port for http resource server")
        .addOption("debug", false, "enable debugging output");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    ZMQConfiguration zmqConfig =
        new ZMQConfiguration(
            context,
            cmd.getOptionValue("address"),
            cmd.getOptionValue("registration"),
            Integer.parseInt(cmd.getOptionValue("resources")));

    resourceServer =
        new ResourceServer(
            Main.class.getResource("/images").toExternalForm(), zmqConfig.getResourcePort());
    try {
      resourceServer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (cmd.hasOption("tokenizer")) {
      services.add(new PythonTokenizer(zmqConfig));
    }
    if (cmd.hasOption("parser")) {
      services.add(new PythonParser(zmqConfig));
    }
    if (cmd.hasOption("outliner")) {
      services.add(new PythonOutliner(zmqConfig));
    }
    if (cmd.hasOption("identifierfinder")) {
      services.add(new PythonIdentifierFinder(zmqConfig));
    }
    if (cmd.hasOption("codecompletioner")) {
      services.add(new PythonCodeCompletioner(zmqConfig));
    }
    if (cmd.hasOption("debug")) {
      services.forEach(MontoService::enableDebugging);
    }

    for (MontoService service : services) {
      try {
        service.start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
