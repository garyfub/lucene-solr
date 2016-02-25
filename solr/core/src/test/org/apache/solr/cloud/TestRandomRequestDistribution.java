package org.apache.solr.cloud;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.Create;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.servlet.SolrDispatchFilter;


@SolrTestCaseJ4.SuppressSSL
public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase {

  List<String> nodeNames = new ArrayList<>(3);
  private Create create;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    shardCount = 3;
  }
  
  public void doTest() throws Exception {
    waitForThingsToLevelOut(30);

    for (CloudJettyRunner cloudJetty : cloudJettys) {
      nodeNames.add(cloudJetty.nodeName);
    }
    assertEquals(3, nodeNames.size());

    testQueryAgainstDownReplica();
  }

  /**
   * Asserts that requests against a collection are only served by a 'active' local replica
   */
  private void testQueryAgainstDownReplica() throws Exception {

    log.info("Creating collection 'football' with 1 shard and 2 replicas");
    create = new CollectionAdminRequest.Create();
    create.setCollectionName("football");
    create.setNumShards(1);
    create.setReplicationFactor(2);
    create.setCreateNodeSet(nodeNames.get(0) + ',' + nodeNames.get(1));
    create.process(cloudClient);

    waitForRecoveriesToFinish("football", true);

    cloudClient.getZkStateReader().updateClusterState(true);

    Replica leader = null;
    Replica notLeader = null;

    Collection<Replica> replicas = cloudClient.getZkStateReader().getClusterState().getSlice("football", "shard1").getReplicas();
    for (Replica replica : replicas) {
      if (replica.getStr(ZkStateReader.LEADER_PROP) != null) {
        leader = replica;
      } else {
        notLeader = replica;
      }
    }

    //Simulate a replica being in down state.
    ZkNodeProps m = new ZkNodeProps(Overseer.QUEUE_OPERATION, "state",
        ZkStateReader.BASE_URL_PROP, notLeader.getStr(ZkStateReader.BASE_URL_PROP),
        ZkStateReader.NODE_NAME_PROP, notLeader.getStr(ZkStateReader.NODE_NAME_PROP),
        ZkStateReader.COLLECTION_PROP, "football",
        ZkStateReader.SHARD_ID_PROP, "shard1",
        ZkStateReader.CORE_NAME_PROP, notLeader.getStr(ZkStateReader.CORE_NAME_PROP),
        ZkStateReader.ROLES_PROP, "",
        ZkStateReader.STATE_PROP, ZkStateReader.DOWN);

    log.info("Forcing {} to go into 'down' state", notLeader.getStr(ZkStateReader.CORE_NAME_PROP));
    DistributedQueue q = Overseer.getInQueue(cloudClient.getZkStateReader().getZkClient());
    q.offer(ZkStateReader.toJSON(m));

    super.verifyReplicaStatus(cloudClient.getZkStateReader(), "football", "shard1", notLeader.getName(), ZkStateReader.DOWN);

    //Query against the node which hosts the down replica

    String baseUrl = notLeader.getStr(ZkStateReader.BASE_URL_PROP);
    if (!baseUrl.endsWith("/")) baseUrl += "/";
    String path = baseUrl + "football";
    log.info("Firing query against path=" + path);
    HttpSolrServer client = new HttpSolrServer(path);
    client.setSoTimeout(5000);
    client.setConnectionTimeout(2000);

    client.query(new SolrQuery("*:*"));
    client.shutdown();

    //Test to see if the query got forwarded to the active replica or not.
    for (JettySolrRunner jetty : jettys) {
      CoreContainer container = ((SolrDispatchFilter) jetty.getDispatchFilter().getFilter()).getCores();
      for (SolrCore core : container.getCores()) {
        if (core.getName().equals(leader.getStr(ZkStateReader.CORE_NAME_PROP))) {
          SolrRequestHandler select = core.getRequestHandler("");
          long c = (long) select.getStatistics().get("requests");
          assertEquals(core.getName() + " should have got 1 request", 1, c);
          break;
        }
      }
    }

  }
}