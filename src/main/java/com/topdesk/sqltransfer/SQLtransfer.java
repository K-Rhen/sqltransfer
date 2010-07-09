package com.topdesk.sqltransfer;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.tiling.scheduling.Scheduler;
import org.tiling.scheduling.SchedulerTask;

public class SQLtransfer {

	private final boolean debug;
	private final boolean embedded;
	private final SQLtransferDefinition definition;
	private Scheduler scheduler = null;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLtransfer.class);
	
	public SQLtransfer (SQLtransferDefinition definition, boolean debug, boolean embedded) {
    	this.definition = definition;
		this.debug = debug;
		this.embedded = embedded;
    }
    
    public void runNow() {
    	executeTransfer();
    }
    
    public void runLater(Date time) {
    	if (scheduler == null) {
    		scheduler = new Scheduler();
    	}
        scheduler.schedule(new SchedulerTask() {
            public void run() {
            	executeTransfer();
            }
        }, new DailyIterator(time));
    }

    private void executeTransfer() {
		logger.info("Starting transfer to importtables...");
		long begin = System.currentTimeMillis();
		Map<String, SQLtransferConnection> connections = new HashMap<String, SQLtransferConnection>();
		SQLtransferWriter writer = null;
		try {
			writer = new SQLtransferWriter(definition);
			writer.prepareTargetDatabase();
			for (ConnectionData cd : definition.getSources()) {
				connections.put(cd.getName(), cd.connect());
			}
	  		for (ImportTable table : definition.getTables()) {
	  			writer.write(table,  new QueryExecuter(connections.get(table.getSource()), table));
	  		}
	  		NumberFormat nf = NumberFormat.getInstance();
            logger.info(String.format("Duration: %s s", nf.format((System.currentTimeMillis() - begin) / 1000)));
            logger.info("Done!");
	   	}
		catch (SQLtransferException e) {
			logger.info("");
			logger.info("");
			logger.info("");
			logger.info("Execution of a query failed, recieved this error from the database: ");
			logger.info("  " + e.getMessage());
			logger.info("");
			logger.info("");
			if (embedded) {
				String errorMessage = "Execution of a query failed, recieved this error from the database: " + e.getMessage();
				throw new RuntimeException(errorMessage, e);
			}
			if (debug) {
				logger.error("", e);
			}
		} 
		finally {
			for (SQLtransferConnection c : connections.values()) {
				try {
					c.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (writer != null) {
				writer.close();
			}
		}
	}
} 