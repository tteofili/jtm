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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.Issue;
import com.github.tteofili.jtm.tm.EmbeddingsTopicModel;
import com.github.tteofili.jtm.tm.TopicModel;
/**
 * Tool for analyzing Jira issues exported as XML.
 *
 */
public class JiraAnalysisTool implements AnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(JiraAnalysisTool.class);

  private final int epochs;

  private final int layerSize;

  private final int topN;

  private final boolean hierarchicalVectors;

  private final boolean includeComments;

  private final boolean index;

  public JiraAnalysisTool(int epochs,
                          int layerSize,
                          int topN,
                          boolean hierarchicalVectors,
                          boolean includeComments,
                          boolean index) {
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    this.index = index;
  }

  @Override
  public void analyze(Feed feed) throws Exception {
    log.info("Analysing feed {} ({})",
             feed.getIssues().getTitle(),
             feed.getIssues().getBuildInfo());

    Collection<Issue> issues = feed.getIssues().getIssues();

    log.info("{} issues parsed", issues.size());

    TopicModel topicModel = new EmbeddingsTopicModel(epochs, layerSize, hierarchicalVectors, includeComments);
    topicModel.fit(issues);

    for (Issue issue : feed.getIssues().getIssues()) {
      List<String> topics = new ArrayList<>(topicModel.extractTopics(topN, issue.getKey().getValue()));
      issue.setTopics(topics);
      log.info("{} : {}", issue.getTitle(), topics);
    }

    if (index) {
      Settings settings = Settings.builder()
          .put("cluster.name", "elasticsearch")
          .put("xpack.security.user", "elastic:Q96HdxDpHMH2p9TrMvqA")
          .build();
      TransportClient client = new PreBuiltXPackTransportClient(settings);
      client.addTransportAddresses(new TransportAddress(InetAddress.getByName("localhost"), 9300));

      String indexName = feed.getIssues().getTitle().toLowerCase();

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
        for (Issue issue : issues) {

          XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
              .startObject()
              .field("id", issue.getKey().getValue())
              .field("assignee", issue.getAssignee())
              .field("components", issue.getComponents())
              .field("created", issue.getCreated())
              .field("description", issue.getDescription())
              .field("labels", issue.getLabels())
              .field("link", issue.getLink())
              .field("projectId", issue.getProject().getName())
              .field("reporter", issue.getReporter())
              .field("resolution", issue.getResolution())
              .field("summary", issue.getSummary())
              .field("title", issue.getTitle())
              .field("topics", issue.getTopics())
              .field("typeId", issue.getType())
              .field("updated", issue.getUpdated())
              .endObject();

          IndexResponse response = client.prepareIndex(indexName, type, issue.getKey().getValue())
              .setSource(jsonBuilder)
              .get();

          log.info("indexing {} : {}", issue.getKey().getValue(), response.status());
        }
      } finally {
        client.close();
      }
    }
  }

}
