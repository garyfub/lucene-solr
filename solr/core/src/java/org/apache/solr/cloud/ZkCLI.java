package org.apache.solr.cloud;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.common.cloud.OnReconnect;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeoutException;

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

public class ZkCLI {
  
  private static final String MAKEPATH = "makepath";
  private static final String PUT = "put";
  private static final String PUT_FILE = "putfile";
  private static final String GET = "get";
  private static final String GET_FILE = "getfile";
  private static final String DOWNCONFIG = "downconfig";
  private static final String ZK_CLI_NAME = "ZkCLI";
  private static final String HELP = "help";
  private static final String LINKCONFIG = "linkconfig";
  private static final String CONFDIR = "confdir";
  private static final String CONFNAME = "confname";
  private static final String REVERSE = "reverse";
  private static final String ZKHOST = "zkhost";
  private static final String RUNZK = "runzk";
  private static final String SOLRHOME = "solrhome";
  private static final String BOOTSTRAP = "bootstrap";
  private static final String SOLR_XML = "solr.xml";
  private static final String UPCONFIG = "upconfig";
  private static final String COLLECTION = "collection";
  private static final String CLEAR = "clear";
  private static final String LIST = "list";
  private static final String CMD = "cmd";
  private static final String GETCOLLECTIONS = "getcollections";
  private static final String HOSTNAME = "hostname";
  
  /**
   * Allows you to perform a variety of zookeeper related tasks, such as:
   * 
   * Bootstrap the current configs for all collections in solr.xml.
   * 
   * Upload a named config set from a given directory.
   * 
   * Link a named config set explicity to a collection.
   * 
   * Clear ZooKeeper info.
   * 
   * If you also pass a solrPort, it will be used to start an embedded zk useful
   * for single machine, multi node tests.
   */
  public static void main(String[] args) throws InterruptedException,
      TimeoutException, IOException, ParserConfigurationException,
      SAXException, KeeperException {

    CommandLineParser parser = new PosixParser();
    Options options = new Options();
    
    options.addOption(OptionBuilder
        .hasArg(true)
        .withDescription(
            "cmd to run: " + BOOTSTRAP + ", " + UPCONFIG + ", " + DOWNCONFIG
                + ", " + LINKCONFIG + ", " + MAKEPATH + ", "+ LIST + ", " + PUT + ", " + PUT_FILE + ", "
                + GET + ", " + GET_FILE + ", " + GETCOLLECTIONS + ", " +CLEAR).create(CMD));

    Option zkHostOption = new Option("z", ZKHOST, true,
        "ZooKeeper host address");
    options.addOption(zkHostOption);
    Option solrHomeOption = new Option("s", SOLRHOME, true,
        "for " + BOOTSTRAP + ", " + RUNZK + ": solrhome location");
    options.addOption(zkHostOption);
    options.addOption(solrHomeOption);
    
    options.addOption("d", CONFDIR, true,
        "for " + UPCONFIG + ": a directory of configuration files");
    options.addOption("n", CONFNAME, true,
        "for " + UPCONFIG + ", " + LINKCONFIG + ": name of the config set");

    
    options.addOption("c", COLLECTION, true,
        "for " + LINKCONFIG + ": name of the collection");

    options.addOption("H", HOSTNAME, true,
              "for " + GETCOLLECTIONS + ": hostname of the node holding the shards");
    
    options
        .addOption(
            "r",
            RUNZK,
            true,
            "run zk internally by passing the solr run port - only for clusters on one machine (tests, dev)");
    
    options.addOption("h", HELP, false, "bring up this help page");
    
    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      
      if (line.hasOption(HELP) || !line.hasOption(ZKHOST)
          || !line.hasOption(CMD)) {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(ZK_CLI_NAME, options);
        System.out.println("Examples:");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + BOOTSTRAP + " -" + SOLRHOME + " /opt/solr");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + UPCONFIG + " -" + CONFDIR + " /opt/solr/collection1/conf" + " -" + CONFNAME + " myconf");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + DOWNCONFIG + " -" + CONFDIR + " /opt/solr/collection1/conf" + " -" + CONFNAME + " myconf");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + LINKCONFIG + " -" + COLLECTION + " collection1" + " -" + CONFNAME + " myconf");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + MAKEPATH + " /apache/solr/data.txt 'config data'");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + PUT + " /solr.conf 'conf data'");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + PUT_FILE + " /solr.xml /User/myuser/solr/solr.xml");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + GET + " /solr.xml");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + GET_FILE + " /solr.xml solr.xml.file");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + CLEAR + " /solr");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + GETCOLLECTIONS + " -" + HOSTNAME + " hostname");
        System.out.println("zkcli.sh -zkhost localhost:9983 -cmd " + LIST);
        return;
      }
      
      // start up a tmp zk server first
      String zkServerAddress = line.getOptionValue(ZKHOST);
      String solrHome = line.getOptionValue(SOLRHOME);
      
      String solrPort = null;
      if (line.hasOption(RUNZK)) {
        if (!line.hasOption(SOLRHOME)) {
          System.out
              .println("-" + SOLRHOME + " is required for " + RUNZK);
          System.exit(1);
        }
        solrPort = line.getOptionValue(RUNZK);
      }
      
      SolrZkServer zkServer = null;
      if (solrPort != null) {
        zkServer = new SolrZkServer("true", null, solrHome + "/zoo_data",
            solrHome, solrPort);
        zkServer.parseConfig();
        zkServer.start();
      }
      ZkStateReader stateReader = null;
      SolrZkClient zkClient = null;
      try {
        stateReader = new ZkStateReader(zkServerAddress, 30000, 30000);
        zkClient = stateReader.getZkClient();
        
        if (line.getOptionValue(CMD).equals(BOOTSTRAP)) {
          if (!line.hasOption(SOLRHOME)) {
            System.out.println("-" + SOLRHOME
                + " is required for " + BOOTSTRAP);
            System.exit(1);
          }
          SolrResourceLoader loader = new SolrResourceLoader(solrHome);
          solrHome = loader.getInstanceDir();

          ConfigSolr cfg = ConfigSolr.fromSolrHome(loader, solrHome);

          if(!ZkController.checkChrootPath(zkServerAddress, true)) {
            System.out.println("A chroot was specified in zkHost but the znode doesn't exist. ");
            System.exit(1);
          }

          ZkController.bootstrapConf(zkClient, cfg, solrHome);
          
        } else if (line.getOptionValue(CMD).equals(UPCONFIG)) {
          if (!line.hasOption(CONFDIR) || !line.hasOption(CONFNAME)) {
            System.out.println("-" + CONFDIR + " and -" + CONFNAME
                + " are required for " + UPCONFIG);
            System.exit(1);
          }
          String confDir = line.getOptionValue(CONFDIR);
          String confName = line.getOptionValue(CONFNAME);
          
          if(!ZkController.checkChrootPath(zkServerAddress, true)) {
            System.out.println("A chroot was specified in zkHost but the znode doesn't exist. ");
            System.exit(1);
          }
          
          ZkController.uploadConfigDir(zkClient, new File(confDir), confName);
        } else if (line.getOptionValue(CMD).equals(DOWNCONFIG)) {
          if (!line.hasOption(CONFDIR) || !line.hasOption(CONFNAME)) {
            System.out.println("-" + CONFDIR + " and -" + CONFNAME
                + " are required for " + DOWNCONFIG);
            System.exit(1);
          }
          String confDir = line.getOptionValue(CONFDIR);
          String confName = line.getOptionValue(CONFNAME);
          
          ZkController.downloadConfigDir(zkClient, confName, new File(confDir));
        } else if (line.getOptionValue(CMD).equals(LINKCONFIG)) {
          if (!line.hasOption(COLLECTION) || !line.hasOption(CONFNAME)) {
            System.out.println("-" + CONFDIR + " and -" + CONFNAME
                + " are required for " + LINKCONFIG);
            System.exit(1);
          }
          String collection = line.getOptionValue(COLLECTION);
          String confName = line.getOptionValue(CONFNAME);
          
          ZkController.linkConfSet(zkClient, collection, confName);
        } else if (line.getOptionValue(CMD).equals(LIST)) {
          zkClient.printLayoutToStdOut();
        } else if (line.getOptionValue(CMD).equals(GETCOLLECTIONS)) {
           stateReader.updateClusterState(true);
           String hostname = line.hasOption(HOSTNAME) ? line.getOptionValue(HOSTNAME) : null;
           for (String collection : stateReader.getClusterState().getCollections()) {
             for (Slice slice : stateReader.getClusterState().getActiveSlicesMap(collection).values()) {
               for (Replica replica : slice.getReplicas()) {
                 String nodeName = replica.getNodeName().split(":")[0];
                 if (hostname == null) {
                   System.out.printf(nodeName + "\t");
                 } else if (!hostname.equals(nodeName)) {
                   continue;
                 }
                 System.out.printf("    <core schema=\"schema.xml\" loadOnStartup=\"true\" shard=\"%s\" instanceDir=\"%s/\" transient=\"false\" name=\"%s\" config=\"solrconfig.xml\" collection=\"%s\"/>\n",
                                   replica.getStr("shard"),
                                   replica.getStr("core"),
                                   replica.getStr("core"),
                                   replica.getStr("collection"));
               }
             }
           }
        } else if (line.getOptionValue(CMD).equals(CLEAR)) {
          List arglist = line.getArgList();
          if (arglist.size() != 1) {
            System.out.println("-" + CLEAR + " requires one arg - the path to clear");
            System.exit(1);
          }
          zkClient.clean(arglist.get(0).toString());
        } else if (line.getOptionValue(CMD).equals(MAKEPATH)) {
          List arglist = line.getArgList();
          if (arglist.size() != 1) {
            System.out.println("-" + MAKEPATH + " requires one arg - the path to make");
            System.exit(1);
          }
          zkClient.makePath(arglist.get(0).toString(), true);
        } else if (line.getOptionValue(CMD).equals(PUT)) {
          List arglist = line.getArgList();
          if (arglist.size() != 2) {
            System.out.println("-" + PUT + " requires two args - the path to create and the data string");
            System.exit(1);
          }
          zkClient.create(arglist.get(0).toString(), arglist.get(1).toString().getBytes("UTF-8"), CreateMode.PERSISTENT, true);
        } else if (line.getOptionValue(CMD).equals(PUT_FILE)) {
          List arglist = line.getArgList();
          if (arglist.size() != 2) {
            System.out.println("-" + PUT_FILE + " requires two args - the path to create in ZK and the path to the local file");
            System.exit(1);
          }
          InputStream is = new FileInputStream(arglist.get(1).toString());
          try {
            zkClient.create(arglist.get(0).toString(), IOUtils.toByteArray(is), CreateMode.PERSISTENT, true);
          } finally {
            IOUtils.closeQuietly(is);
          }
        } else if (line.getOptionValue(CMD).equals(GET)) {
          List arglist = line.getArgList();
          if (arglist.size() != 1) {
            System.out.println("-" + GET + " requires one arg - the path to get");
            System.exit(1);
          }
          byte [] data = zkClient.getData(arglist.get(0).toString(), null, null, true);
          System.out.println(new String(data, "UTF-8"));
        } else if (line.getOptionValue(CMD).equals(GET_FILE)) {
          List arglist = line.getArgList();
          if (arglist.size() != 2) {
            System.out.println("-" + GET_FILE + "requires two args - the path to get and the file to save it to");
            System.exit(1);
          }
          byte [] data = zkClient.getData(arglist.get(0).toString(), null, null, true);
          FileUtils.writeByteArrayToFile(new File(arglist.get(1).toString()), data);
        }
      } finally {
        if (solrPort != null) {
          zkServer.stop();
        }
        if (zkClient != null) {
          zkClient.close();
        }
      }
    } catch (ParseException exp) {
      System.out.println("Unexpected exception:" + exp.getMessage());
    }
    
  }
}
