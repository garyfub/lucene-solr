package org.apache.solr.cloud.hdfs;

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

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.lucene.util.Constants;
import org.apache.lucene.util.LuceneTestCase.Slow;
import org.apache.solr.cloud.UnloadDistributedZkTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;

@Slow
@ThreadLeakScope(Scope.NONE) // hdfs client currently leaks thread(s)
public class HdfsUnloadDistributedZkTest extends UnloadDistributedZkTest {
  private static MiniDFSCluster dfsCluster;
  
  @BeforeClass
  public static void setupClass() throws Exception {
    assumeFalse("FIXME: This test fails under Java 8 all the time, see SOLR-4711", Constants.JRE_IS_MINIMUM_JAVA8);
    
    dfsCluster = HdfsTestUtil.setupClass(new File(TEMP_DIR,
        HdfsUnloadDistributedZkTest.class.getName() + "_"
            + System.currentTimeMillis()).getAbsolutePath());
  }
  
  @AfterClass
  public static void teardownClass() throws Exception {
    HdfsTestUtil.teardownClass(dfsCluster);
    dfsCluster = null;
  }

  
  @Override
  protected String getDataDir(String dataDir) throws IOException {
    return HdfsTestUtil.getDataDir(dfsCluster, dataDir);
  }

}
