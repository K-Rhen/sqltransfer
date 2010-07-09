package com.topdesk.sqltransfer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

public class DataPump {
	private static final int LOGS_PER_LINE = 10;
	private static final int LOG_EVERY_N_INSERTS = 1000;
	
	private final PreparedStatement statement;
	private final List<ColumnMetaData> metaData;
	private int numberOfRecordsInSource = 0;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataPump.class);
	
	public DataPump(SQLtransferSQLConnection connection, String insert, List<ColumnMetaData> metaData) throws SQLtransferException {
		statement = connection.prepareStatement(insert);
		this.metaData = metaData;
	}

	public void pump(SQLtransferResultSet data) throws SQLtransferException, SQLException {
		int numberOfInsertedRecords = 0;
   		try {
			while (data.hasNext()) {
	   			numberOfInsertedRecords += insertRecord(data.next());
	   			increaseReadCount();
	   		}
   		}
   		catch (Exception e) {
   			throw new SQLtransferException(e);
   		}
   		finally {
   			statement.close();
   		}
   		logger.info("");
   		logger.info("  Total inserts : " + numberOfInsertedRecords);
	}

	private void increaseReadCount() {
		numberOfRecordsInSource++;
		if (numberOfRecordsInSource % LOG_EVERY_N_INSERTS == 0) {
			logger.info(numberOfRecordsInSource + " ");
			if (numberOfRecordsInSource % (LOG_EVERY_N_INSERTS * LOGS_PER_LINE) == 0) {
				// TODO andere manier van loggen
				logger.info("");
			}
		}
	}

	private int insertRecord(Map<String, Object> data) throws SQLException {
		StringBuilder debugLog = new StringBuilder("  Starting dump of column names and values\n");
		try {
			for (ColumnMetaData column : metaData) {
				write(debugLog, column, data.get(column.getName().toLowerCase()));
			}
			statement.execute();
		}
		catch (SQLException e) {
			logger.info(debugLog.toString());
			throw e;
		}
		return statement.getUpdateCount();
	}

	private void write(StringBuilder debugLog, ColumnMetaData column, Object value) throws SQLException {
		int index = column.getIndex();
		debugLog.append("    Column " + column.getName() + " (" + column.getTypeName() + ") - Value " + value + "\n");
		if (value == null) {
			statement.setNull(index, column.getType());
		}
		else {
			statement.setObject(index, value);
		}
	}
}
