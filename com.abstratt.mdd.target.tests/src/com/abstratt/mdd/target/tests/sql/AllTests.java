package com.abstratt.mdd.target.tests.sql;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTest(SelectTests.suite());
        return suite;
    }
}
