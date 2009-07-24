package com.topdesk.sqltransfer;

public final class SQLColumnMetaData implements ColumnMetaData {
	private final String name;
	private final String typeName;
	private final int type;
	private final int displaySize;
	private final int index;
	private TranslationType translationType = null;

	public SQLColumnMetaData(String name, String typeName, int type, int displaySize, int index) {
		this.name = name;
		this.typeName = typeName;
		this.type = type;
		this.displaySize = displaySize;
		this.index = index;
	}
	
	public int getDisplaySize() {
		return displaySize;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Object getValue(Object value) {
		if (translationType == null) {
			return value;
		}
		return translationType.getValue(value);
	}

	public void setTranslationType(TranslationType translationType) {
		this.translationType = translationType;
	}

	public TranslationType getTranslationType() {
		return translationType;
	}
}