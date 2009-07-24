package com.topdesk.sqltransfer;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.log4j.Logger;


public class LDAPConnectionData implements ConnectionData {
	
	private final String server;
	private final int port;
	private final String basedn;
	private final String user;
	private final String password;
	private final String name;
	private final boolean secure;
	private final int pagesize;
	private final boolean source;
	private final String referral;

	static Logger logger = Logger.getLogger(LDAPConnectionData.class);

	public String getReferral() {
		return referral;
	}

	public LDAPConnectionData(String server, int port, String basedn, String user, String password, String name, boolean secure, int pagesize, boolean source, String referral) {
		if (server == null) {
			throw new NullPointerException("server");
		}
		if (server.equals("")) {
			throw new IllegalArgumentException("server must be specified");
		}
		this.server = server;
		if (port == 0) {
			port = 389;
		}
		this.port = port;
		if (basedn == null) {
			throw new NullPointerException("basedn");
		}
		if (basedn.equals("")) {
			throw new IllegalArgumentException("basedn must be specified");
		}
		this.basedn = basedn;
		this.user = user;
		this.password = password;
		if (name == null) {
			name = "default";
		}
		this.name = name;
		// TODO controle poort 389 <> 636?
		this.secure = secure;
		this.pagesize = pagesize;
		this.source = source;
		this.referral = referral;
	}

	public String getPassword() {
		return password;
	}

	public String getUser() {
		return user;
	}
	
//	@Override
//	public String toString() {
//		return String.format("ConnectionData{url: %s, user: %s, password: %s}", url, user, password);
//	}

	public String getName() {
		return name;
	}

	public SQLtransferConnection connect() throws SQLtransferException {
		SQLtransferLDAPConnection connection;
		try {
			connection = new SQLtransferLDAPConnection(this);
			LdapContext ctx = createContext(Collections.<String>emptySet());
			ctx.close();
		}
		catch (Exception e) {
			logger.info("Error creating connection to " + (source ? "source" : "target") + " database: " + name);
			logger.info("");
			logger.info("");
			String clearErrorMessage = "Got this error from LDAP: " + e.getMessage();
			logger.info(clearErrorMessage);
			logger.info("");
			logger.info(e);
			if (e.getMessage().indexOf("error code 49") > 0) {
				clearErrorMessage += " Probably the username or password is incorrect.";
			}
			throw new SQLtransferException(clearErrorMessage, e);
		}
		return connection;
	}

	public LdapContext createContext(Set<String> binaryAttributes) throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		
		env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
 
//		env.put(Context.SECURITY_PROTOCOL, "ssl");
		
		env.put(Context.SECURITY_AUTHENTICATION,"simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, password);
		
		env.put(Context.REFERRAL, referral);
				
		if (binaryAttributes.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (String binaryAttribute : binaryAttributes) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(binaryAttribute);
			}
			env.put("java.naming.ldap.attributes.binary", sb.toString());
		}
		
		env.put(Context.PROVIDER_URL, String.format("ldap://%s:%s/%s", server, port, basedn));
		LdapContext ctx = new InitialLdapContext(env, null);
		return ctx;
	}

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public String getBasedn() {
		return basedn;
	}

	public boolean isSecure() {
		return secure;
	}

	public int getPagesize() {
		return pagesize;
	}

	public boolean isSource() {
		return source;
	}
}
