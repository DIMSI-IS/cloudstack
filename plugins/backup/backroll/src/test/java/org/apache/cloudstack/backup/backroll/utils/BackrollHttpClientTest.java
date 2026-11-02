// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.backup.backroll.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.backup.backroll.model.response.api.LoginApiResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient.BackrollHttpClientException;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient.NotOkBodyException;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackrollHttpClientTest {

    @Spy
    @InjectMocks
    BackrollHttpClient backupHttpClient;

        @Mock
        private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private RequestConfig config;

    @Spy
    private SSLUtils sslUtils;

    @Spy
    private HttpClientBuilder httpClientBuilder;

    @Mock
    private SSLContext sslContext;

    @Mock
    private SSLConnectionSocketFactory sslConnectionSocketFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        backupHttpClient = BackrollHttpClient.createProvider(backupHttpClient, "http://api.backup.demo.ccc:5050/api/v1", "backroll-api",
                "test-secret", true, 300, 600);
    }

    private void defaultTestHttpClient(String path) throws ClientProtocolException, IOException, NotOkBodyException, BackrollHttpClientException {

                LoginApiResponse responseLogin = new LoginApiResponse();
                responseLogin.accessToken = "dummyToken";
                responseLogin.expiresIn = 3600;
                responseLogin.notBeforePolicy = "dummyNotBeforePolicy";
                responseLogin.refreshExpiresIn = "7200";
                responseLogin.scope = "dummyScope";

                String virtualMachineResponseString = "{ \"state\": \"SUCCESS\", \"info\": { \"archives\": [ { \"archive\": \"ROOT-00000\", \"barchive\": \"ROOT-00000\", \"id\": \"25d55ad283aa400af464c76d713c07ad7d163abdd3b8fbcdbdc46b827e5e0457\", \"name\": \"ROOT-00000\", \"start\": \"2024-11-08T18:24:48.000000\", \"time\": \"2024-11-08T18:24:48.000000\" } ], \"encryption\": { \"mode\": \"none\" }, \"repository\": { \"id\": \"36a11ebc0775a097c927735cc7015d19be7309be69fc15b896c5b1fd87fcbd79\", \"last_modified\": \"2024-11-29T09:53:09.000000\", \"location\": \"/mnt/backup/backup1\" } } }";

        CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        doReturn(httpClient).when(backupHttpClient).createHttpClient();
        doReturn(response).when(httpClient).execute(argThat(argument -> argument != null && argument.getURI().toString().contains("login")));

        doReturn(response2).when(httpClient).execute(argThat(argument -> argument != null && argument.getURI().toString().contains(path)));

                doReturn(new ObjectMapper().writeValueAsString(responseLogin)).when(backupHttpClient)
                                .okBody(response);
                doReturn(virtualMachineResponseString).when(backupHttpClient).okBody(response2);

                doReturn(statusLine).when(response).getStatusLine();
                doReturn(HttpStatus.SC_OK).when(statusLine).getStatusCode();

                doNothing().when(response).close();

        doReturn(new StringEntity("{\"mockKey\": \"mockValue\"}", ContentType.APPLICATION_JSON)).when(response).getEntity();
    }

    @Test
    public void testCreateHttpClient_WithValidateCertificateTrue() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, BackrollHttpClientException {
        backupHttpClient = BackrollHttpClient.createProvider(backupHttpClient, "http://api.backup.demo.ccc:5050/api/v1", "backroll-api",
                "test-secret", true, 300, 600);

        // Mock HttpClientBuilder
        HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
        try (MockedStatic<HttpClientBuilder> utilities = Mockito.mockStatic(HttpClientBuilder.class)) {
            utilities.when(HttpClientBuilder::create).thenReturn(mockBuilder);
            when(mockBuilder.setDefaultRequestConfig(config)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(httpClient);
        }

        // Test the method
        CloseableHttpClient client = backupHttpClient.createHttpClient();

        // Verify and assert
        assertNotNull(client);
    }

    @Test
    public void testCreateHttpClient_WithValidateCertificateFalse() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, BackrollHttpClientException {
        backupHttpClient = BackrollHttpClient.createProvider(backupHttpClient, "http://api.backup.demo.ccc:5050/api/v1", "backroll-api",
                "test-secret", false, 300, 600);

        // Mock HttpClientBuilder
        HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
        try (MockedStatic<HttpClientBuilder> utilities = Mockito.mockStatic(HttpClientBuilder.class)) {
            utilities.when(HttpClientBuilder::create).thenReturn(mockBuilder);
            when(mockBuilder.setDefaultRequestConfig(config)).thenReturn(mockBuilder);
            when(mockBuilder.setSSLSocketFactory(any(SSLConnectionSocketFactory.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(httpClient);
        }

        // Test the method
        CloseableHttpClient client = backupHttpClient.createHttpClient();

        // Verify and assert
        assertNotNull(client);
    }

    @Test
    public void NotOkBodyException_Test() {
        BackrollHttpClient.NotOkBodyException exception = new BackrollHttpClient.NotOkBodyException();
        assertNotNull(exception);
    }

    @Test
    public void get_Test_success() throws Exception {
        // Arrange
        String path = "/test";
        defaultTestHttpClient(path);

                // Act
                VirtualMachineBackupsResponse result = backupHttpClient.getParse(path,
                                VirtualMachineBackupsResponse.class);

                // Assert
                assertNotNull(result);
                verify(backupHttpClient, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
                verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
                verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
                verify(response, times(1)).close();
        }

    @Test
    public void delete_Test_success() throws Exception {
        // Arrange
        String path = "/test";
        defaultTestHttpClient(path);

        // Act
        VirtualMachineBackupsResponse result = backupHttpClient.delete(path, VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClient, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpDelete.class));
        verify(response, times(1)).close();
    }

    @Test
    public void okBody_Test_success() throws IOException, BackrollHttpClientException, NotOkBodyException {
        StatusLine statusLine = mock(StatusLine.class);
        doReturn(statusLine).when(response).getStatusLine();
        doReturn(HttpStatus.SC_OK).when(statusLine).getStatusCode();
        doReturn(new StringEntity("{\"mockKey\": \"mockValue\"}", ContentType.APPLICATION_JSON)).when(response).getEntity();
        doNothing().when(response).close();

        String result = backupHttpClient.okBody(response);
        assertNotNull(result);
    }

    @Test(expected = BackrollHttpClient.NotOkBodyException.class)
    public void okBody_Test_Error() throws IOException, BackrollHttpClientException, NotOkBodyException {
        StatusLine statusLine = mock(StatusLine.class);
        doReturn(statusLine).when(response).getStatusLine();
        doReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).when(statusLine).getStatusCode();

        backupHttpClient.okBody(response);
    }

    @Test
    public void okBody_Test_Error_KeepsStatusAndBody() throws Exception {
        String body = "{\"detail\":{\"state\":\"FAILURE\"}}";
        StatusLine statusLine = mock(StatusLine.class);
        doReturn(statusLine).when(response).getStatusLine();
        doReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).when(statusLine).getStatusCode();
        doReturn(new StringEntity(body, ContentType.APPLICATION_JSON)).when(response).getEntity();

        NotOkBodyException exception = assertThrows(NotOkBodyException.class, () -> backupHttpClient.okBody(response));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals(body, exception.getBody());
    }

    @Test
    public void getWait_PollsWhileTaskIsInProgress_Test() throws Exception {
        backupHttpClient.pollIntervalSeconds = 0;
        String success = "{\"state\":\"SUCCESS\"}";
        doReturn("{\"state\":\"PENDING\"}", "{\"state\":\"PROGRESS\"}", success).when(backupHttpClient).get("/task");

        assertEquals(success, backupHttpClient.getWait("/task"));
        verify(backupHttpClient, times(3)).get("/task");
    }

    @Test
    public void getWaitParse_Test() throws Exception {
        String path = "/test";
        defaultTestHttpClient(path);

        // Act
        VirtualMachineBackupsResponse result = backupHttpClient.getWaitParse(path, VirtualMachineBackupsResponse.class);

        // Assert
        assertNotNull(result);
        verify(backupHttpClient, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
        verify(response, times(1)).close();
    }

    @Test
    public void getWait_Test() throws Exception {
        String path = "/test";
        defaultTestHttpClient(path);

        // Act
        String result = backupHttpClient.getWait(path);

        // Assert
        assertNotNull(result);
        verify(backupHttpClient, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
        verify(httpClient, times(1)).execute(Mockito.any(HttpGet.class));
        verify(response, times(1)).close();
    }

    @Test
    public void testPost_success() throws Exception {
        // Arrange
        String path = "/test";
        JSONObject json = new JSONObject();
        defaultTestHttpClient(path);

        // Act
        VirtualMachineBackupsResponse result = backupHttpClient.post(path, json, VirtualMachineBackupsResponse.class);

                // Assert
                assertNotNull(result);
                verify(backupHttpClient, times(2)).okBody(Mockito.any(CloseableHttpResponse.class));
                verify(httpClient, times(2)).execute(Mockito.any(HttpPost.class));
                verify(response, times(1)).close();
        }

    @Test
    public void testAuthenticationFailure() throws Exception {
        defaultTestHttpClient("/test");
        doReturn(false).when(backupHttpClient).isAuthenticated();

        // Not authenticated: ensureLoggedIn() must log in, which is a single POST on the login route
        backupHttpClient.ensureLoggedIn();

        verify(httpClient, times(1)).execute(Mockito.any(HttpPost.class));
        verify(backupHttpClient, times(1)).okBody(response);
    }

    @Test
    public void testLoginFailure() throws Exception {
        doReturn(false).when(backupHttpClient).isAuthenticated();
        doThrow(new BackrollHttpClientException(new Exception("Backroll unreachable"))).when(backupHttpClient).createHttpClient();

        assertThrows(BackrollHttpClientException.class, () -> backupHttpClient.ensureLoggedIn());
    }

    @Test
    public void testLoginSuccess() throws Exception {
        doReturn(true).when(backupHttpClient).isAuthenticated();
        backupHttpClient.ensureLoggedIn();

        // Already authenticated: no login request must be sent
        Mockito.verifyNoInteractions(httpClient);
    }

    @Test
    public void getWait_ReturnsWhenStateIsNotPending_Test() throws Exception {
        // The word PENDING appears in the payload but the state is SUCCESS: there is nothing to wait for
        String body = "{\"state\":\"SUCCESS\",\"info\":\"vm-PENDING-backup\"}";
        doReturn(body).when(backupHttpClient).get("/task");

        assertEquals(body, backupHttpClient.getWait("/task"));
        verify(backupHttpClient, times(1)).get("/task");
    }

    @Test
    public void getWait_PollsUntilStateIsNotPending_Test() throws Exception {
        backupHttpClient.pollIntervalSeconds = 0;
        String pending = "{\"state\":\"PENDING\"}";
        String success = "{\"state\":\"SUCCESS\"}";
        doReturn(pending, pending, success).when(backupHttpClient).get("/task");

        assertEquals(success, backupHttpClient.getWait("/task"));
        verify(backupHttpClient, times(3)).get("/task");
    }

    @Test
    public void getWaitRestore_UsesRestoreTimeout_Test() throws Exception {
        backupHttpClient.pollIntervalSeconds = 0;
        String pending = "{\"state\":\"PENDING\"}";
        String success = "{\"state\":\"SUCCESS\"}";
        // The restore timeout is long enough to wait for as many polls as needed
        doReturn(pending, pending, pending, pending, pending, success).when(backupHttpClient).get("/task");

        assertEquals(success, backupHttpClient.getWaitRestore("/task"));
        verify(backupHttpClient, times(6)).get("/task");
    }

    @Test
    public void getWaitRestore_Timeout_Test() throws Exception {
        backupHttpClient = BackrollHttpClient.createProvider(backupHttpClient, "http://api.backup.demo.ccc:5050/api/v1", "backroll-api",
                "test-secret", true, 300, 0);
        backupHttpClient.pollIntervalSeconds = 0;
        doReturn("{\"state\":\"PENDING\"}").when(backupHttpClient).get("/task");

        assertThrows(BackrollHttpClientException.class, () -> backupHttpClient.getWaitRestore("/task"));
    }
}
