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
package com.github.tteofili.jtm.tm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.github.tteofili.jtm.AnalysisUtils;
import com.github.tteofili.jtm.feed.Identifiable;
import com.github.tteofili.jtm.feed.Issue;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;

/**
 * {@link TopicModel} based on document and word embeddings
 */
public class EmbeddingsTopicModel implements TopicModel {

  private static final String[] stopTags = new String[] {"CD", "VB", "RB", "JJ", "VBN", "VBG", ".", "JJS", "FW", "VBD", "RB", "VBG"};

  private static final Pattern pattern = Pattern.compile("\\w+-\\d+");

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final int epochs;
  private final int layerSize;
  private final boolean hierarchicalVectors;
  private final boolean includeComments;
  private final Analyzer analyzer;
  private final String vectorsOutputFile;

  private final POSTagger tagger;

  private final boolean generateClusters;

  private ParagraphVectors paragraphVectors;

  public EmbeddingsTopicModel(int epochs, int layerSize, boolean hierarchicalVectors,
                              boolean includeComments, boolean generateClusters, Analyzer analyzer,
                              String vectorsOutputFile) {
    // TODO : move this into some configuration / builder object
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    this.generateClusters = generateClusters;
    this.analyzer = analyzer;
    this.vectorsOutputFile = vectorsOutputFile;
    InputStream posStream = getClass().getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel;
    try {
      posModel = new POSModel(posStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.tagger = new POSTaggerME(posModel);
    Arrays.sort(stopTags);
  }

  public EmbeddingsTopicModel(File file) throws IOException {
    InputStream posStream = getClass().getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel;
    try {
      posModel = new POSModel(posStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.tagger = new POSTaggerME(posModel);

    this.paragraphVectors = WordVectorSerializer.readParagraphVectors(file);
    this.paragraphVectors.setTokenizerFactory(new LuceneTokenizerFactory());

    // TODO : move this into some configuration / builder object
    this.layerSize = this.paragraphVectors.getLayerSize();
    this.hierarchicalVectors = false;
    this.includeComments = true;
    this.generateClusters = false;
    try {
      this.analyzer = AnalysisUtils.simpleAnalyzer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.vectorsOutputFile = file.getAbsolutePath();
    this.epochs = 5;
    Arrays.sort(stopTags);
  }

  public void fit(Collection<Issue> issues) throws IOException {

    assert issues != null;

    log.info("fitting on {} issues", issues.size());

    IssueIterator iterator = new IssueIterator(issues, includeComments);

    TokenizerFactory tf = new LuceneTokenizerFactory(analyzer);

    ParagraphVectors issuesParagraphVectors = new ParagraphVectors.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .trainWordVectors(true)
        .useUnknown(true)
        .minWordFrequency(5)
        .build();
    issuesParagraphVectors.fit();

    log.info("paragraph vectors model fit");

    iterator.reset();

    if (hierarchicalVectors) {
      Par2Hier par2Hier = new Par2Hier(issuesParagraphVectors, Par2Hier.Method.CLUSTER, 3);
      par2Hier.fit();

      log.info("par2hier model fit");

      paragraphVectors = par2Hier;
    } else {
      paragraphVectors = issuesParagraphVectors;
    }

    if (generateClusters) {
      String distanceFunction = "cosinesimilarity";
      int topN = 10;
      KMeansClustering kMeansClustering = KMeansClustering.setup(50, 100, distanceFunction, true);

      List<Point> points = Point.toPoints(paragraphVectors.getLookupTable().getWeights());
      ClusterSet clusterSet = kMeansClustering.applyTo(points);

      for (Cluster c : clusterSet.getClusters()) {
        final INDArray center = c.getCenter().getArray();
        Collection<String> strings = getTopicsForVector(topN, center);
        log.info("cluster {} initial topics: {}", c.getId(), strings);

        Comparator<? super String> comparator = (Comparator<String>) (o1, o2) -> {
          INDArray vector1 = paragraphVectors.getLookupTable().vector(o1);
          INDArray vector2 = paragraphVectors.getLookupTable().vector(o2);
          return (int) (Transforms.cosineSim(center, vector1) - Transforms.cosineSim(center, vector2));
        };
        Map<String, Set<String>> topicCandidates = new HashMap<>();
        for (String t : strings) {
          topicCandidates.put(t, Sets.newHashSet(extractTopics(topN,t)));
        }
        Set<String> finalTopic = new TreeSet<>(comparator);
        for (Set<String> s : topicCandidates.values()) {
          if (finalTopic.isEmpty()) {
            finalTopic = s;
          } else {
            finalTopic = Sets.intersection(finalTopic, s);
          }
        }

        if (finalTopic.isEmpty()) {
          c.setLabel(strings.toString());
        } else {
          c.setLabel(finalTopic.toString());
        }

        log.info("cluster {}: {} ({})", c.getId(), c.getLabel(), + c.getPoints().size());

      }
    }

    if (vectorsOutputFile != null && vectorsOutputFile.trim().length() > 0) {
      WordVectorSerializer.writeParagraphVectors(paragraphVectors, new File(vectorsOutputFile));
    }

  }

  @Override
  public Collection<String> extractTopics(int topN, Identifiable documentId) {
    INDArray issueVector = paragraphVectors.getLookupTable().vector(documentId.getValue());
    return getTopicsForVector(topN, issueVector);
  }

  private Collection<String> getTopicsForVector(int topN, INDArray vector) {
    Collection<String> result = Collections.emptyList();
    if (vector != null) {
      try {
        // nearest words
        Collection<String> nearestWords = paragraphVectors.wordsNearestSum(vector, topN);

        Collection<String> nearestLabelsWords = new LinkedList<>();
        for (String label : paragraphVectors.nearestLabels(vector, topN)) {
          INDArray nearestIssueVector = paragraphVectors.getLookupTable().vector(label);
          nearestLabelsWords.addAll(paragraphVectors.wordsNearestSum(nearestIssueVector, topN));
        }

        // nearest words of nearest neighbours
        nearestWords.addAll(nearestLabelsWords);

        // calculate average vector of nearest words
        INDArray wordVectorsMean = paragraphVectors.getWordVectorsMean(nearestWords);

        // nearest words of average vector
        Collection<String> topics = paragraphVectors.wordsNearestSum(wordVectorsMean, topN);
        for (String w : nearestWords) {
          if (!topics.contains(w)) {
            topics.add(w);
          }
        }
        // TODO : sort topics by similarity ?
        result = filterTopics(topics);
      } catch (Throwable e) {
        log.error("could not get topics", e);
      }
    }
    return result;
  }

  @Override
  public Collection<String> extractTopics(int topN, String text) {
    try {
      log.debug("{} nearest labels {}", text, paragraphVectors.nearestLabels(text, topN));
      List<String> tokens = paragraphVectors.getTokenizerFactory().create(text).getTokens();
      INDArray textVector = paragraphVectors.getWordVectorsMean(tokens);
      return getTopicsForVector(topN, textVector);
    } catch (Throwable t) {
      return Collections.emptySet();
    }
  }

  private Collection<String> filterTopics(Collection<String> topics) {
    Collection<String> toRemove = new LinkedList<>();
    for (String t : topics) {
      try {
        String[] tags = tagger.tag(new String[] {t});
        boolean stopTag = false;
        for (String tag : tags) {
          stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
          if (stopTag) {
            break;
          }
        }
        List<String> labels = paragraphVectors.getLabelsSource().getLabels();
        if (stopTag || labels.contains(t.toUpperCase()) || labels.contains(t) || StringUtils.isNumeric(t) ||
            pattern.matcher(t).find()) {
          toRemove.add(t);
        }
      } catch (Throwable throwable) {
        log.error("could not filter topic {}", t);
      }
    }
    topics.removeAll(toRemove);
    return topics;
  }

}
