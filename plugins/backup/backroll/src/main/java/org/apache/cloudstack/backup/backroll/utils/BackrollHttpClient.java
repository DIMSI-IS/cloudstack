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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.api.LoginApiResponse;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackrollHttpClient {
    private static final String AUTH_LOGIN = "/auth/cloudstack/login";
    private static final String AUTH_TEST = "/auth/cloudstack/test";
    private static final int DEFAULT_WAIT_SECONDS = 120;
    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 10;

    // Backroll may add fields to its answers: the ones the plugin does not read must not make the parsing fail
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private URI apiURI;
    private String backrollToken = null;
    private String appname = null;
    private String password = null;
    private RequestConfig config = null;
    private boolean validateCertificate = false;
    private int restoreWaitSeconds = DEFAULT_WAIT_SECONDS;
    // Not private so that tests can avoid really sleeping between two polls
    int pollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;

    private Logger logger = LogManager.getLogger(BackrollClient.class);

    public static class BackrollHttpClientException extends Exception {
        public BackrollHttpClientException(Throwable cause) {
            super(cause);
        }
    }

    public static BackrollHttpClient createProvider(BackrollHttpClient backrollHttpClientProvider,
            final String url, final String appname, final String password,
            final boolean validateCertificate, final int timeout,
            final int restoreTimeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        backrollHttpClientProvider.apiURI = new URI(url);
        backrollHttpClientProvider.appname = appname;
        backrollHttpClientProvider.password = password;
        backrollHttpClientProvider.validateCertificate = validateCertificate;
        backrollHttpClientProvider.restoreWaitSeconds = restoreTimeout;

        backrollHttpClientProvider.config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        return backrollHttpClientProvider;
    }

    protected CloseableHttpClient createHttpClient() throws BackrollHttpClientException {
        try {
            if (!validateCertificate) {
                final SSLContext sslContext = SSLUtils.getSSLContext();
                sslContext.init(null, new X509TrustManager[] { new TrustAllManager() }, new SecureRandom());

                final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext,
                        NoopHostnameVerifier.INSTANCE);

                return HttpClientBuilder.create()
                        .setDefaultRequestConfig(config)
                        .setSSLSocketFactory(factory)
                        .build();

            } else {
                return HttpClientBuilder
                        .create()
                        .setDefaultRequestConfig(config)
                        .build();
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    private String getApiUrl(String path) {
        final String url = apiURI.toString() + path;
        logger.debug("getApiUrl {}", url);
        return url;
    }

    private void setRequestHeaders(HttpRequestBase request) {
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
    }

    public static class NotOkBodyException extends Exception {
        private final int statusCode;
        private final String body;

        public NotOkBodyException() {
            this(0, null);
        }

        public NotOkBodyException(final int statusCode, final String body) {
            super("Unexpected HTTP status " + statusCode);
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        /**
         * @return the body of the response, if it could be read. Backroll puts the state of a failed task in it.
         */
        public String getBody() {
            return body;
        }
    }

    private String readBodyQuietly(final CloseableHttpResponse response) {
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        try {
            return EntityUtils.toString(entity);
        } catch (Exception exception) {
            logger.debug("Unable to read the body of the response", exception);
            return null;
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    public String okBody(final CloseableHttpResponse response) throws BackrollHttpClientException, NotOkBodyException {
        try {
            final int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("okBody : statusCode {}", statusCode);

            // final String reasonPhrase = response.getStatusLine().getReasonPhrase();

            switch (statusCode) {
                case HttpStatus.SC_OK:
                case HttpStatus.SC_ACCEPTED:
                    HttpEntity bodyEntity = response.getEntity();
                    try {
                        logger.debug("bodyentity : {}", bodyEntity);
                        final String result = EntityUtils.toString(bodyEntity);
                        logger.debug("bodyentity : result {}", result);
                        return result;
                    } finally {
                        EntityUtils.consumeQuietly(bodyEntity);
                    }
                default:
                    throw new NotOkBodyException(statusCode, readBodyQuietly(response));
            }
        } catch (NotOkBodyException exception) {
            throw exception;
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    private <T> T parseValue(String json, Class<T> classOfT) throws BackrollHttpClientException {
        try {
            logger.debug("parseValue {}", json);
            return OBJECT_MAPPER.readValue(json, classOfT);
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    private void login(final String appname, final String appsecret) throws BackrollHttpClientException {
        try {
            logger.debug("Backroll client -  start login");

            final HttpPost request = new HttpPost(getApiUrl(AUTH_LOGIN));
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            final JSONObject jsonBody = new JSONObject();
            jsonBody.put("app_id", appname);
            jsonBody.put("app_secret", appsecret);

            final String jsonString = jsonBody.toString();
            request.setEntity(new StringEntity(jsonString, ContentType.APPLICATION_JSON));

            try (final CloseableHttpClient httpClient = createHttpClient()) {
                try (final CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                    final LoginApiResponse response = parseValue(okBody(httpResponse), LoginApiResponse.class);
                    final String token = response.accessToken;
                    if (StringUtils.isEmpty(token)) {
                        throw new CloudRuntimeException("Backroll token is not available to perform API requests");
                    }
                    this.backrollToken = token;
                }
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    boolean isAuthenticated() throws BackrollHttpClientException {
        try {
            if (StringUtils.isEmpty(backrollToken)) {
                logger.debug("isAuthenticated : token is empty : {}", backrollToken);
                return false;
            }

            final HttpGet request = new HttpGet(getApiUrl(AUTH_TEST));
            setRequestHeaders(request);

            try (CloseableHttpClient httpClient = createHttpClient()) {
                try (final CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                    okBody(httpResponse);
                    return true;
                } catch (NotOkBodyException exception) {
                    logger.error(exception);
                }
            }

            return false;
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    public void ensureLoggedIn() throws BackrollHttpClientException {
        if (!isAuthenticated()) {
            login(appname, password);
        }
    }

    public <T> T post(
            final String path,
            final JSONObject json,
            Class<T> classOfT) throws BackrollHttpClientException {
        try {
            ensureLoggedIn();

            final HttpPost request = new HttpPost(getApiUrl(path));
            setRequestHeaders(request);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            if (json != null) {
                logger.debug("post JSON {}", json.toString());
                request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
            }

            try (CloseableHttpClient httpClient = createHttpClient()) {
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    return parseValue(okBody(response), classOfT);
                }
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    public String get(String path) throws BackrollHttpClientException {
        try {
            ensureLoggedIn();

            final HttpGet request = new HttpGet(getApiUrl(path));
            setRequestHeaders(request);

            try (CloseableHttpClient httpClient = createHttpClient()) {
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    return okBody(response);
                }
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    public <T> T getParse(String path, Class<T> classOfT) throws BackrollHttpClientException {
        return parseValue(get(path), classOfT);
    }

    public <T> T delete(String path, Class<T> classOfT) throws BackrollHttpClientException {
        try {
            ensureLoggedIn();

            final HttpDelete request = new HttpDelete(getApiUrl(path));
            setRequestHeaders(request);

            try (final CloseableHttpClient httpClient = createHttpClient()) {
                try (final CloseableHttpResponse response = httpClient.execute(request)) {
                    return parseValue(okBody(response), classOfT);
                }
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    public String getWait(String path) throws BackrollHttpClientException {
        return getWait(path, DEFAULT_WAIT_SECONDS);
    }

    /**
     * Same as {@link #getWait(String)} but waits as long as the restore timeout, since a restore can be much longer than other tasks.
     */
    public String getWaitRestore(String path) throws BackrollHttpClientException {
        return getWait(path, restoreWaitSeconds);
    }

    private String getWait(String path, int maxWaitSeconds) throws BackrollHttpClientException {
        try {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(maxWaitSeconds);

            while (true) {
                final String body = get(path);
                if (!TaskState.isRunning(TaskState.of(body))) {
                    logger.debug("waitGetWithoutParseResponse : result {}", body);
                    return body;
                }

                if (System.nanoTime() >= deadline) {
                    throw new Exception("Timeout of " + maxWaitSeconds + " seconds reached while waiting for a non-pending response.");
                }

                TimeUnit.SECONDS.sleep(pollIntervalSeconds);
            }
        } catch (Exception exception) {
            logger.error(exception);
            throw new BackrollHttpClientException(exception);
        }
    }

    public <T> T getWaitParse(String path, Class<T> classOfT) throws BackrollHttpClientException {
        return parseValue(getWait(path), classOfT);
    }
}
