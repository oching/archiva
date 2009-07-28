package org.apache.archiva.web.test.parent;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.codehaus.plexus.util.StringUtils;

import org.testng.Assert;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: AbstractSeleniumTestCase.java 761154 2009-04-02 03:31:19Z wsmoak $
 */

public abstract class AbstractSeleniumTest {

	public static String baseUrl;

    public static String maxWaitTimeInMs;

    private static ThreadLocal<Selenium> selenium;

    public static Properties p;
    
    public void open()
	    throws Exception
	{
	    p = new Properties();
	    p.load( this.getClass().getClassLoader().getResourceAsStream( "testng.properties" ) );
	
	    baseUrl = p.getProperty( "BASE_URL" );
	    maxWaitTimeInMs = p.getProperty( "MAX_WAIT_TIME_IN_MS" );
	
	    String seleniumHost	= p.getProperty( "SELENIUM_HOST" );
	    int seleniumPort = Integer.parseInt( ( p.getProperty( "SELENIUM_PORT" ) ) );

        String seleniumBrowser = System.getProperty( "browser" );
        if ( StringUtils.isEmpty( seleniumBrowser ) )
        {
            seleniumBrowser = p.getProperty( "SELENIUM_BROWSER" );
        }

	    final Selenium s = new DefaultSelenium( seleniumHost, seleniumPort, seleniumBrowser, baseUrl );
	    selenium = new ThreadLocal<Selenium>()
	    {
	        protected Selenium initialValue()
	        {
	            return s;
	        }
	    };
	    getSelenium().start();
	}
	
	protected static Selenium getSelenium()
	{
	    return selenium.get();
	}
    
	    public void close()
	    throws Exception
	{
	    getSelenium().stop();
	}
	
	// *******************************************************
	// Auxiliar methods. This method help us and simplify test.
	// *******************************************************
	
	public void captureScreenshot()
	{
		Date t = new Date();
		File f = new File( "" );
		String baseDir = f.getAbsolutePath();
		String time = t.toString();
		getSelenium().windowMaximize();
		getSelenium().windowFocus();
		getSelenium().captureScreenshot( baseDir + "/target/screenshots/" + getClass() + "-" + time + ".png" );
	}
	    
	public void assertFieldValue( String fieldValue, String fieldName )
	{
		try 
		{
			assertElementPresent( fieldName );
			Assert.assertEquals( fieldValue, getSelenium().getValue( fieldName ) );
		}
		catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			assertElementPresent( fieldName );
			Assert.assertEquals( fieldValue, getSelenium().getValue( fieldName ) );
		}
	}
	
	public void assertPage( String title )
	{
	    try 
	    {
			Assert.assertEquals( getSelenium().getTitle(), title );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertEquals( getSelenium().getTitle(), title );
		}
	}
	
	public String getTitle()
	{
	    return getSelenium().getTitle();
	}
	
	public String getHtmlContent()
	{
	    return getSelenium().getHtmlSource();
	}
	
	public void assertTextPresent( String text )
	{
		try
		{
			Assert.assertTrue( getSelenium().isTextPresent( text ), "'" + text + "' isn't present." );
		}
		catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertTrue( getSelenium().isTextPresent( text ), "'" + text + "' isn't present." );
		}
	}
	
	public void assertTextNotPresent( String text )
	{
		try 
	    {
			Assert.assertFalse( getSelenium().isTextPresent( text ), "'" + text + "' is present." );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertFalse( getSelenium().isTextPresent( text ), "'" + text + "' is present." );
		}
	}
	
	public void assertElementPresent( String elementLocator )
	{
	    try 
	    {
			Assert.assertTrue( isElementPresent( elementLocator ), "'" + elementLocator + "' isn't present." );
		} 
	    catch ( java.lang.AssertionError e)
	    {
	    	captureScreenshot();
	    	Assert.assertTrue( isElementPresent( elementLocator ), "'" + elementLocator + "' isn't present." );
		}
	}
	
	public void assertElementNotPresent( String elementLocator )
	{
	    try 
	    {
			Assert.assertFalse( isElementPresent( elementLocator ), "'" + elementLocator + "' is present." );
		} 
	    catch ( java.lang.AssertionError e) 
	    {
	    	captureScreenshot();
	    	Assert.assertFalse( isElementPresent( elementLocator ), "'" + elementLocator + "' is present." );
		}
	}
	
	public void assertLinkPresent( String text )
	{
		try 
		{
			Assert.assertTrue( isElementPresent( "link=" + text ), "The link '" + text + "' isî't present." );
		} 
		catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertTrue( isElementPresent( "link=" + text ), "The link '" + text + "' isî't present." );
		}
	}
	
	public void assertLinkNotPresent( String text )
	{
		try 
		{
			Assert.assertFalse( isElementPresent( "link=" + text ), "The link('" + text + "' is present." );
		} 
		catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertFalse( isElementPresent( "link=" + text ), "The link('" + text + "' is present." );
		}
	}
	
	public void assertImgWithAlt( String alt )
	{
	    try 
	    {
			assertElementPresent( "/¯img[@alt='" + alt + "']" );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			assertElementPresent( "/¯img[@alt='" + alt + "']" );
		}
	}
	
	public void assertImgWithAltAtRowCol( boolean isALink, String alt, int row, int column )
	{
	    String locator = "//tr[" + row + "]/td[" + column + "]/";
	    locator += isALink ? "a/" : "";
	    locator += "img[@alt='" + alt + "']";
	
	    try 
	    {
			assertElementPresent( locator );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			assertElementPresent( locator );
		}
	}
	
	public void assertCellValueFromTable( String expected, String tableElement, int row, int column )
	{
	    try 
	    {
			Assert.assertEquals( expected, getCellValueFromTable( tableElement, row, column ) );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertEquals( expected, getCellValueFromTable( tableElement, row, column ) );
		}
	}
	
	public boolean isTextPresent( String text )
	{
	    return getSelenium().isTextPresent( text );
	}
	
	public boolean isLinkPresent( String text )
	{
	    return isElementPresent( "link=" + text );
	}
	
	public boolean isElementPresent( String locator )
	{
	    return getSelenium().isElementPresent( locator );
	}
	
	public void waitPage()
	{
	    getSelenium().waitForPageToLoad( maxWaitTimeInMs );
	}
	
	public String getFieldValue( String fieldName )
	{
	    return getSelenium().getValue( fieldName );
	}
	
	public String getCellValueFromTable( String tableElement, int row, int column )
	{
	    return getSelenium().getTable( tableElement + "." + row + "." + column );
	}
	
	public void selectValue( String locator, String value )
	{
	    getSelenium().select( locator, "label=" + value );
	}
	
	public void assertOptionPresent( String selectField, String[] options )
	{
	    assertElementPresent( selectField );
	    String[] optionsPresent = getSelenium().getSelectOptions( selectField );
	    List<String> expected = Arrays.asList( options );
        List<String> present = Arrays.asList( optionsPresent );
	    Assert.assertTrue( present.containsAll( expected ), "Options expected are not included in present options" );
	}
	
	public void assertSelectedValue( String value, String fieldName )
	{
	    assertElementPresent( fieldName );
	    String optionsPresent = getSelenium().getSelectedLabel( value );
	    Assert.assertEquals( optionsPresent, value );
	}
	
	public void submit()
	{
	    clickLinkWithXPath( "//input[@type='submit']" );
	}
	
	public void assertButtonWithValuePresent( String text )
	{
	    try 
	    {
			Assert.assertTrue( isButtonWithValuePresent( text ), "'" + text + "' button isn't present" );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertTrue( isButtonWithValuePresent( text ), "'" + text + "' button isn't present" );
		}
	}
	
	public void assertButtonWithIdPresent( String id )
	{
	    try 
	    {
			Assert.assertTrue( isButtonWithIdPresent( id ), "'Button with id =" + id + "' isn't present" );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertTrue( isButtonWithIdPresent( id ), "'Button with id =" + id + "' isn't present" );
		}
	}
	
	public void assertButtonWithValueNotPresent( String text )
	{
	    try 
	    {
			Assert.assertFalse( isButtonWithValuePresent( text ), "'" + text + "' button is present" );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertFalse( isButtonWithValuePresent( text ), "'" + text + "' button is present" );
		}
	}
	
	public boolean isButtonWithValuePresent( String text )
	{
	    return isElementPresent( "//button[@value='" + text + "']" )
	        || isElementPresent( "//input[@value='" + text + "']" );
	}
	
	public boolean isButtonWithIdPresent( String text )
	{
	    return isElementPresent( "//button[@id='" + text + "']" ) || isElementPresent( "//input[@id='" + text + "']" );
	}
	
	public void clickButtonWithValue( String text )
	{
	    clickButtonWithValue( text, true );
	}
	
	public void clickButtonWithValue( String text, boolean wait )
	{
	    assertButtonWithValuePresent( text );
	
	    if ( isElementPresent( "//button[@value='" + text + "']" ) )
	    {
	        clickLinkWithXPath( "//button[@value='" + text + "']", wait );
	    }
	    else
	    {
	        clickLinkWithXPath( "//input[@value='" + text + "']", wait );
	    }
	}
	
	public void clickSubmitWithLocator( String locator )
	{
	    clickLinkWithLocator( locator );
	}
	
	public void clickSubmitWithLocator( String locator, boolean wait )
	{
	    clickLinkWithLocator( locator, wait );
	}
	
	public void clickImgWithAlt( String alt )
	{
	    clickLinkWithLocator( "//img[@alt='" + alt + "']" );
	}
	
	public void clickLinkWithText( String text )
	{
	    clickLinkWithText( text, true );
	}
	
	public void clickLinkWithText( String text, boolean wait )
	{
	    clickLinkWithLocator( "link=" + text, wait );
	}
	
	public void clickLinkWithXPath( String xpath )
	{
	    clickLinkWithXPath( xpath, true );
	}
	
	public void clickLinkWithXPath( String xpath, boolean wait )
	{
	    clickLinkWithLocator( "xpath=" + xpath, wait );
	}
	
	public void clickLinkWithLocator( String locator )
	{
	    clickLinkWithLocator( locator, true );
	}
	
	public void clickLinkWithLocator( String locator, boolean wait )
	{
	    assertElementPresent( locator );
	    getSelenium().click( locator );
	    if ( wait )
	    {
	        waitPage();
	    }
	}
	
	public void setFieldValues( Map<String, String> fieldMap )
	{
	    Map.Entry<String, String> entry;
	
	    for ( Iterator<Entry<String, String>> entries = fieldMap.entrySet().iterator(); entries.hasNext(); )
	    {
	        entry = entries.next();
	
	        getSelenium().type( entry.getKey(), entry.getValue() );
	    }
	}
	
	public void setFieldValue( String fieldName, String value )
	{
	    getSelenium().type( fieldName, value );
	}
	
	public void checkField( String locator )
	{
	    getSelenium().check( locator );
	}
	
	public void uncheckField( String locator )
	{
	    getSelenium().uncheck( locator );
	}
	
	public boolean isChecked( String locator )
	{
	    return getSelenium().isChecked( locator );
	}
	
	public void assertIsChecked( String locator )
	{
	    try 
	    {
			Assert.assertTrue( getSelenium().isChecked( locator ) );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertTrue( getSelenium().isChecked( locator ) );
		}
	}
	
	public void assertIsNotChecked( String locator )
	{
	    try 
	    {
			Assert.assertFalse( getSelenium().isChecked( locator ) );
		} 
	    catch ( java.lang.AssertionError e) 
		{
			captureScreenshot();
			Assert.assertFalse( getSelenium().isChecked( locator ) );
		}
	}
	    
}