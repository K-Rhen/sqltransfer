package com.topdesk.sqltransfer;

public final class QueryExecuter {
	private final ImportTable table;
	private final SQLtransferConnection connection;

	public QueryExecuter(SQLtransferConnection connection, ImportTable table) {
		this.table = table;
		this.connection = connection;
	}
	
	public SQLtransferResultSet execute() throws SQLtransferException {
		return connection.query(table);
	}
}
