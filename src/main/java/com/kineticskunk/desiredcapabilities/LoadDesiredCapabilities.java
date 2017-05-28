package com.kineticskunk.desiredcapabilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.kineticskunk.driverutilities.WebDriverLoggingPreferences;
import com.kineticskunk.driverutilities.WebDriverProxy;
import com.kineticskunk.firefox.LoadFireFoxProfilePreferences;
import com.kineticskunk.utilities.Converter;

public class LoadDesiredCapabilities {

	private final Logger logger = LogManager.getLogger(Thread.currentThread().getName());
	private final Marker LOADDESIREDCAPABILITIES = MarkerManager.getMarker("LOADDESIREDCAPABILITIES");

	private static final String CONFIGLOADSETTINGS = "configloadingsetting";

	private static final String COMMONDESIREDCAPABILITIES = "commondesiredcapabilities";
	private static final String LOADFIREFOXPREFERENCES = "loadfirefoxprofilepreferences";
	private static final String LOADFIREBUG = "loadfirebug";
	private static final String LOADPROXYSERVER = "loadproxyserver";
	
	private static final String FIREFOXDESIREDCAPABILITIES = "firefoxdesiredcapabilities";
	private static final String FIREFOXPROFILEPREFERENCES = "firefoxprofilepreferences";

	private static final String PROXYSERVER = "proxyserver";
	private static final String HTTPPROXY = "httpproxy";
	private static final String SSLPROXY = "sslproxy";
	private static final String FTPPROXY = "ftpproxy";
	
	private static final String LOADINGLOGGINFPREFS = "loadloggingprefs";
	private static final String LOGGINGPREFS = "loggingPrefs";
	
	private DesiredCapabilities dc = new DesiredCapabilities();
	private WebDriverLoggingPreferences wdlp = new WebDriverLoggingPreferences();
	
	private boolean loadfirefoxprofilepreferences = false;
	private boolean loadProxyServer = false;
	private boolean loadloggingprefs = false;
	
	private String browserType = null;

	private JSONParser parser = new JSONParser();
	private JSONObject desiredCapabilitiesJSONObject = null;

	public LoadDesiredCapabilities() {
	}

	public LoadDesiredCapabilities(String browserType, String desiredCapabilitiesConfigJSON) {
		this();
		this.browserType = browserType;
		this.setDesiredCapabilitiesJSONObject(desiredCapabilitiesConfigJSON);

		if (this.jsonKeyExists(this.desiredCapabilitiesJSONObject, CONFIGLOADSETTINGS)) {
			JSONObject configloadingsetting = (JSONObject) this.desiredCapabilitiesJSONObject.get(CONFIGLOADSETTINGS);
			this.loadfirefoxprofilepreferences = this.getJSONBooleanValue(configloadingsetting, LOADFIREFOXPREFERENCES);
			this.loadProxyServer = getJSONBooleanValue(configloadingsetting, LOADPROXYSERVER);
			this.loadloggingprefs = this.getJSONBooleanValue(configloadingsetting, LOADINGLOGGINFPREFS);
		}
	}
	
	public  LoadDesiredCapabilities(String browserType, String desiredCapabilitiesConfigJSON, DesiredCapabilities dc) {
		this(browserType, desiredCapabilitiesConfigJSON);
		this.dc = dc;
	}

	private Boolean getJSONBooleanValue(JSONObject jsonObject, String key) {
		if (this.jsonKeyExists(jsonObject, key)) {
			return Converter.toBoolean(jsonObject.get(key));
		}
		return false;
	}

	private boolean jsonKeyExists(JSONObject jsonObject, String key) {
		if (jsonObject.containsKey(key)) {
			return true; 
		} else {
			this.logger.error(LOADDESIREDCAPABILITIES, "JSONObject " + (char)34 + jsonObject.toJSONString() + (char)34 + " object doesn't contain key " + (char)34 + key + (char)34);
		}
		return false;
	}

	public void setDesiredCapabilitiesJSONObject(String desiredCapabilitiesConfigJSON) {
		try {
			this.desiredCapabilitiesJSONObject = (JSONObject) this.parser.parse(new FileReader(new File(this.getClass().getClassLoader().getResource(desiredCapabilitiesConfigJSON).getPath())));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setDesiredCapabilities(JSONObject jsonObj, String jsonKey) {
		if (this.jsonKeyExists(jsonObj, jsonKey)) {
			this.logger.info(LOADDESIREDCAPABILITIES, "Loading " + (char)34 + jsonKey + (char)34 + " desired capabilities ");
			JSONObject jsonKeyObj = (JSONObject) jsonObj.get(jsonKey);
			Iterator<?> iterator = jsonKeyObj.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				if (Converter.isBoolean(value)) {
					this.dc.setCapability(key, Converter.toBoolean(value));
				} else if (Converter.isNumeric(value)) {
					this.dc.setCapability(key, Converter.toInteger(value));
				} else {
					if (key.equalsIgnoreCase("firefox_binary")) {
						this.dc.setCapability(key, new FirefoxBinary(new File(value.toString())));
					} else {
						this.dc.setCapability(key, value);
					}
				}
				this.logger.info(LOADDESIREDCAPABILITIES, "Loaded desiredCapability " + (char)34 + key + (char)34 + " with value " + (char)34 + this.dc.getCapability(key) + (char)34);
			}
		} else {
			this.logger.info(LOADDESIREDCAPABILITIES, "JSON Key " + (char)34 + jsonKey + (char)34 + " does not exist in " + (char)34 + jsonObj.toJSONString() + (char)34);
		}
	}

	public JSONObject getDesiredCapabilitiesJSONObject() {
		return this.desiredCapabilitiesJSONObject;
	}

	public void setBrowerDesiredCapabilities() {
		try {
			if (this.desiredCapabilitiesJSONObject != null) {
				this.setDesiredCapabilities(this.desiredCapabilitiesJSONObject, COMMONDESIREDCAPABILITIES);
				this.setSeleniumProxy(this.loadProxyServer);
				this.setLoggingPrefs(this.loadloggingprefs);
				switch (this.browserType.toLowerCase()) {
				case "firefox":
					this.setDesiredCapabilities(this.desiredCapabilitiesJSONObject, FIREFOXDESIREDCAPABILITIES);
					this.dc.setBrowserName("firefox");
					if (this.loadfirefoxprofilepreferences) {
						if (this.jsonKeyExists(this.desiredCapabilitiesJSONObject, FIREFOXPROFILEPREFERENCES)) {
							JSONObject preferences = (JSONObject) this.desiredCapabilitiesJSONObject.get(FIREFOXPROFILEPREFERENCES);
							LoadFireFoxProfilePreferences lfpp = new LoadFireFoxProfilePreferences(preferences);
							lfpp.loadFireFoxExtensionsAndExtensionPreferences();
						}
					}
					break;
				case "chrome":
					this.dc.setBrowserName("chrome");
					//this.dc.setCapability("chromeDriverExecutable", driverExecutable.getAbsolutePath());
					this.dc.setCapability("chrome.switches", Arrays.asList("--no-default-browser-check"));
					this.dc.setCapability("chromeOptions", getChromeOptions());
					this.dc.setCapability("chromePreferences", getChromePreferences()); 
					break;
				case "opera":
					this.dc.setBrowserName("opera");
					break;
				case "ie":

					this.dc.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
					System.setProperty("webdriver.ie.driver", browserType);
					break;
				}		
			} 
		} catch (Exception ex) {

		}
	}

	public DesiredCapabilities getDesiredCapabilities() {
		return this.dc;
	}

	public void setSeleniumProxy(boolean useProxy) {
		WebDriverProxy proxy = new WebDriverProxy();
		if (useProxy) {
			JSONObject proxySettings = (JSONObject) this.desiredCapabilitiesJSONObject.get(PROXYSERVER);
			if (this.jsonKeyExists(proxySettings, HTTPPROXY)) {
				proxy.setHTTPProxy(proxySettings.get(HTTPPROXY).toString());
			}
			if (this.jsonKeyExists(proxySettings, SSLPROXY)) {
				proxy.setHTTPProxy(proxySettings.get(SSLPROXY).toString());
			}
			if (this.jsonKeyExists(proxySettings, FTPPROXY)) {
				proxy.setHTTPProxy(proxySettings.get(FTPPROXY).toString());
			}
			proxy.setProxyType("MANUAL");
		} else {
			proxy.setProxyType("AUTODETECT");
		}
		this.dc.setCapability(CapabilityType.PROXY, proxy.getProxy());
	}
	
	public void setLoggingPrefs(boolean loadLoggingPrefs) {
		if (loadLoggingPrefs) {
			this.logger.log(Level.INFO, LOADDESIREDCAPABILITIES, "Loading logging preferences");
			try {
				JSONObject loggingPrefs = (JSONObject) this.desiredCapabilitiesJSONObject.get(LOGGINGPREFS);
				wdlp = new WebDriverLoggingPreferences(loggingPrefs);
				this.dc.setCapability(CapabilityType.LOGGING_PREFS, wdlp.getLoggingPreferences());
			} catch (Exception ex) {
				logger.catching(ex);

			}
		} else {
			this.logger.log(Level.INFO, LOADDESIREDCAPABILITIES, "Loading logging preferences ARE NOT being loaded.");
		}
	}

	private ChromeOptions getChromeOptions() {
		ChromeOptions co = new ChromeOptions();
		co.addArguments("start-maximized");
		co.addArguments("ignore-certificate-errors");
		co.addArguments("--new-window");
		return co;
	}

	private HashMap<String, Object> getChromePreferences() {
		HashMap<String, Object> chromePreferences = new HashMap<String, Object>();
		chromePreferences.put("profile.password_manager_enabled", "false");
		return chromePreferences;
	}



}