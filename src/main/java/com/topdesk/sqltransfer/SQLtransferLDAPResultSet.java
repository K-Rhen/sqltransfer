package com.topdesk.sqltransfer;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

public class SQLtransferLDAPResultSet implements SQLtransferResultSet {
		
	private static final String BINARYSTRING = "BINARYSTRING";
	private final List<LDAPColumnMetaData> metaData;
	private NamingEnumeration<SearchResult> objectEnumeration;
	private NamingEnumeration<?> attributeEnumeration = null;
	private Iterator<String> contextIterator;

	boolean needsNext = true;
	boolean hasNext;
	
	private boolean expandRows;
	private final int pageSize;
	private LdapContext context;
	private Object object;
	private Attributes attributes;
	private byte[] cookie;
	private String searchBase;
	private String searchFilter;
	private SearchControls searchCtls;

	public SQLtransferLDAPResultSet(LDAPConnectionData connectionData, ImportTable table) throws SQLtransferException {
		this.metaData = getAttributes(table);
		this.expandRows = table.getLDAPTransfer().isExpandRows();
		pageSize = connectionData.getPagesize();
		
		Set<String> binaryAttributes = new HashSet<String>();
		for (LDAPColumnMetaData columnMetaData : metaData) {
			if (BINARYSTRING.equals(columnMetaData.getTypeName())) {
				binaryAttributes.add(columnMetaData.getNameLDAP());
			}
		}
		
		try {
			context = connectionData.createContext(binaryAttributes);
			searchCtls = new SearchControls();
			setPageSizeBegin();

			List<String> list = new ArrayList<String>();
			for (LDAPColumnMetaData column : this.metaData) {
				list.add(column.getNameLDAP());
			}
//			TODO attributen beperken
//			String returnedAtts[] = (String[]) list.toArray();
//			searchCtls.setReturningAttributes(returnedAtts);
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			searchFilter = table.getLDAPTransfer().getLDAPQuery();
			contextIterator = table.getLDAPTransfer().getContexts().iterator();
			searchBase = contextIterator.next();
			objectEnumeration = context.search(searchBase, searchFilter, searchCtls);
		} catch (NamingException e) {
			String errorCodeString = "error code 1";
			String myErrorMessage = "Probably the baseDN of the LDAP-server is incorrect: " + connectionData.getBasedn() + ".";
			throw createSQLtransferException(e, errorCodeString, myErrorMessage);
		}
	}

	private SQLtransferException createSQLtransferException(NamingException e, String errorCodeString, String myErrorMessage) {
		String clearErrorMessage = "Got this error from LDAP:\n" + e.getMessage() + "\n\n";
		if (e.getMessage() != null) {
			if ((e.getMessage().indexOf(errorCodeString) > 0)) {
				clearErrorMessage += myErrorMessage;
			}
		}
		else {
			clearErrorMessage += e.getRootCause().toString();
		}
		return new SQLtransferException(clearErrorMessage, e);
	}

	private List<LDAPColumnMetaData> getAttributes(ImportTable table) {
		List<LDAPColumnMetaData> metaData = new ArrayList<LDAPColumnMetaData>();
		int index = 0;
		for (LDAPAttribute attribute : table.getLDAPTransfer().getAttributes()) {
			index++;
			String name = attribute.getName();
			if (attribute.getValuePosition() > 0) {
				name += attribute.getValuePosition();
			}
			String typeName = "VARCHAR";
			int type = java.sql.Types.VARCHAR;
			int displaySize = 255;
			if (attribute.getLength() != null) {
				displaySize = attribute.getLength();
			}
			if (attribute.getType() == LDAPAttribute.Type.FILETIME) {
				typeName = "FILETIME";
				type = java.sql.Types.TIMESTAMP;
				displaySize = 0;
			}
			if (attribute.getType() == LDAPAttribute.Type.BINARYSTRING) {
				typeName = BINARYSTRING;
			}
			metaData.add(new LDAPColumnMetaData(name, attribute.getName(), typeName, type, displaySize, index, attribute.getValuePosition(), attribute.isExpandRows()));
		}
		return metaData;
	}

	public List<ColumnMetaData> getMetaData() throws SQLtransferException {
		List<ColumnMetaData> columnMetaData = new ArrayList<ColumnMetaData>();
		columnMetaData.addAll(metaData);
		return columnMetaData;
	}
	
	public boolean hasNext()  {
		if (needsNext) {
			needsNext = false;
			try {
				if (expandRows) {
					hasNextExpandRows();
				}
				else {
					hasNextNormal();
				}
			} catch (NamingException e) {
				throw new RuntimeException(e);
			}
		}
		return hasNext;
	}

	private void hasNextNormal() throws NamingException {
		hasNext = hasMoreObjects();
		if (hasNext) {
			SearchResult result = objectEnumeration.next();
			attributes = result.getAttributes();
			attributes.put("id", result.getNameInNamespace());
		}
	}

	private void hasNextExpandRows() throws NamingException {
		hasNext = (attributeEnumeration != null) && (attributeEnumeration.hasMore());
		if (hasNext) {
			object = attributeEnumeration.next();
		}
		if (!hasNext) {
			hasNext = hasMoreObjects();
			if (hasNext) {
				SearchResult result = objectEnumeration.next();
				attributes = result.getAttributes();
				attributes.put("id", result.getNameInNamespace());
				attributeEnumeration = null;
			}
		}
	}

	private boolean hasMoreObjects() throws NamingException {
		boolean hasMore = false;
		try {
			hasMore = objectEnumeration.hasMore();
		} catch (NamingException e) {
			String errorCodeString = "error code 4";
			String myErrorMessage = "Probably we are not allowed to request all objects from the LDAP-server, try setting the page size in the source-element.";
			throw new RuntimeException(createSQLtransferException(e, errorCodeString, myErrorMessage));
		}
		if (!hasMore) {
			if (pageSize > 0) {
				cookie = parseControls(context.getResponseControls());
				try {
					context.setRequestControls(new Control[] { new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (cookie != null & cookie.length != 0) {
					objectEnumeration = context.search(searchBase, searchFilter, searchCtls);
					hasMore = objectEnumeration.hasMore();
				}
			}
			if (!hasMore & contextIterator.hasNext()) {
				searchBase = contextIterator.next();
				setPageSizeBegin();
				try {
					objectEnumeration = context.search(searchBase, searchFilter, searchCtls);
				} catch (NamingException e) {
					String errorCodeString = "error code 32";
					String myErrorMessage = "Probably the context can not be found on the LDAP-server: " + searchBase + ".";
					throw new RuntimeException(createSQLtransferException(e, errorCodeString, myErrorMessage));
				}
				hasMore = objectEnumeration.hasMore();
			}
		}
		return hasMore;
	}

	private void setPageSizeBegin() throws NamingException {
		if (pageSize > 0) {
			cookie = null;
			Control[] ctls = null;
			try {
				ctls = new Control[]{ new PagedResultsControl(pageSize, false) };
			} catch (IOException e) {
				e.printStackTrace();
			}
			context.setRequestControls(ctls);
		}
	}

	private static byte[] parseControls(Control[] controls) {
		byte[] cookie = null;
		if (controls != null) {
			for (int i = 0; i < controls.length; i++) {
				if (controls[i] instanceof PagedResultsResponseControl) {
					PagedResultsResponseControl prrc = (PagedResultsResponseControl)controls[i];
					cookie = prrc.getCookie();
				}
			}
		}
		return (cookie == null) ? new byte[0] : cookie;
	}
	
	public Map<String, Object> next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		needsNext = true;

		Map<String, Object> data = new HashMap<String, Object>(); 
        try {
			for (LDAPColumnMetaData column : metaData) {
				Object object = null;
				if (!column.isExpandRows()) {
					BasicAttribute attribute = (BasicAttribute) attributes.get(column.getNameLDAP());
					if (attribute != null) {
						try {
							if (column.getValuePosition() > 0) {
								object = attribute.get(column.getValuePosition() - 1);
							}
							else {
								object = attribute.get(column.getValuePosition());
							}
						}
						catch (ArrayIndexOutOfBoundsException e) {
							// ldap-attribute can not contain the value at the requested position
						}
					}
				}
				else {
					if (attributeEnumeration == null) {
						BasicAttribute attribute = (BasicAttribute) attributes.get(column.getNameLDAP());
						if (attribute != null) {
							attributeEnumeration = attribute.getAll();
							if (attributeEnumeration.hasMore()) {
								object = attributeEnumeration.next();
							}
						}
					}
					else {
						object = this.object;
					}
				}
				if (object != null && "binarystring".equalsIgnoreCase(column.getTypeName())) {
					byte[] buffer = (byte[])object;
					int readBytes = buffer.length;
					StringBuffer hexData = new StringBuffer();
					for (int i=0; i < readBytes; i++) {
						hexData.append(padHexString(Integer.toHexString(0xff & buffer[i])));
					}
					object = hexData.toString();
				}
				if (object != null && "filetime".equalsIgnoreCase(column.getTypeName())) {
					long dateLong = Long.parseLong(object.toString());
			 
					// Filetime Epoch is JAN 01 1601
					// java date Epoch is January 1, 1970
					// so take the number and subtract java Epoch:
					long javaTime = dateLong - 0x19db1ded53e8000L;
			 
					// convert UNITS from (100 nano-seconds) to (milliseconds)
					javaTime /= 10000;
			 
					object = new Timestamp(javaTime);
				}
				data.put(column.getName().toLowerCase(), column.getValue(object));
			}
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
		
		if (!hasNext()) {
			internalClose(true);
		}
		return Collections.unmodifiableMap(data);
	}

	public String padHexString(String hexNum) {
		if (hexNum.length() < 2) {
			return "0" + hexNum;
		}
		return hexNum;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void internalClose(boolean log) {
		try {
			context.close();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
