package com.topdesk.sqltransfer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

public class SQLtransferSQLResultSet implements SQLtransferResultSet {
	static Logger logger = Logger.getLogger(SQLtransferResultSet.class);
	
	@SuppressWarnings("unused")
	private final Object finalizerGuardian = new Object() {
		@Override
		protected void finalize() throws Throwable {
			try {
				internalClose(false);
			}
			catch (Exception e) {
				// Ignore
			}
		}
	};
	
	private final Statement statement;
	private final ResultSet rs;
	private final List<ColumnMetaData> metaData;

	boolean needsNext = true;
	boolean hasNext;

	public SQLtransferSQLResultSet(SQLtransferSQLConnection connection, ImportTable table) throws SQLtransferException {
		statement = connection.createStatement();
		try {
			long begin = System.currentTimeMillis();
			rs = statement.executeQuery(table.getQuery());
	  		NumberFormat nf = NumberFormat.getInstance();
			logger.info(String.format("    Duration : %s ms", nf.format(System.currentTimeMillis() - begin)));
			metaData = connection.createMetaData(rs.getMetaData());
		} 
		catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}

	public List<ColumnMetaData> getMetaData() throws SQLtransferException {
		return metaData;
	}

	private Object getValue(ColumnMetaData column) throws SQLtransferException {
		try {
			int index = column.getIndex();
			// Vieze fix voor Oracle, volgens MetaData is het opeens 2005 en omzetten lukt niet
			if (column.getType() == 2005) {
				return rs.getString(index);
			}
			return column.getValue(rs.getObject(index));
		} 
		catch (SQLException e) {
			throw new SQLtransferException(e);
		}
	}
	
	
	public boolean hasNext()  {
		if (needsNext) {
			needsNext = false;
			try {
				hasNext = rs.next();
			} 
			catch (SQLException e) {
				throw new RuntimeException(e);
			}		
		}
		return hasNext;
	}
	
	public Map<String, Object> next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		needsNext = true;
		
		Map<String, Object> data = new HashMap<String, Object>(); 
		for (ColumnMetaData column : metaData) {
			try {
				data.put(column.getName().toLowerCase(), getValue(column));
			} 
			catch (SQLtransferException e) {
				throw new RuntimeException(e);
			}		
		}
		
		if (!hasNext()) {
			internalClose(true);
		}
		return Collections.unmodifiableMap(data);
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void internalClose(boolean log) {
		try {
			rs.close();
		} 
		catch (SQLException e) {
			if (log) {
				e.printStackTrace();
			}
		}
		try {
			statement.close();
		} 
		catch (SQLException e) {
			if (log) {
				e.printStackTrace();
			}
		}
	}
}
