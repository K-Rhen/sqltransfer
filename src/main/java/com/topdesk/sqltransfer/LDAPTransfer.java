package com.topdesk.sqltransfer;

import java.util.Collections;
import java.util.List;

public class LDAPTransfer {
	private final String lDAPQuery;
	private final List<LDAPAttribute> attributes;
	private final List<String> contexts;
	private final boolean expandRows;

	public LDAPTransfer(String lDAPQuery, List<LDAPAttribute> attributes, List<String> contexts) {
		int attributeExpandRows = 0;
		boolean expandRows = false;
		for (LDAPAttribute lDAPAttribute : attributes) {
			if (lDAPAttribute.isExpandRows()) {
				attributeExpandRows++;
				expandRows = true;
			}
		}
		if (attributeExpandRows > 1) {
			throw new IllegalArgumentException("An ldaptransfer can have only one attribute with expand-rows enabled!");
		}
		this.expandRows = expandRows;
		this.lDAPQuery = lDAPQuery;
		this.attributes = Collections.unmodifiableList(attributes);
		this.contexts = Collections.unmodifiableList(contexts);
	}

	public String getLDAPQuery() {
		return lDAPQuery;
	}

	public List<LDAPAttribute> getAttributes() {
		return attributes;
	}

	public boolean isExpandRows() {
		return expandRows;
	}

	public List<String> getContexts() {
		return contexts;
	}

}
