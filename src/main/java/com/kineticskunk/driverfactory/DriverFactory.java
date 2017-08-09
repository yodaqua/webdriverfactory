package com.kineticskunk.driverfactory;

/*
	Copyright [2016] [KineticSkunk Information Technology Solutions]
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.Toolkit;
import java.net.MalformedURLException;
import java.net.URL;

import com.kineticskunk.auto.conversion.Converter;
import static com.kineticskunk.driverfactory.DriverType.valueOf;

public class DriverFactory {
	
	private final Logger logger = LogManager.getLogger(DriverFactory.class.getName());
	private final Marker DRIVEFACTORY = MarkerManager.getMarker("DRIVEFACTORY");
	
    private WebDriver webdriver;
    private DriverType selectedDriverType;
    private DriverType defaultDriverType;
    private boolean useRemoteWebDriver;
    private String remoteDriverURL;
    private boolean bringDriverToFront;
    private boolean resizeBrowser;
   
    private DesiredCapabilities dc;
    
    public DriverFactory() {
    	this.logger.log(Level.INFO, DRIVEFACTORY, "In " + DriverFactory.class.getName());
    }
    
    public DriverFactory(DesiredCapabilities dc) {
    	this();
    	this.dc = dc; 
    }
    
    public DriverFactory(DesiredCapabilities dc, boolean useRemoteWebDriver, String remoteDriverURL) {
    	this();
    	this.dc = dc;
    	this.useRemoteWebDriver = useRemoteWebDriver;
    	this.remoteDriverURL = remoteDriverURL;
    }
    
    public void setBringDriverToFront(boolean bringDriverToFront) {
    	this.bringDriverToFront = bringDriverToFront;
    }
    
    public void setResizeBrowser(boolean resizeBrowser) {
    	this.resizeBrowser = resizeBrowser;
    }
    
    public WebDriver getDriver() throws Exception {
        if (null == webdriver) {
            determineEffectiveDriverType();
            DesiredCapabilities desiredCapabilities = selectedDriverType.getDesiredCapabilities(this.dc);
            instantiateWebDriver(desiredCapabilities);
        }
        
        if (this.bringDriverToFront) {
        	String currentWindowHandle = this.webdriver.getWindowHandle();
    		((JavascriptExecutor) this.webdriver).executeScript("alert('Test')"); 
    		this.webdriver.switchTo().alert().accept();
    		this.webdriver.switchTo().window(currentWindowHandle);
        }
        
        if (this.resizeBrowser) {
        	java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        	
    		this.webdriver.manage().window().setSize(new Dimension(Converter.toInteger(screenSize.getWidth()), Converter.toInteger(screenSize.getHeight())));
        }
        return this.webdriver;
    }

    public void quitDriver() {
        if (null != webdriver) {
            webdriver.quit();
        }
    }

    private void determineEffectiveDriverType() {
        DriverType driverType = defaultDriverType;
        try {
            driverType = valueOf(this.dc.getBrowserName().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        	logger.catching(ignored);
        	logger.log(Level.FATAL, DRIVEFACTORY, "Unknown driver specified, defaulting to '" + driverType + "'...");
        } catch (NullPointerException ignored) {
        	driverType = DriverType.FIREFOX;
        	logger.log(Level.DEBUG, DRIVEFACTORY, "No driver specified, defaulting to '" + driverType + "'...");
        }
        this.selectedDriverType = driverType;
    }

    private void instantiateWebDriver(DesiredCapabilities desiredCapabilities) throws MalformedURLException {
    	logger.log(Level.INFO, DRIVEFACTORY, "Current Operating System: " + System.getProperty("os.name").toUpperCase());
    	logger.log(Level.INFO, DRIVEFACTORY, "Current Architecture: " + System.getProperty("os.arch"));
    	logger.log(Level.INFO, DRIVEFACTORY, "Current Browser Selection: " + this.selectedDriverType);
    	
        if (useRemoteWebDriver) {
            this.webdriver = new RemoteWebDriver(new URL(this.remoteDriverURL), desiredCapabilities);
        } else {
            this.webdriver = this.selectedDriverType.getWebDriverObject(desiredCapabilities);
        }
    }
}