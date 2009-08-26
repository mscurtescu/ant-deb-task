package com.googlecode.ant_deb_task;

import junit.framework.TestCase;
import org.apache.tools.ant.BuildException;

public class DebVersionTest extends TestCase
{
    private Deb.Version version = null;

    protected void setUp() throws Exception
    {
        version = new Deb.Version();
    }

    public DebVersionTest(String s)
    {
        super(s);
    }
    
    public void testNumericUpstream()
    {
        version.setUpstream("123");

        assertEquals("123" + "-1", version.toString());
    }

    public void testAlphanumericUpstream()
    {
        version.setUpstream("123AbC");

        assertEquals("123AbC" + "-1", version.toString());
    }

    public void testBadStartAlphanumericUpstream()
    {
        try
        {
            version.setUpstream("x123AbC");

            fail("Upstream version must start with a digit.");
        }
        catch (BuildException e)
        {
            // expected
        }
    }

    public void testAlphanumericSymbolUpstream()
    {
        version.setUpstream("12.3A+b-C~");

        assertEquals("12.3A+b-C~" + "-1", version.toString());
    }

    public void testEmptyDebian()
    {
        version.setUpstream("123");
        version.setDebian("");

        assertEquals("123", version.toString());
    }

    public void testDefaultDebian()
    {
        version.setUpstream("123");

        assertEquals("123" + "-1", version.toString());
    }

    public void testNumericDebian()
    {
        version.setUpstream("123");
        version.setDebian("7");

        assertEquals("123" + "-7", version.toString());
    }

    public void testAlphanumericDebian()
    {
        version.setUpstream("123");
        version.setDebian("7aBc");

        assertEquals("123" + "-7aBc", version.toString());
    }

    public void testAlphanumeric2Debian()
    {
        version.setUpstream("123");
        version.setDebian("x7");

        assertEquals("123" + "-x7", version.toString());
    }

    public void testAlphanumericSymbolDebian()
    {
        version.setUpstream("123");
        version.setDebian("7+a.B~c");

        assertEquals("123" + "-7+a.B~c", version.toString());
    }

    public void testBadAlphanumericSymbolDebian()
    {
        try
        {
            version.setDebian("7+a.B-c");
        }
        catch (BuildException e)
        {
            // expected
        }
    }

    public void testEmptyDebianAndUpstreamWithHyphen()
    {
        version.setUpstream("123-a");
        version.setDebian("");

        try
        {
            assertEquals("123-a", version.toString());

            fail("Upstream version cannot contain hyphen is debian version not present.");
        }
        catch (BuildException e) 
        {
            // expected
        }
    }

    public void testNoEpochAndUpstreamWithHyphen()
    {
        version.setUpstream("123:abc");

        try
        {
            version.toString();
        }
        catch (BuildException e)
        {
            // expected
        }
    }

    public void testEpochAndUpstreamWithHyphen()
    {
        version.setUpstream("123:abc");
        version.setEpoch(23);

        assertEquals("23:" + "123:abc" + "-1", version.toString());
    }

    public void testEpochUpstreamDebian()
    {
        version.setEpoch(6);
        version.setUpstream("12-a:z+~");
        version.setDebian("3~a+z.");

        assertEquals("6:" + "12-a:z+~" + "-3~a+z.", version.toString());
    }
}
