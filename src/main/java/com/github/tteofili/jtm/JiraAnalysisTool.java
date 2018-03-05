package com.github.tteofili.jtm;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 *
 */
public class JiraAnalysisTool {

  public static void main(String[] args) throws IOException, XMLStreamException {

    // defaults
    int epochs = 5;
    int layerSize = 200;
    int clusterCount = 30;
    int maxIterationCount = 10;
    String distanceFunction = "cosinesimilarity";
    int topN = 5;

    String pathToJiraExport = args[0];
    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser(pathToJiraExport);
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();
    JiraIterator iterator = new JiraIterator(issues);

    Analyzer analyzer = CustomAnalyzer.builder()
        .withTokenizer(StandardTokenizerFactory.class)
        .addTokenFilter(WordDelimiterGraphFilterFactory.class)
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .build();
    TokenizerFactory tf = new LuceneTokenizerFactory(analyzer);

    Word2Vec word2Vec = new Word2Vec.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .build();
    word2Vec.fit();

    iterator.reset();

    ParagraphVectors paragraphVectors = new ParagraphVectors.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .useExistingWordVectors(word2Vec)
        .build();
    paragraphVectors.fit();

    iterator.reset();

    Par2Hier par2Hier = new Par2Hier(paragraphVectors, Par2HierUtils.Method.CLUSTER, 3);
    par2Hier.fit();

    KMeansClustering kMeansClustering = KMeansClustering.setup(clusterCount, maxIterationCount, distanceFunction, true);

    List<Point> points = Point.toPoints(paragraphVectors.lookupTable().getWeights());
    ClusterSet clusterSet = kMeansClustering.applyTo(points);

    for (Cluster c : clusterSet.getClusters()) {
      INDArray center = c.getCenter().getArray();
      Collection<String> strings = word2Vec.wordsNearest(center, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by words -> " + c.getLabel() + ", " + c.getPoints().size());
      System.out.println("topic by words -> " + word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings),1));

      Collection<String> labels = paragraphVectors.nearestLabels(center, topN);
      INDArray wordVectorsMean = paragraphVectors.getWordVectorsMean(labels);
      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by pv -> " + c.getLabel() + ", " + c.getPoints().size());
      System.out.println("topic by pv -> " + word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings),1));

      labels = par2Hier.nearestLabels(center, topN);
      wordVectorsMean = par2Hier.getWordVectorsMean(labels);
      strings = word2Vec.wordsNearest(wordVectorsMean, topN);
      c.setLabel(strings.toString());
      System.out.println("labels by p2h -> " + c.getLabel() + ", " + c.getPoints().size());
      System.out.println("topic by p2h -> " + word2Vec.wordsNearestSum(word2Vec.getWordVectorsMean(strings),1));
    }
  }

}
