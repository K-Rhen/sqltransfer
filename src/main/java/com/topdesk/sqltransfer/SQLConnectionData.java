package com.topdesk.sqltransfer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;

public class SQLConnectionData implements ConnectionData {
	
	private final String url;
	private final String user;
	private final String password;
	private final String name;
	private final boolean source;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLConnectionData.class);

	public SQLConnectionData(String url, String user, String password, String name, boolean source) {
		this.source = source;
		
		if (url == null) {
			throw new NullPointerException("url");
		}
		if (url.equals("")) {
			throw new IllegalArgumentException("url must be specified");
		}
		
		this.url = url;
		this.user = user;
		this.password = password;
		if (name == null) {
			name = "default";
		}
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}
	
	@Override
	public String toString() {
		return String.format("ConnectionData{url: %s, user: %s, password: %s}", url, user, password);
	}

	public String getName() {
		return name;
	}

	public SQLtransferConnection connect() throws SQLtransferException {
		SQLtransferSQLConnection connection;
		try {
			connection = new SQLtransferSQLConnection(createConnection());
		}
		catch (Exception e) {
			logger.info("Error creating connection to " + (source ? "source" : "target") + " database:");
			logger.info("  " + url);
			logger.info("");
			throw new SQLtransferException(e.getMessage());
		}
		return connection;
	}
	
	private Connection createConnection() throws SQLException {
		if (user == null) {
			return DriverManager.getConnection(url);
		}
		return DriverManager.getConnection(url, user, password);		
	}
}
