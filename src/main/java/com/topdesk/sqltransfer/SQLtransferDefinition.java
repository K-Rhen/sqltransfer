package com.topdesk.sqltransfer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class SQLtransferDefinition {
	
	private final List<ConnectionData> sources;
	private final ConnectionData target;
	private final List<Query> prepareTargetDatabase;
	private final List<ImportTable> tables;
	private final Map<String, TranslationType> types;
	private final List<String> dropErrorMessages;
	private final String dropTableSyntax;

	public SQLtransferDefinition(List<ConnectionData> sources, ConnectionData target, List<Query> prepareTargetDatabase, List<ImportTable> tables, Map<String, TranslationType> types, List<String> dropErrorMessages, String dropTableSyntax) {
		this.sources = Collections.unmodifiableList(new ArrayList<ConnectionData>(sources));
		this.target = target;
		this.prepareTargetDatabase = prepareTargetDatabase;
		this.tables = Collections.unmodifiableList(new ArrayList<ImportTable>(tables));
		this.types = Collections.unmodifiableMap(new HashMap<String, TranslationType>(types));
		this.dropErrorMessages = Collections.unmodifiableList(new ArrayList<String>(dropErrorMessages));
		this.dropTableSyntax = dropTableSyntax;
	}

	public List<String> getDropErrorMessages() {
		return dropErrorMessages;
	}

	public List<ConnectionData> getSources() {
		return sources;
	}

	public List<ImportTable> getTables() {
		return tables;
	}

	public ConnectionData getTarget() {
		return target;
	}

	public Map<String, TranslationType> getTypes() {
		return types;
	}
	
	@Override
	public String toString() {
		return String.format("PrepareImportDefinition{source: %s; target: %s; tables: %s; types: %s; dropErrorMessage: %s;}", sources, target, tables, types, dropErrorMessages);
	}

	public String getDropTableSyntax() {
		return dropTableSyntax;
	}

	public List<Query> getPrepareTargetDatabase() {
		return prepareTargetDatabase;
	}
}
