package com.topdesk.sqltransfer;

public class LDAPAttribute {
	private final String name;
	private final int valuePosition;
	private final boolean expandRows;
	private final Type type;
	private Integer length = null;
	
	public enum Type {
		VARCHAR("varchar"),
		FILETIME("filetime"),
		BINARYSTRING("binarystring"),
		BINARY("binary");
		
		final String a;
	
		Type(String a) {
			this.a = a;
		}
		
		String getA() {
			return a;
		}
	}
	
	public LDAPAttribute(String name, int valuePostion, boolean expandRows, String type) {
		this.name = name;
		this.valuePosition = valuePostion;
		this.expandRows = expandRows;
		if ("varchar".equals(type)){
			this.type = Type.VARCHAR;
		}
		else {
			if ("binary".equals(type)){
				this.type = Type.BINARY;
			}
			else if ("binarystring".equals(type)){
				this.type = Type.BINARYSTRING;
			}
			else {
				this.type = Type.FILETIME;
			}
		}
	}

	public String getName() {
		return name;
	}

	public int getValuePosition() {
		return valuePosition;
	}

	public boolean isExpandRows() {
		return expandRows;
	}
	
	@Override
	public String toString() {
		return String.format("LDAPAttribute{name: %s, value: %s, expandrows: %s}", name, valuePosition, expandRows);
	}

	public Type getType() {
		return type;
	}
	
	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

}
