package com.topdesk.sqltransfer;

public interface ColumnMetaData {

	int getDisplaySize();

	String getName();

	int getType();
	
	String getTypeName();
	
	int getIndex();
	
	void setTranslationType(TranslationType translationType);
	
	TranslationType getTranslationType();
	
	Object getValue(Object value);
}