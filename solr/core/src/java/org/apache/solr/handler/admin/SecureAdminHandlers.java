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
package org.apache.solr.handler.admin;

import java.io.IOException;
import java.util.EnumSet;
import org.apache.solr.core.SolrCore;
import org.apache.sentry.core.model.search.SearchModelAction;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.SecureRequestHandlerUtil;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.zookeeper.KeeperException;

/**
 * Secure version of AdminHandlers that creates Sentry-aware AdminHandlers.
 */
public class SecureAdminHandlers extends AdminHandlers {

  @Override
  protected StandardHandler[] getStandardHandlers() {
    return new StandardHandler[] {
      new StandardHandler( "luke", new SecureLukeRequestHandler() ),
      new StandardHandler( "system", new SecureSystemInfoHandler() ),
      new StandardHandler( "mbeans", new SecureSolrInfoMBeanHandler() ),
      new StandardHandler( "plugins", new SecurePluginInfoHandler() ),
      new StandardHandler( "threads", new SecureThreadDumpHandler() ),
      new StandardHandler( "properties", new SecurePropertiesRequestHandler() ),
      new StandardHandler( "logging", new SecureLoggingHandler() ),
      new StandardHandler( "file", new SecureShowFileRequestHandler() )
    };
  }

  public static class SecureLoggingHandler extends LoggingHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
      // logging handler can be used both to read and change logs
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_AND_UPDATE, false, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecureLukeRequestHandler extends LukeRequestHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, true, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecurePluginInfoHandler extends PluginInfoHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, true, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecurePropertiesRequestHandler extends PropertiesRequestHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, false, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecureShowFileRequestHandler extends ShowFileRequestHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp)
     throws IOException, KeeperException, InterruptedException {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, true, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecureSolrInfoMBeanHandler extends SolrInfoMBeanHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, true, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecureSystemInfoHandler extends SystemInfoHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
      // this may or may not have the core
      SolrCore core = req.getCore();
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, core != null, null);
      super.handleRequestBody(req, rsp);
    }
  }

  public static class SecureThreadDumpHandler extends ThreadDumpHandler {
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
      SecureRequestHandlerUtil.checkSentryAdmin(req, SecureRequestHandlerUtil.QUERY_ONLY, false, null);
      super.handleRequestBody(req, rsp);
    }
  }
}