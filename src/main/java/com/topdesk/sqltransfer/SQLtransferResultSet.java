package com.topdesk.sqltransfer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface SQLtransferResultSet extends Iterator<Map<String, Object>> {
	List<ColumnMetaData> getMetaData() throws SQLtransferException;
}
