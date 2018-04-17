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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.TypeTokenFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.aggregation.Topics;
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

  private final String analyzerType;

  public JiraAnalysisTool(int epochs,
                          int layerSize,
                          int topN,
                          boolean hierarchicalVectors,
                          boolean includeComments,
                          String analyzerType) {
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    this.analyzerType = analyzerType;
  }

  @Override
  public Topics analyze(Feed feed) throws Exception {
    log.info("Analysing feed {} ({})",
             feed.getIssues().getTitle(),
             feed.getIssues().getBuildInfo());

    Collection<Issue> issues = feed.getIssues().getIssues();

    log.info("{} issues parsed", issues.size());

    Analyzer analyzer ;

    if ("opennlp".equalsIgnoreCase(analyzerType)) {
      String sentenceModel = "en-sent.bin";
      String tokenizerModel = "en-token.bin";
      String posModel = "en-pos-maxent.bin";
      String chunkerModel = "en-chunker.bin";
      analyzer = CustomAnalyzer.builder()
          .addCharFilter(HTMLStripCharFilterFactory.class)
          .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
              sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
          .addTokenFilter(LowerCaseFilterFactory.class)
          .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
          .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL, chunkerModel)
          .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
          .build();
    } else if ("simple".equalsIgnoreCase(analyzerType)) {

    String revisionsPattern = "r\\d+";
      analyzer = CustomAnalyzer.builder()
          .addCharFilter(HTMLStripCharFilterFactory.class)
          .withTokenizer(ClassicTokenizerFactory.class)
          .addTokenFilter(LowerCaseFilterFactory.class)
          .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern, "replacement", "", "replace", "all")
          .build();
    } else {
      throw new Exception("undefined Analyzer of type '" + analyzerType + "'");
    }

    TopicModel topicModel = new EmbeddingsTopicModel(epochs, layerSize, hierarchicalVectors, includeComments, analyzer);
    topicModel.fit(issues);

    Topics topics = new Topics();

    for (Issue issue : feed.getIssues().getIssues()) {
      List<String> extractedTopics = new ArrayList<>(topicModel.extractTopics(topN, issue.getKey().getValue()));
      issue.setTopics(extractedTopics);

      log.info("Topics {} extracted from issue '{}'", extractedTopics, issue.getTitle());

      topics.add(issue);
    }

    return topics;
  }

}
