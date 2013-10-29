package org.apache.solr.update.processor;

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

import org.apache.solr.common.SolrException;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.sentry.SentryIndexAuthorizationSingleton;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.MergeIndexesCommand;
import org.apache.solr.update.RollbackUpdateCommand;
import org.apache.sentry.core.model.search.SearchModelAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;

public class UpdateIndexAuthorizationProcessor extends UpdateRequestProcessor {

  private SolrQueryRequest req;

  public UpdateIndexAuthorizationProcessor(SolrQueryRequest req,
      SolrQueryResponse rsp, UpdateRequestProcessor next) {
    super(next);
    this.req = req;
  }

  public void authorizeCollectionAction() throws SolrException {
    SentryIndexAuthorizationSingleton.getInstance().authorizeCollectionAction(
      req, EnumSet.of(SearchModelAction.UPDATE));
  }

  @Override
  public void processAdd(AddUpdateCommand cmd) throws IOException {
    authorizeCollectionAction();
    super.processAdd(cmd);
  }

  @Override
  public void processDelete(DeleteUpdateCommand cmd) throws IOException {
    authorizeCollectionAction();
    super.processDelete(cmd);
  }

  @Override
  public void processMergeIndexes(MergeIndexesCommand cmd) throws IOException {
    authorizeCollectionAction();
    super.processMergeIndexes(cmd);
  }

  @Override
  public void processCommit(CommitUpdateCommand cmd) throws IOException
  {
    authorizeCollectionAction();
    super.processCommit(cmd);
  }

  @Override
  public void processRollback(RollbackUpdateCommand cmd) throws IOException
  {
    authorizeCollectionAction();
    super.processRollback(cmd);
  }

  @Override
  public void finish() throws IOException {
    authorizeCollectionAction();
    super.finish();
  }
}
