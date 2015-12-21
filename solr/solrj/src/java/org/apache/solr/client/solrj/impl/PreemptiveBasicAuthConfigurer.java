/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.client.solrj.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * HttpClientConfigurer implementation providing support for preemptive Http Basic authentication
 * scheme.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PreemptiveBasicAuthConfigurer extends HttpClientConfigurer {
  /**
   * A system property used to specify a properties file containing default parameters used for
   * creating a HTTP client. This is specifically useful for configuring the HTTP basic auth
   * credentials (i.e. username/password). The name of the property must match the relevant
   * Solr config property name.
   */
  public static final String SYS_PROP_HTTP_CLIENT_CONFIG = "solr.httpclient.config";
  private static SolrParams defaultParams;

  static {
    String configFile = System.getProperty(SYS_PROP_HTTP_CLIENT_CONFIG);
    if(configFile != null) {
      try {
        Properties defaultProps = new Properties();
        defaultProps.load(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));
        defaultParams = new MapSolrParams(new HashMap(defaultProps));
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to read the Http client config file", e);
      }
    }
  }

  /**
   * This method enables configuring system wide defaults (apart from using a config file based approach).
   */
  public static void setDefaultSolrParams(SolrParams params) {
    defaultParams = params;
  }

  @Override
  protected void configure(DefaultHttpClient httpClient, SolrParams config) {
    config = SolrParams.wrapDefaults(config, defaultParams);
    final String basicAuthUser = config.get(HttpClientUtil.PROP_BASIC_AUTH_USER);
    final String basicAuthPass = config.get(HttpClientUtil.PROP_BASIC_AUTH_PASS);
    if(basicAuthUser == null || basicAuthPass == null) {
      throw new IllegalArgumentException("username & password must be specified with " + getClass().getName());
    }
    super.configure(httpClient, config);
    httpClient.addRequestInterceptor(new PreemptiveAuth(new BasicScheme()), 0);
  }
}