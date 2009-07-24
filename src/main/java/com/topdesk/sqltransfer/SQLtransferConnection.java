package com.topdesk.sqltransfer;

public interface SQLtransferConnection {

	void close() throws SQLtransferException;

	SQLtransferResultSet query(ImportTable table) throws SQLtransferException;

}
