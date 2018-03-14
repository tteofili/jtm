/*
 * Copyright 2018 Tommaso Teofili
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

/**
 * Tool for analyzing Jira issues exported as XML.
 * This tool does topic modelling based on word2vec and paragraph vectors.
 *
 */
public class JiraAnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(JiraAnalysisTool.class);

  private static final String[] stopTags = new String[] {"CD", "VB", "RB", "JJ", "VBN", "VBG", ".", "JJS", "FW", "VBD"};

  public static void main(String[] args) throws IOException, XMLStreamException {

    String pathToJiraExport = readFrom(args, 0);
    int epochs = Integer.parseInt(readFrom(args, 1));
    int layerSize = Integer.parseInt(readFrom(args, 2));
    int topN = Integer.parseInt(readFrom(args, 3));
    boolean hierarchicalVectors = Boolean.parseBoolean(readFrom(args, 4));
    boolean includeComments = Boolean.parseBoolean(readFrom(args, 5));
    boolean index = Boolean.parseBoolean(readFrom(args, 6));

    Arrays.sort(stopTags);
    InputStream posStream = JiraAnalysisTool.class.getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger = new POSTaggerME(posModel);

    log.info("Command line arguments {}", Arrays.toString(args));

    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser(pathToJiraExport);
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();

    log.info("{} issues parsed", issues.size());

    JiraIterator iterator = new JiraIterator(issues, includeComments);

//    String sentenceModel = "en-sent.bin";
//    String tokenizerModel = "en-token.bin";
//    String posModel = "en-pos-maxent.bin";
//    String chunkerModel = "en-chunker.bin";
//    Analyzer openNLPAnalyzer = CustomAnalyzer.builder()
//        .addCharFilter(HTMLStripCharFilterFactory.class)
//        .withTokenizer(OpenNLPTokenizerFactory.class,OpenNLPTokenizerFactory.SENTENCE_MODEL,
//            sentenceModel,OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
//        .addTokenFilter(OpenNLPPOSFilterFactory.class,OpenNLPPOSFilterFactory.POS_TAGGER_MODEL,posModel)
//        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL, chunkerModel)
//        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
//        .build();

    String revisionsPattern = "r\\d+";
    Analyzer simpleAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(ClassicTokenizerFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern, "replacement", "", "replace", "all")
        .build();

    TokenizerFactory tf = new LuceneTokenizerFactory(simpleAnalyzer);

    Word2Vec issuesWord2vec = new Word2Vec.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .elementsLearningAlgorithm(new SkipGram<>())
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .build();
    issuesWord2vec.fit();

    iterator.reset();

    ParagraphVectors issuesParagraphVectors = new ParagraphVectors.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .useExistingWordVectors(issuesWord2vec)
        .build();
    issuesParagraphVectors.fit();

    iterator.reset();

    ParagraphVectors paragraphVectors;
    if (hierarchicalVectors) {
      Par2Hier par2Hier = new Par2Hier(issuesParagraphVectors, Par2HierUtils.Method.SUM, 3);
      par2Hier.fit();
      paragraphVectors = par2Hier;
    } else {
      paragraphVectors = issuesParagraphVectors;
    }

    for (Map.Entry<String, JiraIssue> entry : issues.entrySet()) {
      JiraIssue issue = entry.getValue();
      Collection<String> topics = getTopics(topN, issues, issuesWord2vec, paragraphVectors, issue, tagger);
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

  private static Collection<String> getTopics(int topN, Map<String, JiraIssue> issues, Word2Vec issuesWord2vec, ParagraphVectors paragraphVectors, JiraIssue issue, POSTaggerME tagger) {
    INDArray issueVector = paragraphVectors.getLookupTable().vector(issue.getId());
    Collection<String> nearestWords = issuesWord2vec.wordsNearest(issueVector, topN);

    log.debug("issue {} : {}", issue.getTitle(), nearestWords);

    Collection<String> nearestLabelsWords = new LinkedList<>();
    for (String label : paragraphVectors.nearestLabels(issueVector, topN)) {
      INDArray nearestIssueVector = paragraphVectors.getLookupTable().vector(label);
      nearestLabelsWords.addAll(issuesWord2vec.wordsNearest(nearestIssueVector, topN));
    }

    log.debug("nearest issues : {}", nearestLabelsWords);

    nearestWords.addAll(nearestLabelsWords);

    INDArray wordVectorsMean = issuesWord2vec.getWordVectorsMean(nearestWords);
    Collection<String> topics = issuesWord2vec.wordsNearest(wordVectorsMean, topN);

    Collection<String> toRemove = new LinkedList<>();
    for (String t : topics) {
      String[] tags = tagger.tag(new String[] {t});
      String tag = tags[0];
      boolean stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
      if (stopTag || issues.containsKey(t.toUpperCase()) || issues.containsKey(t) || StringUtils.isNumeric(t)) {
        toRemove.add(t);
      }
    }
    topics.removeAll(toRemove);

    log.debug("topics : {}", topics);

    return topics;
  }

  private static String readFrom(String[] args, int i) {
    // resource, epochs, layerSize, clusterCount, maxIterationCount, distanceFunction, topN, hierarchical vectors, include comments, index
    if (args != null && args.length > i) {
      return args[i];
    } else {
      String defaultString;
      if (i == 0) {
        defaultString = "src/test/resources/opennlp-issues.xml";
      } else if (i == 1) {
        defaultString = "5";
      } else if (i == 2) {
        defaultString = "200";
      } else if (i == 3) {
        defaultString = "5";
      } else if (i == 4) {
        defaultString = "false";
      } else if (i == 5) {
        defaultString = "true";
      } else if (i == 6) {
        defaultString = "true";
      } else {
        throw new RuntimeException("unexpected index " + i + "with args " + Arrays.toString(args));
      }
      return defaultString;
    }

  }

}
