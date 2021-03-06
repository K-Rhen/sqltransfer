package com.topdesk.sqltransfer;

import java.sql.Timestamp;

public class TranslationType {
	private final int sqltype;
	private final String source;		
	private final String target;		
	private final boolean length;
	private boolean limits = false;
	private Timestamp minimumDate = null;
	private Timestamp maximumDate = null;
	
	public TranslationType(int sqltype, String source, String target, boolean length) {
		this.sqltype = sqltype;
		this.source = source;
		this.target = target;
		this.length = length;
	}

	public TranslationType(String sqltype, String source, String target, String length) {
		this.sqltype = Integer.parseInt(sqltype);
		this.source = source;
		this.target = target;
		this.length = Boolean.parseBoolean(length);
	}

	public boolean isLength() {
		return length;
	}

	public String getSource() {
		return source;
	}

	public int getSqltype() {
		return sqltype;
	}

	public String getTarget() {
		return target;
	}

	public Timestamp getMinimumDate() {
		return minimumDate;
	}

	public void setMinimumDate(java.util.Date minimumDate) {
		this.limits = true;
		this.minimumDate = new Timestamp(minimumDate.getTime());
	}

	public Timestamp getMaximumDate() {
		return maximumDate;
	}

	public void setMaximumDate(java.util.Date maximumDate) {
		this.limits = true;
		this.maximumDate = new Timestamp(maximumDate.getTime());
	}

	public boolean hasLimits() {
		return limits;
	}

	public Object getValue(Object value) {
		if (limits) {
			if (value instanceof java.util.Date) {
				if (minimumDate.compareTo((java.util.Date)value) > 0) {
					return null;
				}
				if (maximumDate.compareTo((java.util.Date)value) < 0) {
					return null;
				}
			}
		}
		return value;
	}
	
}
