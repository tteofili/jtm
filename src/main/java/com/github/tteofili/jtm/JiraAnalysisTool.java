package com.github.tteofili.jtm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.TypeTokenFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.learning.impl.sequence.DM;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class JiraAnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(JiraAnalysisTool.class);

  public static void main(String[] args) throws IOException, XMLStreamException {

    String pathToJiraExport = readFrom(args, 0);
    int epochs = Integer.parseInt(readFrom(args, 1));
    int layerSize = Integer.parseInt(readFrom(args, 2));
    int clusterCount = Integer.parseInt(readFrom(args, 3));
    int maxIterationCount = Integer.parseInt(readFrom(args, 4));
    String distanceFunction = readFrom(args, 5);
    int topN = Integer.parseInt(readFrom(args, 6));

    log.info("Command line arguments {}", Arrays.toString(args));

    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser(pathToJiraExport);
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();

    log.info("Issues parsed");

    JiraIterator iterator = new JiraIterator(issues);

    String sentenceModel = "en-sent.bin";
    String tokenizerModel = "en-token.bin";
    String posModel = "en-pos-maxent.bin";
    String chunkerModel = "en-chunker.bin";
    Analyzer openNLPAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(OpenNLPTokenizerFactory.class,OpenNLPTokenizerFactory.SENTENCE_MODEL,
            sentenceModel,OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
        .addTokenFilter(OpenNLPPOSFilterFactory.class,OpenNLPPOSFilterFactory.POS_TAGGER_MODEL,posModel)
        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL,
            chunkerModel)
        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
        .build();

    Analyzer simpleAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(StandardTokenizerFactory.class)
        .addTokenFilter(ShingleFilterFactory.class)
        .addTokenFilter(WordDelimiterGraphFilterFactory.class)
        .build();

    TokenizerFactory tf = new LuceneTokenizerFactory(openNLPAnalyzer);

    Word2Vec word2Vec = new Word2Vec.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .elementsLearningAlgorithm(new CBOW<>())
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .build();
    word2Vec.fit();

    iterator.reset();

    ParagraphVectors paragraphVectors = new ParagraphVectors.Builder()
        .iterate(iterator)
        .sequenceLearningAlgorithm(new DM<>())
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .useExistingWordVectors(word2Vec)
        .build();
    paragraphVectors.fit();

    iterator.reset();

    Par2Hier par2Hier = new Par2Hier(paragraphVectors, Par2HierUtils.Method.CLUSTER, 3);
    par2Hier.fit();

    KMeansClustering kMeansClustering = KMeansClustering.setup(clusterCount, maxIterationCount, distanceFunction);

    List<Point> points = Point.toPoints(paragraphVectors.lookupTable().getWeights());
    ClusterSet clusterSet = kMeansClustering.applyTo(points);

    for (Cluster c : clusterSet.getClusters()) {
      INDArray center = c.getCenter().getArray();
      Collection<String> strings = word2Vec.wordsNearest(center, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by words -> " + c.getLabel() + ", " + c.getPoints().size());
      Collection<String> topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
      System.out.println("topic by words -> " + topics);

      Collection<String> labels = paragraphVectors.nearestLabels(center, topN);
      INDArray wordVectorsMean = paragraphVectors.getWordVectorsMean(labels);
      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by pv -> " + c.getLabel() + ", " + c.getPoints().size());
      topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
      System.out.println("topic by pv -> " + topics);

      labels = par2Hier.nearestLabels(center, topN);
      wordVectorsMean = par2Hier.getWordVectorsMean(labels);
      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by p2h -> " + c.getLabel() + ", " + c.getPoints().size());
      topics = word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings), 1);
      System.out.println("topic by p2h -> " + topics);
    }

    for (Cluster c : clusterSet.getClusters()) {
      StringBuilder builder = new StringBuilder();

      INDArray center = c.getCenter().getArray();
      Collection<String> topicBySingleWord = word2Vec.wordsNearest(center, 1);
      double distance1 = c.getDistanceToCenter(Point.toPoints(word2Vec.getWordVectors(topicBySingleWord)).get(0));
      builder.append(topicBySingleWord).append('(').append(distance1).append(')').append(',');

      Collection<String> nearestWords = word2Vec.wordsNearest(center, topN);
      INDArray wordVectorsMean = word2Vec.getWordVectorsMean(nearestWords);
      Collection<String> topicByAverageWords = word2Vec.wordsNearest(wordVectorsMean, 1);
      double distance2 = c.getDistanceToCenter(Point.toPoints(wordVectorsMean).get(0));
      builder.append(topicByAverageWords).append('(').append(distance2).append(')').append(',');

      Collection<String> nearestLabels = paragraphVectors.nearestLabels(center, topN);
      INDArray docVectorsMean = paragraphVectors.getWordVectorsMean(nearestLabels);
      Collection<String> nearestWordsForPV = paragraphVectors.wordsNearest(docVectorsMean, 1);
      double distance3 = c.getDistanceToCenter(Point.toPoints(docVectorsMean).get(0));
      builder.append(nearestWordsForPV).append('(').append(distance3).append(')').append(',');

      Collection<String> nearestLabelsP2H = par2Hier.nearestLabels(center, topN);
      INDArray docVectorsMeanP2H = par2Hier.getWordVectorsMean(nearestLabelsP2H);
      Collection<String> nearestWordsForP2H = par2Hier.wordsNearest(wordVectorsMean, 1);
      double distance4 = c.getDistanceToCenter(Point.toPoints(docVectorsMeanP2H).get(0));
      builder.append(nearestWordsForP2H).append('(').append(distance4).append(')');

      c.setLabel(builder.toString());
      System.out.println(c.getLabel() + " - " + c.getPoints().size());

//      Collection<String> clusterPoints = paragraphVectors.nearestLabels(c.getCenter().getArray(), c.getPoints().size());
//      for (String cp : clusterPoints) {
//        if (issues.containsKey(cp)) {
//          issues.get(cp).asString()
//        }
//      }
    }

//    for (String key : issues.keySet()) {
//
//    }


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
        defaultString = "5";
      } else {
        throw new RuntimeException("unexpected index " + i + "with args " + Arrays.toString(args));
      }
      return defaultString;
    }

  }

}
