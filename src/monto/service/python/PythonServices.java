package monto.service.python;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;

import org.apache.commons.cli.*;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;

public class PythonServices {
	
	public static void main(String[] args) throws ParseException{
	        ZContext context = new ZContext(1);
	        List<MontoService> services = new ArrayList<>();

	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            @Override
	            public void run() {
	                System.out.println("terminating...");
	                for (MontoService service : services) {
	                    service.stop();
	                }
	                context.destroy();
	                System.out.println("everything terminated, good bye");
	            }
	        });

	        Options options = new Options();
	        options.addOption("t", false, "enable python tokenizer")
	                .addOption("p", false, "enable python parser")
	                .addOption("o", false, "enable python outliner")
	                .addOption("c", false, "enable python code completioner")
	                .addOption("address", true, "address of services")
	                .addOption("registration", true, "address of broker registration")
	                .addOption("configuration", true, "address of configuration messages");

	        CommandLineParser parser = new DefaultParser();
	        CommandLine cmd = parser.parse(options, args);
	        
	        ZMQConfiguration zmqConfig = new ZMQConfiguration(
	        		context,
	        		cmd.getOptionValue("address"),
	        		cmd.getOptionValue("registration"),
	        		cmd.getOptionValue("configuration"));

	        if (cmd.hasOption("t")) {
	            services.add(new PythonTokenizer(zmqConfig));
	        }
	        if (cmd.hasOption("p")) {
	            services.add(new PythonParser(zmqConfig));
	        }
	        if (cmd.hasOption("o")) {
	            services.add(new PythonOutliner(zmqConfig));
	        }
	        if (cmd.hasOption("c")) {
	            services.add(new PythonCodeCompletion(zmqConfig));
	        }

	        for (MontoService service : services) {
	            service.start();
	        }
	}

}
