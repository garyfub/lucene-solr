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

package org.apache.solr.client.solrj.response;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Delegation Token responses
 */
public class DelegationTokenResponse extends SolrResponseBase {

  public static class Get extends DelegationTokenResponse {

    /**
     * Get the urlString to be used as the delegation token
     */
    public String getDelegationToken() {
      try {
        Map map = (Map)getResponse().get("Token");
        if (map != null) {
          return (String)map.get("urlString");
        }
      } catch (ClassCastException e) {
        throw new SolrException (SolrException.ErrorCode.SERVER_ERROR,
          "parsing error", e);
      }
      return null;
    }
  }

  public static class Renew extends DelegationTokenResponse {
    public Long getExpirationTime() {
      try {
        return (Long)getResponse().get("long");
      } catch (ClassCastException e) {
        throw new SolrException (SolrException.ErrorCode.SERVER_ERROR,
          "parsing error", e);
      }
    }
  }

  public static class Cancel extends DelegationTokenResponse {
  }

  /**
   * ResponseParser for JsonMaps.  Used for Get and Renew DelegationToken responses.
   */
  public static class JsonMapResponseParser extends ResponseParser {
    @Override
    public String getWriterType() {
      return "json";
    }

    @Override
    public NamedList<Object> processResponse(InputStream body, String encoding) {
      ObjectMapper mapper = new ObjectMapper();
      Map map = null;
      try {
        map = mapper.readValue(body, Map.class);
      } catch (IOException e) {
        throw new SolrException (SolrException.ErrorCode.SERVER_ERROR,
          "parsing error", e);
      }
      NamedList<Object> list = new NamedList<Object>();
      list.addAll(map);
      return list;
    }

    @Override
    public NamedList<Object> processResponse(Reader reader) {
      throw new RuntimeException("Cannot handle character stream");
    }

    @Override
    public String getContentType() {
      return "application/json";
    }

    @Override
    public String getVersion() {
      return "1";
    }
  }

  /**
   * ResponseParser for null Content-Type responses.  Used for Cancel DelegationToken responses.
   */
  public static class NullResponseParser extends ResponseParser {
    @Override
    public String getWriterType() {
      return null;
    }

    @Override
    public NamedList<Object> processResponse(InputStream body, String encoding) {
      // should be empty body
      try {
        int byteRead = body.read();
        if (byteRead != -1) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
            "expected empty body, got first byte of: " + byteRead);
        }
      } catch (IOException e) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
          "parsing error", e);
      }
      return new NamedList<Object>();
    }

    @Override
    public NamedList<Object> processResponse(Reader reader) {
      throw new RuntimeException("Cannot handle character stream");
    }

    @Override
    public String getContentType() {
      return null;
    }

    @Override
    public String getVersion() {
      return "1";
    }
  }
}
