package com.topdesk.sqltransfer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public final class Main {
	private static final String VERSION_ID = "0.9";
	
	static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage : sqltransfer <sqltransfer.xml> [args]");
			System.out.println("	Please give the correct location of a valid xml for sqltransfer.");
			System.out.println("");
			System.out.println("Available args:");
			System.out.println("	runonce	- just run sqltransfer once");
			System.out.println("	debug	- run sqltransfer with more debug-info");
			System.exit(1);
		}
		String fileName = args[0];
		Arrays.sort(args);
		boolean runOnce = Arrays.binarySearch(args, "runonce") > -1;
		boolean debug = Arrays.binarySearch(args, "debug") > -1;
        
		try {
			run(fileName, runOnce, debug);
		}
		catch (Exception e) {
			if (logger.getAppender(Main.class.toString()) != null) {
				logger.info("");
				logger.info(String.format("Error (%s): ", e.getClass()));
				logger.info("  " + e.getMessage());
				if (debug) {
					logger.error("", e);
				}
			}
			else {
				System.err.println(e);
			}
			System.exit(1);
		}
	}

	public static void run(String fileName, boolean runOnce, boolean debug) throws IOException, ClassNotFoundException, ParseException {
		String baseDir = System.getProperty("basedir");
		if (baseDir != null && !"".equals(baseDir)) { 
			System.setProperty("user.dir", baseDir);
		}

		File logProperties = new File("etc/logging.properties");
		PropertyConfigurator.configure(logProperties.toURI().toURL());
		logConfiguration(fileName);
		
		File schemaLocation = new File("etc/sqltransfer.xsd");
		SQLtransferXMLParser parser = new SQLtransferXMLParser(new File(fileName), schemaLocation);
		SQLtransferDefinition definition = parser.createPrepareImportDefinition();
		
		SQLtransfer sqlTransfer = new SQLtransfer(definition, debug, false);
		List<Date> times = parser.gatherTimes();
		if (runOnce || times.isEmpty()) {
			sqlTransfer.runNow();
		}
		else {
			for (Date date : times) {
				sqlTransfer.runLater(date);
			}
		}
	}
    
	public static void logConfiguration(String filename) {
		logger.info("");
		logger.info(String.format("--- SQLtransfer (%s) started", VERSION_ID));
		logger.info("Using settings from: " + filename);
	}
	
	private Main() {
		// Prevent instantiation
	}
}
