package com.topdesk.sqltransfer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

public class SQLtransferSQLConnection implements SQLtransferConnection {
	Connection connection;

	static Logger logger = Logger.getLogger(SQLtransferSQLConnection.class);

	public SQLtransferSQLConnection(Connection connection) {
		this.connection = connection;
	}

	public void close() throws SQLtransferException {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}

	public Statement createStatement() throws SQLtransferException {
		try {
			return connection.createStatement();
		} catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}

	public PreparedStatement prepareStatement(String query) throws SQLtransferException {
		try {
			return connection.prepareStatement(query);
		} catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}

	public SQLtransferResultSet query(ImportTable table) throws SQLtransferException {
		logger.info("");
		logger.info(String.format("  Executing query on source (%s) : ", table.getSource()));
		logger.info(String.format("    %s", removeTabsAndLines(table.getQuery())));
   			
		return new SQLtransferSQLResultSet(this, table);
	}
	
	public List<ColumnMetaData> createMetaData(ResultSetMetaData rsmd) throws SQLtransferException {
		try {
			List<ColumnMetaData> result = new ArrayList<ColumnMetaData>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				result.add(new SQLColumnMetaData(rsmd.getColumnName(i), rsmd.getColumnTypeName(i), rsmd.getColumnType(i), rsmd.getColumnDisplaySize(i), i));
			}
			return Collections.unmodifiableList(result);
		} 
		catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}	

	public static String removeTabsAndLines(String query) {
		return query.replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "");
	}
}
