package monto.service.python;

import monto.service.MontoService;
import org.apache.commons.cli.*;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;

public class PythonServices {
	
	public static void main(String[] args) throws ParseException{
		 String address = "tcp://*";
	        String regAddress = "tcp://*:5004";
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
	                .addOption("registration", true, "address of broker registration");

	        CommandLineParser parser = new DefaultParser();
	        CommandLine cmd = parser.parse(options, args);

	        if (cmd.hasOption("address")) {
	            address = cmd.getOptionValue("address");
	        }

	        if (cmd.hasOption("registration")) {
	            regAddress = cmd.getOptionValue("registration");
	        }

	        if (cmd.hasOption("t")) {
	            services.add(new PythonTokenizer(context, address, regAddress, "pythonTokenizer"));
	        }
	        if (cmd.hasOption("p")) {
	            services.add(new PythonParser(context, address, regAddress, "pythonParser"));
	        }
	        if (cmd.hasOption("o")) {
	            services.add(new PythonOutliner(context, address, regAddress, "pythonOutliner"));
	        }
	        if (cmd.hasOption("c")) {
	            services.add(new PythonCodeCompletion(context, address, regAddress, "pythonCodeCompletioner"));
	        }

	        for (MontoService service : services) {
	            service.start();
	        }
	}

}
