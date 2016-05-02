package monto.service.python;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.resources.ResourceServer;
import monto.service.types.ServiceID;

import org.apache.commons.cli.*;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;

public class PythonServices {
	public static final ServiceID PYTHON_TOKENIZER = new ServiceID("pythonTokenizer");
	public static final ServiceID PYTHON_PARSER = new ServiceID("pythonParser");
	public static final ServiceID PYTHON_OUTLINER = new ServiceID("pythonOutliner");
	public static final ServiceID PYTHON_CODE_COMPLETION = new ServiceID("pythonCodeCompletion");
	private static ResourceServer resourceServer;

	
	public static void main(String[] args) throws ParseException{
	        ZContext context = new ZContext(1);
	        List<MontoService> services = new ArrayList<>();
	        

	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            @Override
	            public void run() {
	                System.out.println("terminating...");
	                try{
	                	for (MontoService service : services) {
	                		service.stop();
	                		resourceServer.stop();
	                	}
	                } catch (Exception e){
	                	e.printStackTrace();
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
	                .addOption("configuration", true, "address of configuration messages")
	                .addOption("resources", true, "port for http resource server")
					.addOption("dyndeps", true, "port for dynamic dependency registration");

	        CommandLineParser parser = new DefaultParser();
	        CommandLine cmd = parser.parse(options, args);
	        
	        ZMQConfiguration zmqConfig = new ZMQConfiguration(
	        		context,
	        		cmd.getOptionValue("address"),
	        		cmd.getOptionValue("registration"),
	        		cmd.getOptionValue("configuration"),
					cmd.getOptionValue("dyndeps"),
	        		Integer.parseInt(cmd.getOptionValue("resources")));
	        
	        resourceServer = new ResourceServer(PythonServices.class.getResource("/images").toExternalForm(), zmqConfig.getResourcePort());
	        try {
				resourceServer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}

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
	            try {
					service.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	}

}
