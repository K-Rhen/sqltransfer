package com.topdesk.sqltransfer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SimpleTest extends TestCase {
	
    public SimpleTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SimpleTest.class);
    }

    public void testApplication() {
    	try {
    		System.setProperty("basedir", "src/main/resources");
			Main.run("src/main/resources/examples/sqltransfer_simpletest.xml", false, false);

			assertTrue(true);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
    }

}