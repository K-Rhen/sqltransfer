package com.topdesk.sqltransfer;

import java.util.ArrayList;
import java.util.List;

public final class Query {
	private final String statement;
	private final List<String> errors;

	public Query (String statement, List<String> errors) {
		this.statement = statement;
		if (errors == null) {
			errors = new ArrayList<String>();
		}
		if (errors.size() == 0) {
			errors.add("No error suplied.");
		}
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(statement);
		for (String s : errors) {
			sb.append("\n" + s);
		}
		return sb.toString();
	}

	public String getStatement() {
		return statement;
	}
}
