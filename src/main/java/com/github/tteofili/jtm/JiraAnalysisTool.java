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
import org.deeplearning4j.models.embeddings.learning.impl.sequence.DM;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
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
    int clusterCount = Integer.parseInt(readFrom(args, 3));
    int maxIterationCount = Integer.parseInt(readFrom(args, 4));
    String distanceFunction = readFrom(args, 5);
    int topN = Integer.parseInt(readFrom(args, 6));

    Arrays.sort(stopTags);
    InputStream posStream = JiraAnalysisTool.class.getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger = new POSTaggerME(posModel);

    log.info("Command line arguments {}", Arrays.toString(args));

    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser(pathToJiraExport);
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();

    log.info("{} issues parsed", issues.size());

    JiraIterator iterator = new JiraIterator(issues, true);

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
        .sequenceLearningAlgorithm(new DM<>())
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .useExistingWordVectors(issuesWord2vec)
        .build();
    issuesParagraphVectors.fit();

    iterator.reset();

//    Word2Vec commentsWord2vec = new Word2Vec.Builder()
//        .iterate(iterator.commentsIterator())
//        .epochs(epochs)
//        .elementsLearningAlgorithm(new SkipGram<>())
//        .layerSize(layerSize)
//        .tokenizerFactory(tf)
//        .build();
//    issuesWord2vec.fit();
//
//    ParagraphVectors commentsParagraphVectors = new ParagraphVectors.Builder()
//        .iterate(iterator.commentsIterator())
//        .sequenceLearningAlgorithm(new DM<>())
//        .epochs(epochs)
//        .layerSize(layerSize)
//        .tokenizerFactory(tf)
//        .useExistingWordVectors(commentsWord2vec)
//        .build();
//    issuesParagraphVectors.fit();


//    Par2Hier par2Hier = new Par2Hier(issuesParagraphVectors, Par2HierUtils.Method.CLUSTER, 4);
//    par2Hier.fit();

    for (Map.Entry<String, JiraIssue> entry : issues.entrySet()) {
      JiraIssue issue = entry.getValue();
      Collection<String> topics = getTopics(topN, issues, issuesWord2vec, issuesParagraphVectors, issue, tagger);
      issue.getLabels().addAll(topics);
      log.info("{} : {}", issue.getTitle(), issue.getLabels());
    }

//    KMeansClustering kMeansClustering = KMeansClustering.setup(clusterCount, maxIterationCount, distanceFunction);
//
//    List<Point> points = Point.toPoints(paragraphVectors.lookupTable().getWeights());
//    ClusterSet clusterSet = kMeansClustering.applyTo(points);
//
//    for (Cluster c : clusterSet.getClusters()) {
//      INDArray center = c.getCenter().getArray();
//      Collection<String> strings = word2Vec.wordsNearest(center, topN);
//      c.setLabel(strings.toString());
//      System.out.println("labels by words -> " + c.getLabel() + ", " + c.getPoints().size());
//      Collection<String> topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
//      System.out.println("topic by words -> " + topics);
//
//      Collection<String> labels = paragraphVectors.nearestLabels(center, topN);
//      INDArray wordVectorsMean = paragraphVectors.getWordVectorsMean(labels);
//      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
//      c.setLabel(strings.toString());
//      System.out.println("labels by pv -> " + c.getLabel() + ", " + c.getPoints().size());
//      topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
//      System.out.println("topic by pv -> " + topics);
//
//      labels = par2Hier.nearestLabels(center, topN);
//      wordVectorsMean = par2Hier.getWordVectorsMean(labels);
//      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
//      c.setLabel(strings.toString());
//      System.out.println("labels by p2h -> " + c.getLabel() + ", " + c.getPoints().size());
//      topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
//      System.out.println("topic by p2h -> " + topics);
//    }
//
//    for (Cluster c : clusterSet.getClusters()) {
//      StringBuilder builder = new StringBuilder();
//
//      INDArray center = c.getCenter().getArray();
//      Collection<String> topicBySingleWord = word2Vec.wordsNearest(center, 1);
//      double distance1 = c.getDistanceToCenter(Point.toPoints(word2Vec.getWordVectors(topicBySingleWord)).get(0));
//      builder.append(topicBySingleWord).append('(').append(distance1).append(')').append(',');
//
//      Collection<String> nearestWords = word2Vec.wordsNearest(center, topN);
//      INDArray wordVectorsMean = word2Vec.getWordVectorsMean(nearestWords);
//      Collection<String> topicByAverageWords = word2Vec.wordsNearest(wordVectorsMean, 1);
//      double distance2 = c.getDistanceToCenter(Point.toPoints(wordVectorsMean).get(0));
//      builder.append(topicByAverageWords).append('(').append(distance2).append(')').append(',');
//
//      Collection<String> nearestLabels = paragraphVectors.nearestLabels(center, topN);
//      INDArray docVectorsMean = paragraphVectors.getWordVectorsMean(nearestLabels);
//      Collection<String> nearestWordsForPV = paragraphVectors.wordsNearest(docVectorsMean, 1);
//      double distance3 = c.getDistanceToCenter(Point.toPoints(docVectorsMean).get(0));
//      builder.append(nearestWordsForPV).append('(').append(distance3).append(')').append(',');
//
//      Collection<String> nearestLabelsP2H = par2Hier.nearestLabels(center, topN);
//      INDArray docVectorsMeanP2H = par2Hier.getWordVectorsMean(nearestLabelsP2H);
//      Collection<String> nearestWordsForP2H = par2Hier.wordsNearest(wordVectorsMean, 1);
//      double distance4 = c.getDistanceToCenter(Point.toPoints(docVectorsMeanP2H).get(0));
//      builder.append(nearestWordsForP2H).append('(').append(distance4).append(')');
//
//      c.setLabel(builder.toString());
//      System.out.println(c.getLabel() + " - " + c.getPoints().size());
//
//    }

  }

  private static Collection<String> getTopics(int topN, Map<String, JiraIssue> issues, Word2Vec issuesWord2vec, ParagraphVectors issuesParagraphVectors, JiraIssue issue, POSTaggerME tagger) {
    INDArray issueVector = issuesParagraphVectors.getLookupTable().vector(issue.getId());
    Collection<String> nearestWords = issuesWord2vec.wordsNearest(issueVector, topN);

    log.debug("issue {} : {}", issue.getTitle(), nearestWords);

    Collection<String> nearestLabelsWords = new LinkedList<>();
    for (String label : issuesParagraphVectors.nearestLabels(issueVector, topN)) {
      INDArray nearestIssueVector = issuesParagraphVectors.getLookupTable().vector(label);
      nearestLabelsWords.addAll(issuesWord2vec.wordsNearest(nearestIssueVector, topN));
    }

    log.debug("nearest issues : {}", nearestLabelsWords);

    nearestWords.addAll(nearestLabelsWords);

    INDArray wordVectorsMean = issuesWord2vec.getWordVectorsMean(nearestWords);
    Collection<String> topics = issuesWord2vec.wordsNearest(wordVectorsMean, topN);

    Collection<String> toRemove = new LinkedList<>();
    for (String t : topics) {
      String key = t.toUpperCase();
      String[] tags = tagger.tag(new String[] {t});
      String tag = tags[0];
      boolean stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
      if (stopTag || issues.containsKey(key) || StringUtils.isNumeric(t)) {
        toRemove.add(t);
      }
    }
    topics.removeAll(toRemove);

    log.debug("topics : {}", topics);

    return topics;
  }

  private static String readFrom(String[] args, int i) {
    // resource, epochs, layerSize, clusterCount, maxIterationCount, distanceFunction, topN
    if (args != null && args.length > i) {
      return args[i];
    } else {
      String defaultString;
      if (i == 0) {
        defaultString = "src/test/resources/opennlp-issues.xml";
      } else if (i == 1) {
        defaultString = "3";
      } else if (i == 2) {
        defaultString = "100";
      } else if (i == 3) {
        defaultString = "30";
      } else if (i == 4) {
        defaultString = "15";
      } else if (i == 5) {
        defaultString = "cosinesimilarity";
      } else if (i == 6) {
        defaultString = "7";
      } else {
        throw new RuntimeException("unexpected index " + i + "with args " + Arrays.toString(args));
      }
      return defaultString;
    }

  }

}
