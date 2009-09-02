package com.topdesk.sqltransfer;

import java.io.File;

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
    		File workDir = new File(System.getProperty("user.dir") + "/src/main/resources");
    		System.setProperty("basedir", workDir.getAbsolutePath());
			Main.run("src/main/resources/examples/sqltransfer_simpletest.xml", false, false);

			assertTrue(true);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
    }

}