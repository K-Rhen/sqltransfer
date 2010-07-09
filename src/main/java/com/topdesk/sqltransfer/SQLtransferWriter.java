package com.topdesk.sqltransfer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import org.slf4j.LoggerFactory;

public class SQLtransferWriter {
	private final SQLtransferDefinition definition;
	private final SQLtransferSQLConnection connection;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLtransferWriter.class);

	public SQLtransferWriter(SQLtransferDefinition definition) throws SQLtransferException {
		this.definition = definition;
		connection = (SQLtransferSQLConnection)definition.getTarget().connect();
	}

	public void prepareTargetDatabase() throws SQLtransferException {
		executeQueries(definition.getPrepareTargetDatabase(), "Prepare target database");	
	}

	public void write(ImportTable table, QueryExecuter queryExecuter) throws SQLtransferException {
		String tableName = table.getTableName();
	   	
   		try {
   			logger.info("");
   			logger.info(" - Working on " + tableName);
   			
   			executeQueries(table.getPreQueries(), "Prequery");	
   			
   			SQLtransferResultSet rs = queryExecuter.execute();
   			
   			List<ColumnMetaData> metaDataSource = rs.getMetaData();
   			if (!checkColumnTypes(definition.getTypes(), metaDataSource)) {
   				throw new RuntimeException("Unknown data types found (must be case-sensitive). See output for details.");
   			}
   			
   			logger.info("  Prepared statements :");
   			final String insert = createInsertStatement(metaDataSource, tableName);
   			logger.info("    " + insert);
   			
   			createTableWhenNecessary(table, metaDataSource);
   			
   			logger.info("  Preparing insert into " + tableName);
   			List<ColumnMetaData> metaDataTarget = createMetaDataList(connection, tableName, metaDataSource);

   			DataPump dataPump = new DataPump(connection, insert, metaDataTarget);
			logger.info("  Inserting into " + tableName);
   			dataPump.pump(rs);
   			
   			executeQueries(table.getPostQueries(), "Postquery");	
	   		logger.info(" ---");
	   		logger.info("");
   		}
   		catch (SQLException e) {
   			throw new SQLtransferException(e);
   		}
	}

	private void createTable(String tableName, final String create) throws SQLtransferException, SQLException {
		String query = definition.getDropTableSyntax();
		try {
			executeUpdate(String.format(query, tableName), "Deleting existing table  " + tableName + ".");
		}
		catch (SQLException s) {
			List<String> dropErrorMessages = new ArrayList<String>();
			for (String dropErrorMessage : definition.getDropErrorMessages()) {
				try {
					dropErrorMessages.add(String.format(dropErrorMessage, tableName));
				} catch (MissingFormatArgumentException e) {
					logger.info("");
					logger.info("Your supplied drop-table-message: ");
					logger.info(dropErrorMessage);
					logger.info("");
					logger.info("The database gave this error message: ");
					logger.info(e.getMessage());
					throw e;
				}				
			}
			isKnownErrorMessage("dropping table " + tableName, s, dropErrorMessages);
		}
		executeUpdate(create, "Creating table " + tableName);
	}
	

	private void executeQueries(List<Query> queries, String queryType) throws SQLtransferException {
   		for (Query query : queries) {
  			String statement = query.getStatement();
  			logger.info("");
			logger.info(String.format("  %s:", queryType));
  			try {
  				executeUpdate(statement);
  			}
  			catch (SQLException s) {
  				isKnownErrorMessage("executing the query", s, query.getErrors());
  			}
  		}
	}

	private void createTableWhenNecessary(ImportTable table, List<ColumnMetaData> metaData) throws SQLException, SQLtransferException {
		String tableName = table.getTableName();
		if (!table.isAppend()) {
	   		createTable(tableName, createCreateTableStatement(metaData, tableName));
		}
	}

	private void isKnownErrorMessage(String queryDescription, SQLException s, List<String> errorMessages) throws SQLtransferException {
		boolean knownError = false;
		for (String dropTableErrorString : errorMessages) {
			if (dropTableErrorString.equals(s.getMessage())) {
				knownError = true;
			}
		}
		if (!knownError) {
			logger.info("");
			logger.info(String.format("I got this error-message from the database while %s: ", queryDescription));
			logger.info("  " + s.getMessage());
			logger.info("");
			logger.info("I only ignore these error-messages: ");
			for (String dropTableErrorString : errorMessages) {
				logger.info("  " + dropTableErrorString);
			}
			throw new SQLtransferException(s);
		}
	}

	private void executeUpdate(String sql) throws SQLException, SQLtransferException {
		executeUpdate(sql, null);
	}

	private void executeUpdate(String sql, String description) throws SQLException, SQLtransferException {
		long begin = System.currentTimeMillis();
		if (description != null) {
			logger.info("    " + description);
		}
		logger.info("    Executing: " + SQLtransferSQLConnection.removeTabsAndLines(sql));
		Statement statement = connection.createStatement();
		try {
			int result = statement.executeUpdate(sql + ";");
	  		NumberFormat nf = NumberFormat.getInstance();
			logger.info(String.format("    Duration : %s ms", nf.format(System.currentTimeMillis() - begin)));
			logger.info(String.format("    Result : %s", result));
		}
		finally {
			statement.close();
		}
	}

	private static List<ColumnMetaData> createMetaDataList(SQLtransferSQLConnection connection, String tableName, List<ColumnMetaData> metaData) throws SQLException, SQLtransferException {
	   	StringBuilder selectStatement = new StringBuilder("SELECT ");

	   	boolean first = true;
   		for (ColumnMetaData data : metaData) {
   			if (first) {
   				first = false;
   			}
   			else {
   				selectStatement.append(", ");
   			}
   			selectStatement.append(data.getName());
   		}		
   		selectStatement.append(" FROM ").append(tableName).append(" WHERE 1 = 0");
   		
		Statement statement = connection.createStatement();
		try {
			ResultSet rs = statement.executeQuery(selectStatement.toString());
			try {
				return connection.createMetaData(rs.getMetaData());
			}
			finally {
				rs.close();
			}
		}
		finally {
			statement.close();
		}
	}


	private String createCreateTableStatement(List<ColumnMetaData> metaData, String tableName) {
		Map<String, TranslationType> types = definition.getTypes();
	   	StringBuilder createTableStatement = new StringBuilder("CREATE TABLE " + tableName + " (");

   		for (ColumnMetaData data : metaData) {
			final String columnTypeName = data.getTypeName();
			final String columnName = data.getName();
			final int columnType = data.getType();
			final TranslationType translationType = types.get(columnTypeName);
			data.setTranslationType(translationType);

			createTableStatement.append(columnName);
			createTableStatement.append(" " + translationType.getTarget());
			if (translationType.isLength() && 
					(
							(columnType == java.sql.Types.CHAR) || 
							(columnType == java.sql.Types.VARCHAR) || 
							(columnType == -15) || 
							(columnType == -9)
					)
				) {
				createTableStatement.append("(" + data.getDisplaySize() + ")");
			}
			// TODO decimals
			createTableStatement.append(", ");
   		}		

   		trimExcessComma(createTableStatement);
   		createTableStatement.append(")");
   		
		return createTableStatement.toString();
	}

	private String createInsertStatement(List<ColumnMetaData> metaData, String tableName) {
	   	StringBuilder insertStatement = new StringBuilder("INSERT INTO " + tableName + " (");
	   	StringBuilder values = new StringBuilder(") VALUES (");
	   	boolean first = true;
   		for (ColumnMetaData data : metaData) {
   			if (!first) {
   				insertStatement.append(", ");
   				values.append(", ");
   			}
   			else {
   				first = false;
   			}
			insertStatement.append(data.getName());
			values.append("?");
   		}		
   		values.append(");");
		return insertStatement.append(values).toString();
	}
	
	private static void trimExcessComma(StringBuilder builder) {
		builder.setLength(builder.length() - 2);
	}	
	
	private static boolean checkColumnTypes(Map<String, TranslationType> types, List<ColumnMetaData> metaData) {
		boolean result = true;
   		for (ColumnMetaData data : metaData) {
			final String columnTypeName = data.getTypeName();
			final TranslationType translationType = types.get(columnTypeName);
			
			if (translationType == null) {
				logger.info(String.format("Unknown data type: column '%s' type '%s' [%d].", data.getName(), columnTypeName, data.getType()));
				result = false;
			}
   		}
   		return result;
	}	
	
	public void close() {
		try {
			connection.close();
		}
		catch (SQLtransferException e) {
			e.printStackTrace();
		}
	}
}