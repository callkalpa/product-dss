/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.dss.integration.test.odata;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.dss.integration.test.DSSIntegrationTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains OData specific test cases.
 */
public class ODataTenantUserTestCase extends DSSIntegrationTest {
	private final String serviceName = "ODataSampleService";
	private final String configId = "default";

	@BeforeClass(alwaysRun = true)
	public void serviceDeployment() throws Exception {
		super.init(TestUserMode.TENANT_USER);
		List<File> sqlFileLis = new ArrayList<>();
		sqlFileLis.add(selectSqlFile("CreateODataTables.sql"));
		sqlFileLis.add(selectSqlFile("Customers.sql"));
		deployService(serviceName,
		              createArtifact(getResourceLocation() + File.separator + "dbs" + File.separator + "odata" +
		                             File.separator + "ODataSampleService.dbs", sqlFileLis));
	}

	@Test(groups = { "wso2.dss" }, description = "test the service document retrieval")
	public void validateServiceDocumentTestCase() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/" + serviceName + "/" + configId +
		                  "/$metadata";
		Object[] response = sendGET(endpoint, "Application/xml");
		Assert.assertEquals(response[0], 200);
		endpoint = "http://localhost:9763/odata/t/wso2.com/" + serviceName + "/" + configId + "/";
		response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval")
	public void validateRetrievingData() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/" + serviceName + "/" + configId +
		                  "/CUSTOMERS";
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval")
	public void validatePostingData() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/" + serviceName + "/" + configId + "/FILES";
		String content = "{\"FILENAME\": \"M.K.H.Gunasekara\" ,\"TYPE\" : \"dss\"}";
		int responseCode = sendPOST(endpoint, content, "application/json");
		Assert.assertEquals(responseCode, 204);
		endpoint = "http://localhost:9763/odata/t/wso2.com/" + serviceName + "/" + configId +
		           "/FILES(\'M.K.H.Gunasekara\')";
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
		endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT";
		content = "{\"STUDENTID\" : 3 , \"FIRSTNAME\" : \"Madhawa\" , \"LASTNAME\" : \"Kasun\"}";
		responseCode = sendPOST(endpoint, content, "application/json");
		Assert.assertEquals(responseCode, 204);
		endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT(3)";
		response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
		content = "{\"STUDENTID\" : 4 , \"FIRSTNAME\" : \"Rajith\" , \"LASTNAME\" : \"Vitharana\"}";
		responseCode = sendPOST(endpoint, content, "application/json");
		Assert.assertEquals(responseCode, 204);
		endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT(4)";
		response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval", dependsOnMethods = "validatePatchingData")
	public void validatePuttingData() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT(3)";
		String content = "{\"LASTNAME\" : \"GUNASEKARA\"}";
		int responseCode = sendPUT(endpoint, content, "application/json");
		Assert.assertEquals(responseCode, 204);
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
		Assert.assertTrue(response[1].toString().contains("\"FIRSTNAME\":null") &&
		                  response[1].toString().contains("\"LASTNAME\":\"GUNASEKARA\""));
	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval", dependsOnMethods = "validatePostingData")
	public void validatePatchingData() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT(4)";
		String content = "{\"LASTNAME\" : \"Lanka\"}";
		int responseCode = sendPATCH(endpoint, content, "application/json");
		Assert.assertEquals(responseCode, 204);
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
		Assert.assertTrue(response[1].toString().contains("\"FIRSTNAME\":\"Rajith\"") &&
		                  response[1].toString().contains("\"LASTNAME\":\"Lanka\""));
	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval", dependsOnMethods = "validatePuttingData")
	public void validateDeletingData() throws Exception {
		String endpoint = "http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/STUDENT(3)";
		int responseCode = sendDELETE(endpoint, "application/json");
		Assert.assertEquals(responseCode, 204);
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 404);

	}

	@Test(groups = { "wso2.dss" }, description = "test the entity retrieval")
	public void validateSelectingData() throws Exception {
		String endpoint =
				"http://localhost:9763/odata/t/wso2.com/"+ serviceName + "/" + configId +"/CUSTOMERS?$select=PHONE,COUNTRY,POSTALCODE";
		Object[] response = sendGET(endpoint, "Application/json");
		Assert.assertEquals(response[0], 200);
		Assert.assertTrue(response[1].toString().contains("$metadata#CUSTOMERS(PHONE,POSTALCODE,COUNTRY)"));
	}

	private static int sendPOST(String endpoint, String content, String acceptType) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(endpoint);
		httpPost.setHeader("Accept", acceptType);
		if (null != content) {
			HttpEntity httpEntity = new ByteArrayEntity(content.getBytes("UTF-8"));
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(httpEntity);
		}
		HttpResponse httpResponse = httpClient.execute(httpPost);
		return httpResponse.getStatusLine().getStatusCode();
	}

	private static Object[] sendGET(String endpoint, String acceptType) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(endpoint);
		httpGet.setHeader("Accept", acceptType);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		if (httpResponse.getEntity() != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
			reader.close();
			return new Object[] { httpResponse.getStatusLine().getStatusCode(), response.toString() };
		} else {
			return new Object[] { httpResponse.getStatusLine().getStatusCode() };
		}
	}

	private static int sendPUT(String endpoint, String content, String acceptType) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPut httpPut = new HttpPut(endpoint);
		httpPut.setHeader("Accept", acceptType);
		if (null != content) {
			HttpEntity httpEntity = new ByteArrayEntity(content.getBytes("UTF-8"));
			httpPut.setHeader("Content-Type", "application/json");
			httpPut.setEntity(httpEntity);
		}
		HttpResponse httpResponse = httpClient.execute(httpPut);
		return httpResponse.getStatusLine().getStatusCode();
	}

	private static int sendPATCH(String endpoint, String content, String acceptType) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPatch httpPatch = new HttpPatch(endpoint);
		httpPatch.setHeader("Accept", acceptType);
		if (null != content) {
			HttpEntity httpEntity = new ByteArrayEntity(content.getBytes("UTF-8"));
			httpPatch.setHeader("Content-Type", "application/json");
			httpPatch.setEntity(httpEntity);
		}
		HttpResponse httpResponse = httpClient.execute(httpPatch);
		return httpResponse.getStatusLine().getStatusCode();
	}

	private static int sendDELETE(String endpoint, String acceptType) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpDelete httpDelete = new HttpDelete(endpoint);
		httpDelete.setHeader("Accept", acceptType);
		HttpResponse httpResponse = httpClient.execute(httpDelete);
		return httpResponse.getStatusLine().getStatusCode();
	}
}