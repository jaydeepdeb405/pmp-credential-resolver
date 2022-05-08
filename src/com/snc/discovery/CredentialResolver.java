package com.snc.discovery;

import java.io.IOException;
import java.util.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.snc.automation_common.integration.creds.IExternalCredential;
import com.snc.core_automation_common.logging.Logger;
import com.snc.core_automation_common.logging.LoggerFactory;

/**
 * Basic implementation of a CredentialResolver that uses a properties file.
 */

public class CredentialResolver implements IExternalCredential {

	private static final String PMP_AUTHTOKEN_PROP_NAME = "ext.cred.pmp.authtoken";
	private static final String PMP_HOST_PROP_NAME = "ext.cred.pmp.host";
	private static String PMP_AUTHTOKEN;
	private static String PMP_HOST;

	private static final String AUTHTOKEN_HEADER_NAME = "AUTHTOKEN";
	private static final String PMP_HOST_DEFAULT = "http://localhost:7272";

	// Logger object to log messages in agent.log
	private static final Logger fLogger = LoggerFactory.getLogger(CredentialResolver.class);

	public CredentialResolver() {
	}

	/**
	 * Config method with pre-loaded config parameters from config.xml.
	 * @param configMap - contains config parameters with prefix "ext.cred" only.
	 */
	@Override
	public void config(Map<String, String> configMap) {
		PMP_HOST = configMap.get(PMP_HOST_PROP_NAME);
		if (PMP_HOST == null || PMP_HOST == "")
			PMP_HOST = PMP_HOST_DEFAULT;

		fLogger.info("PMP Host from config.xml '"+PMP_HOST+"'");

		PMP_AUTHTOKEN = configMap.get(PMP_AUTHTOKEN_PROP_NAME);
		if (PMP_HOST == null || PMP_HOST == "")
			fLogger.error("PMP Authtoken not found in config.xml");
		else
			fLogger.info("PMP Authtoken from config.xml '"+PMP_AUTHTOKEN+"'");
	}

	/**
	 * Resolve a credential.
	 */
	public Map<String, String> resolve(Map<String, String> args) {
		String id = (String) args.get(ARG_ID);
		// String type = (String) args.get(ARG_TYPE); // can be used to determine credential type e.g.-"ssh_password"

		Map<String, String> result = new HashMap<String, String>();

		String[] credentialId = id.split("@", 2);
		String userName = null, password = null;

		if (credentialId.length == 2) {
			String resourceId = credentialId[0];
			String accountId = credentialId[1];

			try {
				Integer.parseInt(resourceId);
				Integer.parseInt(accountId);
			} catch(NumberFormatException e) {
				try {
					String[] ids = getIds(resourceId, accountId);
					resourceId = ids[0];
					accountId = ids[1];
				} catch (IOException ioe) {
					fLogger.error("Error while accessing API - " + ioe.getCause() + '\n' + ioe.getMessage());
				}
			}

			try {
				userName = getUsername(resourceId, accountId);
				password = getPassword(resourceId, accountId);
			} catch(IOException e) {
				fLogger.error("Error while accessing API - " + e.getCause() + '\n' + e.getMessage());
			}

			// working for SSH Credentials only as of now
			if (userName != null && password != null) {
				result.put(VAL_USER, userName);
				result.put(VAL_PSWD, password);
			}
		} else {
			fLogger.error("Invalid credential id [" + id + "]");
		}

		return result;
	}


	/**
	 * Return the API version supported by this class.
	 */
	public String getVersion() {
		return "1.0";
	}

	private String[] getIds(String resourceName, String accountName) throws IOException {
		String[] ids = new String[2];
		CloseableHttpResponse response = null;
		JsonObject resultObject = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(PMP_HOST + "/restapi/json/v1/resources/getResourceIdAccountId?RESOURCENAME=" + resourceName + "&ACCOUNTNAME=" + accountName);
		httpGet.addHeader(AUTHTOKEN_HEADER_NAME, PMP_AUTHTOKEN);
		response = httpClient.execute(httpGet);
		String JSONString = EntityUtils.toString(response.getEntity(),"UTF-8");
		JsonParser parser = new JsonParser();
		resultObject = parser.parse(JSONString).getAsJsonObject();
		if (resultObject.get("message") != null) {
			fLogger.error("Error getting object: " + resultObject.get("message"));
		}
		if (isStatusSuccess(resultObject)) {
			ids[0] = resultObject.getAsJsonObject("operation").getAsJsonObject("Details").get("RESOURCEID").getAsString();
			ids[1] = resultObject.getAsJsonObject("operation").getAsJsonObject("Details").get("ACCOUNTID").getAsString();
		}
		response.close();
		return ids;
	}

	private String getUsername(String resourceId, String accountId) throws IOException {
		String userName = null;
		CloseableHttpResponse response = null;
		JsonObject resultObject = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(PMP_HOST + "/restapi/json/v1/resources/" + resourceId + "/accounts");
		httpGet.addHeader(AUTHTOKEN_HEADER_NAME, PMP_AUTHTOKEN);
		response = httpClient.execute(httpGet);
		String JSONString = EntityUtils.toString(response.getEntity(),"UTF-8");
		JsonParser parser = new JsonParser();
		resultObject = parser.parse(JSONString).getAsJsonObject();
		if (resultObject.get("message") != null) {
			fLogger.error("Error getting object: " + resultObject.get("message"));
		}
		if (isStatusSuccess(resultObject)) {
			JsonArray accountList = resultObject.getAsJsonObject("operation").getAsJsonObject("Details").getAsJsonArray("ACCOUNT LIST");
			for(JsonElement el: accountList) {
				JsonObject account = el.getAsJsonObject();
				if(account.get("ACCOUNT ID").getAsString().equalsIgnoreCase(accountId)) {
					userName = account.get("ACCOUNT NAME").getAsString();
					break;
				}
			}
		}
		response.close();
		return userName;
	}

	private String getPassword(String resourceId, String accountId) throws IOException {
		CloseableHttpResponse response = null;
		JsonObject resultObject = null;
		String password = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(PMP_HOST + "/restapi/json/v1/resources/" + resourceId + "/accounts/" + accountId + "/password");
		httpGet.addHeader(AUTHTOKEN_HEADER_NAME, PMP_AUTHTOKEN);
		response = httpClient.execute(httpGet);
		String JSONString = EntityUtils.toString(response.getEntity(),"UTF-8");
		JsonParser parser = new JsonParser();
		resultObject = parser.parse(JSONString).getAsJsonObject();
		if (resultObject.get("message") != null) {
			fLogger.error("Error getting object: " + resultObject.get("message"));
		}
		if (isStatusSuccess(resultObject))
			password = resultObject.getAsJsonObject("operation").getAsJsonObject("Details").get("PASSWORD").getAsString();
		response.close();
		return password;
	}

	private Boolean isStatusSuccess(JsonObject apiResponse) {
		Boolean success = false;
		String apiName = "";
		String apiError = "";
		if (apiResponse.has("operation")) {
			JsonObject operation = apiResponse.getAsJsonObject("operation");
			if (operation.has("name"))
				apiName = operation.get("name").getAsString();
			if (operation.has("result")) {
				JsonObject result = operation.getAsJsonObject("result");
				if (result.has("message")) {
					apiError = result.get("message").getAsString();
				}
				if (result.has("status")) {
					String status = result.get("status").getAsString();
					if (status.equals("Success"))
						success = true;
				}
			}
		}
		if (!success)
			fLogger.error("'"+apiName+"' API call unsuccessful. Message from API: "+apiError);
		return success;
	}

	public static void main(String[] args) {
		CredentialResolver obj = new CredentialResolver();
		// add your hostname & apitoken below to test
		PMP_HOST = PMP_HOST_DEFAULT;
		PMP_AUTHTOKEN = "";

		// sample credential id, resource id & account id should be delimited by '@'
		Map<String, String> resArgs = new HashMap<String, String>();
		resArgs.put(ARG_ID, "2@2");
		Map<String, String> credentials = obj.resolve(resArgs);
		System.out.println(credentials.get(VAL_USER));
		System.out.println(credentials.get(VAL_PSWD));
	}

}