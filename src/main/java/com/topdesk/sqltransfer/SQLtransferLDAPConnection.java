package com.topdesk.sqltransfer;

import org.slf4j.LoggerFactory;

public class SQLtransferLDAPConnection implements SQLtransferConnection {
	private final LDAPConnectionData connectionData;
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLtransferLDAPConnection.class);
	
	public SQLtransferLDAPConnection(LDAPConnectionData connection) {
		this.connectionData = connection;
	}

	public void close() throws SQLtransferException {
		// Connection already closed in query
	}

	public SQLtransferResultSet query(ImportTable table) throws SQLtransferException {
		logger.info("");
		logger.info(String.format("  Getting objects from source (%s) : ", table.getSource()));
		
		return new SQLtransferLDAPResultSet(connectionData, table);
	}

}
