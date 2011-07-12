package com.topdesk.sqltransfer;

public final class LDAPColumnMetaData implements ColumnMetaData {
	private final String name;
	private final String nameLDAP;
	private final String typeName;
	private final int type;
	private final int displaySize;
	private final int index;
	private final int valuePosition;
	private final boolean expandRows;
	private TranslationType translationType = null;

	public LDAPColumnMetaData(String name, String nameLDAP, String typeName, int type, int displaySize, int index, int valuePosition, boolean expandRows) {
		this.name = name;
		this.nameLDAP = nameLDAP;
		this.typeName = typeName;
		this.type = type;
		this.displaySize = displaySize;
		this.index = index;
		this.valuePosition = valuePosition;
		this.expandRows = expandRows;
	}
	
	public int getDisplaySize() {
		return displaySize;
	}

	public String getName() {
		String result = name;
		if (result != null) {
			result = result.replace("-", "_");
			result = result.replace(" ", "_");
		}
		return result;
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

	public int getValuePosition() {
		return valuePosition;
	}

	public boolean isExpandRows() {
		return expandRows;
	}

	public String getNameLDAP() {
		return nameLDAP;
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