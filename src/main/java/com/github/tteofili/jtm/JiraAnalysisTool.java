/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.github.tteofili.jtm.tm.EmbeddingsTopicModel;
import com.github.tteofili.jtm.tm.TopicModel;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.IVersionProvider;

/**
 * Tool for analyzing Jira issues exported as XML.
 *
 */
public class JiraAnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(JiraAnalysisTool.class);

  private final File exportedJiraFeed;

  private final int epochs;

  private final int layerSize;

  private final int topN;

  private final boolean hierarchicalVectors;

  private final boolean includeComments;

  private final boolean index;

  public JiraAnalysisTool(File exportedJiraFeed,
                          int epochs,
                          int layerSize,
                          int topN,
                          boolean hierarchicalVectors,
                          boolean includeComments,
                          boolean index) {
    this.exportedJiraFeed = exportedJiraFeed;
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    this.index = index;
  }

  public void execute() throws IOException, XMLStreamException {

    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser(exportedJiraFeed);
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();

    log.info("{} issues parsed", issues.size());

    TopicModel topicModel = new EmbeddingsTopicModel(epochs, layerSize, hierarchicalVectors, includeComments);
    topicModel.fit(issues.values());

    for (JiraIssue issue : issues.values()) {
      Collection<String> topics = topicModel.extractTopics(topN, issue.getId());
      issue.addTopics(topics);
      log.info("{} : {}", issue.getTitle(), topics);
    }

    if (index) {
      Settings settings = Settings.builder()
          .put("cluster.name", "elasticsearch")
          .put("xpack.security.user", "elastic:Q96HdxDpHMH2p9TrMvqA")
          .build();
      TransportClient client = new PreBuiltXPackTransportClient(settings)
          .addTransportAddresses(new TransportAddress(InetAddress.getByName("localhost"), 9300));

      String indexName = issues.values().iterator().next().getProjectId().toLowerCase();

//      try {
//        BulkByScrollResponse bulk =
//            DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
//                .filter(QueryBuilders.matchAllQuery())
//                .source(indexName)
//                .get();
//
//        long deleted = bulk.getDeleted();
//        log.info("{} deletion : {}", indexName, deleted);
//      } catch (Throwable t) {
//        // do nothing
//      }

      String type = "doc";
      try {
        for (JiraIssue issue : issues.values()) {

          XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
              .startObject()
              .field("id", issue.getId())
              .field("assignee", issue.getAssignee())
              .field("component", issue.getComponent())
              .field("created", issue.getCreated())
              .field("description", issue.getDescription())
              .field("labels", issue.getLabels())
              .field("link", issue.getLink())
              .field("projectId", issue.getProjectId())
              .field("reporter", issue.getReporter())
              .field("resolution", issue.getResolution())
              .field("summary", issue.getSummary())
              .field("title", issue.getTitle())
              .field("topics", issue.getTopics())
              .field("typeId", issue.getTypeId())
              .field("updated", issue.getUpdated())
              .endObject();

          IndexResponse response = client.prepareIndex(indexName, type, issue.getId())
              .setSource(jsonBuilder)
              .get();

          log.info("indexing {} : {}", issue.getId(), response.status());
        }
      } finally {
        client.close();
      }
    }
  }


  public static final class JtmVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
      return new String[] {
          "                         ''~``",
          "                        ( o o )",
          "+------------------.oooO--(_)--Oooo.------------------+",
          String.format("%s v%s (built on %s)",
              System.getProperty("project.artifactId"),
              System.getProperty("project.version"),
              System.getProperty("build.timestamp")),
          String.format("Java version: %s, vendor: %s",
              System.getProperty("java.version"),
              System.getProperty("java.vendor")),
          String.format("Java home: %s", System.getProperty("java.home")),
          String.format("Default locale: %s_%s, platform encoding: %s",
              System.getProperty("user.language"),
              System.getProperty("user.country"),
              System.getProperty("sun.jnu.encoding")),
          String.format("OS name: \"%s\", version: \"%s\", arch: \"%s\", family: \"%s\"",
              System.getProperty("os.name"),
              System.getProperty("os.version"),
              System.getProperty("os.arch"),
              getOsFamily()),
          "                     .oooO                            ",
          "                     (   )   Oooo.                    ",
          "+---------------------\\ (----(   )--------------------+",
          "                       \\_)    ) /",
          "                             (_/"
      };
    }

    private static final String getOsFamily() {
      String osName = System.getProperty("os.name").toLowerCase();
      String pathSep = System.getProperty("path.separator");

      if (osName.indexOf("windows") != -1) {
        return "windows";
      } else if (osName.indexOf("os/2") != -1) {
        return "os/2";
      } else if (osName.indexOf("z/os") != -1 || osName.indexOf("os/390") != -1) {
        return "z/os";
      } else if (osName.indexOf("os/400") != -1) {
        return "os/400";
      } else if (pathSep.equals(";")) {
        return "dos";
      } else if (osName.indexOf("mac") != -1) {
        if (osName.endsWith("x")) {
          return "mac"; // MACOSX
        }
        return "unix";
      } else if (osName.indexOf("nonstop_kernel") != -1) {
        return "tandem";
      } else if (osName.indexOf("openvms") != -1) {
        return "openvms";
      } else if (pathSep.equals(":")) {
        return "unix";
      }

      return "undefined";
    }

  }

  private static final class ShutDownHook extends Thread {

    private final long start = System.currentTimeMillis();

    public ShutDownHook() {
      super("shutdown-hook");
    }

    public void run() {
      // format the uptime string

      Formatter uptime = new Formatter();
      uptime.format("Total uptime:");

      long uptimeInSeconds = (System.currentTimeMillis() - start) / 1000;
      final long hours = uptimeInSeconds / 3600;

      if (hours > 0) {
        uptime.format(" %s hour%s", hours, (hours > 1 ? "s" : ""));
      }

      uptimeInSeconds = uptimeInSeconds - (hours * 3600);
      final long minutes = uptimeInSeconds / 60;

      if (minutes > 0) {
        uptime.format(" %s minute%s", minutes, (minutes > 1 ? "s" : ""));
      }

      uptimeInSeconds = uptimeInSeconds - (minutes * 60);

      if (uptimeInSeconds > 0) {
        uptime.format(" %s second%s", uptimeInSeconds, (uptimeInSeconds > 1 ? "s" : ""));
      }

      log.info(uptime.toString());
      uptime.close();

      log.info("Finished at: {}", new Date());

      final Runtime runtime = Runtime.getRuntime();
      final int megaUnit = 1024 * 1024;
      log.info("Final Memory: {}M/{}M",
          (runtime.totalMemory() - runtime.freeMemory()) / megaUnit,
          runtime.totalMemory() / megaUnit);

      log.info("                     .oooO                            ");
      log.info("                     (   )   Oooo.                    ");
      log.info("+---------------------\\ (----(   )--------------------+");
      log.info("                       \\_)    ) /");
      log.info("                             (_/");
    }

  }

}
