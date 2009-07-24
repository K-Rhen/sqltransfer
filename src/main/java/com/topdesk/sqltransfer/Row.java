package com.topdesk.sqltransfer;

import java.util.HashMap;
import java.util.Map;

public final class Row {

	private Map<String, Object> data;

	public Row(Map<String, Object> data) {
		this.data = new HashMap<String, Object>(data);
	}
	
	public Object getValue(ColumnMetaData column) {
		return data.get(column.getName());
	}
}
