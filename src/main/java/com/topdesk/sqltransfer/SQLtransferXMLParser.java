package com.topdesk.sqltransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class SQLtransferXMLParser {
	private static final SimpleDateFormat JUST_THE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final Node sqltransfer;

	static Logger logger = Logger.getLogger(SQLtransferXMLParser.class);
	
	class Node {
		private final org.w3c.dom.Node node;
		
		public Node (org.w3c.dom.Node node) {
			this.node = node;
		}
		
		public Node getNode(String name) {
			if (name == null) {
				throw new IllegalArgumentException("Name can not be null!");
			}
			Node result = null;
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node node = nodes.item(i);
				if (name.equals(node.getNodeName())) {
					if (result == null) {
						result = new Node(node);
					}
					else {
						throw new RuntimeException(String.format("More nodes with the name %s found.", name));
					}
				}
			}
			if (result == null) {
				throw new RuntimeException(String.format("No node found with the name %s.", name));
			}
			return result;
		}
		
		public boolean hasNode(String name) {
			if (name == null) {
				throw new IllegalArgumentException("Name can not be null!");
			}
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node node = nodes.item(i);
				if (name.equals(node.getNodeName())) {
					return true;
				}
			}
			return false;
		}
		
		public List<Node> getNodes(String name) {
			if (name == null) {
				throw new IllegalArgumentException("Name can not be null!");
			}
			List<Node> result = new ArrayList<Node>();
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node node = nodes.item(i);
				if (name.equals(node.getNodeName())) {
					result.add(new Node(node));
				}
			}
			return result;
		}
		
		public String getAttribute(String attribute) {
			NamedNodeMap attributes = node.getAttributes();
			org.w3c.dom.Node attributeNode = attributes.getNamedItem(attribute);
			if (attributeNode != null) {
				return attributeNode.getNodeValue();
			}
			return null;
		}

		public String getTextContent() {
			NodeList nodes = node.getChildNodes();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node node1 = nodes.item(i);
				if (node1.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
					sb.append(node1.getTextContent().trim());
				}
			}
			return sb.toString();
		}

		public String getChildText(String node) {
			return getNode(node).getTextContent();
		}
	}

	public SQLtransferXMLParser(File xmlFile, final File xsdFile) throws IOException {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
	        Schema schema = schemaFactory.newSchema(xsdFile);			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setSchema(schema);
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new ErrorHandler() {
				public void fatalError(SAXParseException exception) throws SAXException {
				}

				public void error(SAXParseException e) throws SAXParseException {
					logger.error("Error at " + e.getLineNumber() + " line.");
					logger.error(e.getMessage());
					
					throw e;
				}

				public void warning(SAXParseException err) throws SAXParseException {
					logger.error(err.getMessage());
				}
			});
			InputStream xmlStream = new FileInputStream(xmlFile);
			
			Document domtree = builder.parse(xmlStream);
			sqltransfer = new Node(domtree.getDocumentElement());
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
		catch (SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public SQLtransferDefinition createPrepareImportDefinition() throws ClassNotFoundException {
		try {
			Map<String, TranslationType> types = gatherTypes(sqltransfer);
			List<String> dropErrorMessages = getDropErrorMessages(sqltransfer); 
			List<ConnectionData> sources = new ArrayList<ConnectionData>();
			if (sqltransfer.hasNode("source")) {
				sources.add(createConnectionData(sqltransfer.getNode("source"), true));
			}
			else {
				for (Node source : sqltransfer.getNode("sources").getNodes("source")) {
					for (ConnectionData cd : sources) {
						String name = null;
						if (source.hasNode("name")) {
							name = source.getChildText("name");
						}
						if (name == null) {
							logger.info("WARNING: With multiple sources it is recommended to name all the sources!");
						}
						if (name != null && name.equalsIgnoreCase(cd.getName())) {
							throw new RuntimeException(String.format("Another source with the name '%s' already exists: '%s'.", name, cd.getName()));
						}
					}
					sources.add(createConnectionData(source, true));
				}
			}
			ConnectionData target = createConnectionData(sqltransfer.getNode("target"), false);
			List<ImportTable> tables = gatherImportTables(sqltransfer, sources);
			String dropTableSyntax = null;
			if (sqltransfer.getNode("target").hasNode("sql")) {
				dropTableSyntax = sqltransfer.getNode("target").getNode("sql").getChildText("drop-table-syntax");
			}
			if (dropTableSyntax == null) {
				dropTableSyntax = "drop table %s";
			}
			List<Query> prepareTargetDatabase = new ArrayList<Query>();
			if (sqltransfer.getNode("target").hasNode("prepare_database")) {
				prepareTargetDatabase = gatherQueries(sqltransfer.getNode("target"), "prepare_database");
			}
			return new SQLtransferDefinition(sources, target, prepareTargetDatabase, tables, types, dropErrorMessages, dropTableSyntax);
		}
		catch(RuntimeException r) {
			logger.info("");
			logger.info("");
			logger.info("ERROR: " + r.getMessage());
			logger.info("");
			throw r;
		} 
	}

	public List<Date> gatherTimes() throws ParseException {
		List<Date> times = new ArrayList<Date>();
		String today = JUST_THE_DATE_FORMAT.format(Calendar.getInstance().getTime());

		if (sqltransfer.hasNode("execution")) {
			Node execution = sqltransfer.getNode("execution");
			Node daily = execution.getNode("daily");
			for (Node time : daily.getNodes("time")) {
				times.add(DATE_AND_TIME_FORMAT.parse(today + " " + time.getTextContent()));
				logger.info("Prepare importtables scheduled at " + time.getTextContent());
			}
		}
		return times;
	}
 
	private static Map<String, TranslationType> gatherTypes(Node rootElement) {
		Map<String, TranslationType> types = new HashMap<String, TranslationType>();
		Node typetranslation = rootElement.getNode("typetranslation");
		for (Node type : typetranslation.getNodes("type")) {
			TranslationType tt = new TranslationType(
				type.getAttribute("sqltype"), 
				type.getAttribute("source"), 
				type.getAttribute("target"), 
				type.getAttribute("length")
			);
			
			try {
				if (type.getAttribute("mindate") != null) {
					tt.setMinimumDate(JUST_THE_DATE_FORMAT.parse(type.getAttribute("mindate")));
				}
				if (type.getAttribute("maxdate") != null) {
					tt.setMaximumDate(JUST_THE_DATE_FORMAT.parse(type.getAttribute("maxdate")));
				}
			} catch (ParseException e) {
				throw new RuntimeException("Date supplied in wrong format, should be 'yyyy-MM-dd'.", e);
			}
			types.put(tt.getSource(), tt);
		}
		return types;
	}

	private static List<ImportTable> gatherImportTables(Node rootElement, List<ConnectionData> sources) {
		List<ImportTable> tables = new ArrayList<ImportTable>();

		for (Node importTable : rootElement.getNode("importtables").getNodes("importtable")) {
  			String sourceName;
  			if (importTable.hasNode("source_name")) {
  				sourceName = importTable.getChildText("source_name");
  			}
  			else {
  				sourceName = "default";
  			}
  			boolean sourceFound = false;
			for (ConnectionData cd : sources) {
				if (sourceName.equalsIgnoreCase(cd.getName())) {
					sourceName = cd.getName();
					sourceFound = true;
				}
			}
			if (!sourceFound) {
				throw new RuntimeException(String.format("No source found with the name '%s'.", sourceName));
			}
  			
  			List<Query> preQueries = gatherQueries(importTable, "prequeries");
  			List<Query> postQueries = gatherQueries(importTable, "postqueries");
  			
			boolean hasQuery = importTable.hasNode("query");
			boolean hasLdaptransfer = importTable.hasNode("ldaptransfer");
  			if (!hasQuery && !hasLdaptransfer) {
  				throw new IllegalArgumentException("Element has no query or ldaptransfer node");
  			}
  			if (hasQuery && hasLdaptransfer) {
  				throw new IllegalArgumentException("Element has a query and a ldaptransfer node, but just one is allowed");
  			}

  			if (hasQuery) {
  				Node query = importTable.getNode("query");
  				tables.add(new ImportTable(
  						query.getTextContent(), 
  						importTable.getChildText("tablename"), 
  						Boolean.parseBoolean(importTable.getChildText("append")), 
  						preQueries, 
  						postQueries,
  						sourceName
  				));
  			}
  			else {
  				Node ldaptransfer = importTable.getNode("ldaptransfer");
  				tables.add(new ImportTable(
  						gatherLDAPTransfer(ldaptransfer), 
  						importTable.getChildText("tablename"), 
  						Boolean.parseBoolean(importTable.getChildText("append")), 
  						preQueries, 
  						postQueries,
  						sourceName
  				));
  			}
  		}
		return tables;
	}
	
	private static LDAPTransfer gatherLDAPTransfer(Node ldaptransfer) {
		String lDAPQuery = ldaptransfer.getChildText("ldapquery");
		if (lDAPQuery == null) {
			lDAPQuery = "objectClass=*";
		}
		List<LDAPAttribute> attributes = new ArrayList<LDAPAttribute>();
		for (Node attributeElement : ldaptransfer.getNode("attributes").getNodes("attribute")) {
			String name = attributeElement.getAttribute("name");
			int value = 0;
			if (attributeElement.getAttribute("value") != null) {
				value = Integer.parseInt(attributeElement.getAttribute("value"));
			}
			boolean expandRows = false;
			if (attributeElement.getAttribute("expand-rows") != null) {
				expandRows = Boolean.parseBoolean(attributeElement.getAttribute("expand-rows"));
			}
			String type = "varchar";
			if (attributeElement.getAttribute("type") != null) {
				type = attributeElement.getAttribute("type");
			}
			LDAPAttribute lDAPAttribute = new LDAPAttribute(name, value, expandRows, type);
			if (attributeElement.getAttribute("length") != null) {
				lDAPAttribute.setLength(Integer.parseInt(attributeElement.getAttribute("length")));
			}
			attributes.add(lDAPAttribute);
		}
		List<String> contexts = new ArrayList<String>();
		if (ldaptransfer.hasNode("contexts")) {
			for (Node contextElement : ldaptransfer.getNode("contexts").getNodes("context")) {
				contexts.add(contextElement.getTextContent());
			}
		}
		if (contexts.isEmpty()) {
			contexts.add("");
		}
		return new LDAPTransfer(lDAPQuery, attributes, contexts);
	}

	private static List<Query> gatherQueries(Node rootElement, String xmlElement) {
		List<Query> queries = new ArrayList<Query>();
		if (rootElement.hasNode(xmlElement)) {
			Node queriesElement = rootElement.getNode(xmlElement);
			for (Node queryElement : queriesElement.getNodes("query")) {
				List<String> errors = new ArrayList<String>();
				for (Node errorElement : queryElement.getNodes("error")) {
					errors.add(errorElement.getTextContent());
				}
				Query query = new Query(queryElement.getTextContent(), errors);
				queries.add(query);
			}
		}
		return queries;
	}
	
	private static ConnectionData createConnectionData (Node database, boolean source) throws ClassNotFoundException {
		boolean isJdbc = database.hasNode("jdbc");
		boolean isLdap = database.hasNode("ldap");
		if (!isJdbc && !isLdap) {
			throw new IllegalArgumentException("Element has no jdbc or ldap node");
		}
		if (isJdbc && isLdap) {
			throw new IllegalArgumentException("Element has a jdbc and a ldap node, but just one is allowed");
		}
		
		if (isJdbc) {
			Node jdbc = database.getNode("jdbc");
			Class.forName(jdbc.getChildText("driver"));
			
			String name = null;
			if (database.hasNode("name")) {
				name = database.getChildText("name");
			}
			String url = jdbc.getChildText("url");
			return new SQLConnectionData(url, jdbc.getChildText("user"), jdbc.getChildText("password"), name, source);
		}
		else {
			Node ldap = database.getNode("ldap");

			int pageSize = 0;
			if (ldap.hasNode("pagesize")) {
				pageSize = Integer.parseInt(ldap.getChildText("pagesize"));
			}
			String referral;
			if (ldap.hasNode("referral")) {
				referral = ldap.getChildText("referral");
			}
			else {
				referral = "follow";
			}
			boolean secure = false;
			if (ldap.hasNode("secure")) {
				secure = Boolean.parseBoolean(ldap.getChildText("secure"));
			}
			String name = null;
			if (database.hasNode("name")) {
				name = database.getChildText("name");
			}
			return new LDAPConnectionData(ldap.getChildText("server"), Integer.parseInt(ldap.getChildText("port")), ldap.getChildText("basedn"), ldap.getChildText("user"), 
					ldap.getChildText("password"), name, secure, pageSize, source, referral);
		}
	}

	private static List<String> getDropErrorMessages(Node rootElement) {
		Node dropTableErrors = rootElement.getNode("target").getNode("errors");
		List<String> l = new ArrayList<String>();
		for (Node errorElement : dropTableErrors.getNodes("drop-table")) {
			StringBuilder sb = new StringBuilder();
			sb.append(errorElement.getChildText("head"));
			if (errorElement.getNode("tablename") != null) {
				sb.append("%s");
			}
			sb.append(errorElement.getChildText("tail"));
			l.add(sb.toString());
		}
		for (Node errorElement : dropTableErrors.getNodes("drop-table-message")) {
			l.add(errorElement.getTextContent());
		}
		return l;
	}
	
}