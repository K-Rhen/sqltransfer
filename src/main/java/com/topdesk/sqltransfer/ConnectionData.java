package com.topdesk.sqltransfer;


public interface ConnectionData {
	
	SQLtransferConnection connect() throws SQLtransferException;

	String getName();

}
