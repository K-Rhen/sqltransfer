package com.topdesk.sqltransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportTable {
	
	private final String query;
	private final LDAPTransfer lDAPTransfer;
	
	private final String tableName;
	private final boolean append;
	private final List<Query> preQueries;
	private final List<Query> postQueries;
	private final String source;

	public ImportTable(String query, String tableName, boolean append, List<Query> preQueries, List<Query> postQueries, String source) {

		if (query == null) {
			throw new NullPointerException("query");
		}
		if (query.length() == 0) {
			throw new IllegalArgumentException("query must be provided");
		}
		
		if (tableName == null) {
			throw new NullPointerException("tableName");
		}
		if (tableName.length() == 0) {
			throw new IllegalArgumentException("tableName must be provided");
		}
		
		this.query = query;
		this.lDAPTransfer = null; 
		this.tableName = tableName;
		this.append = append;
		this.preQueries =  Collections.unmodifiableList(preQueries == null ? Collections.<Query>emptyList() : new ArrayList<Query>(preQueries));
		this.postQueries = Collections.unmodifiableList(postQueries == null ? Collections.<Query>emptyList() : new ArrayList<Query>(postQueries));
		if (source == null) {
			source = "default";
		}
		this.source = source;
	}

	public ImportTable(LDAPTransfer lDAPTransfer, String tableName, boolean append, List<Query> preQueries, List<Query> postQueries, String source) {

		if (lDAPTransfer == null) {
			throw new NullPointerException("lDAPTransfer");
		}
		
		if (tableName == null) {
			throw new NullPointerException("tableName");
		}
		if (tableName.length() == 0) {
			throw new IllegalArgumentException("tableName must be provided");
		}
		
		this.query = null;
		this.lDAPTransfer = lDAPTransfer;
		this.tableName = tableName;
		this.append = append;
		this.preQueries =  Collections.unmodifiableList(preQueries == null ? Collections.<Query>emptyList() : new ArrayList<Query>(preQueries));
		this.postQueries = Collections.unmodifiableList(postQueries == null ? Collections.<Query>emptyList() : new ArrayList<Query>(postQueries));
		if (source == null) {
			source = "default";
		}
		this.source = source;
	}

	public boolean isAppend() {
		return append;
	}

	public List<Query> getPreQueries() {
		return preQueries;
	}

	public List<Query> getPostQueries() {
		return postQueries;
	}

	public String getQuery() {
		return query;
	}

	public String getTableName() {
		return tableName;
	}
	
	@Override
	public String toString() {
		return String.format("ImportTable{query: %s; tableName: %s; append: %s; postQueries: %s;}", query, tableName, Boolean.toString(append), postQueries.toString());
	}

	public String getSource() {
		return source;
	}

	public LDAPTransfer getLDAPTransfer() {
		return lDAPTransfer;
	}
}
