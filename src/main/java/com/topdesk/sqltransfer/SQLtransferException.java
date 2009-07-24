package com.topdesk.sqltransfer;

public class SQLtransferException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public SQLtransferException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public SQLtransferException(String arg0) {
		super(arg0);
	}

	public SQLtransferException(Throwable arg0) {
		super(arg0);
	}

}
