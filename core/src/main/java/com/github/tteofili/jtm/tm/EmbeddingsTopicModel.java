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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.feed.jira.Issue;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;

/**
 * {@link TopicModel} based on document and word embeddings
 */
public class EmbeddingsTopicModel implements TopicModel {

  private static final String[] stopTags = new String[] {"CD", "VB", "RB", "JJ", "VBN", "VBG", ".", "JJS", "FW", "VBD"};

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final int epochs;
  private final int layerSize;
  private final boolean hierarchicalVectors;
  private final boolean includeComments;
  private final POSTagger tagger;

  private ParagraphVectors paragraphVectors;
  private Word2Vec word2vec;

  public EmbeddingsTopicModel(int epochs, int layerSize, boolean hierarchicalVectors, boolean includeComments) {
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    InputStream posStream = getClass().getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel = null;
    try {
      posModel = new POSModel(posStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.tagger = new POSTaggerME(posModel);
  }

  public void fit(Collection<Issue> issues) throws IOException {

    assert issues != null;

    log.info("fitting on {} issues", issues.size());

    JiraIterator iterator = new JiraIterator(issues, includeComments);

    String sentenceModel = "en-sent.bin";
    String tokenizerModel = "en-token.bin";
    String posModel = "en-pos-maxent.bin";
    String chunkerModel = "en-chunker.bin";
    Analyzer openNLPAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
            sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL, chunkerModel)
        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
        .build();

    String revisionsPattern = "r\\d+";
    try {
      CustomAnalyzer.builder()
                    .addCharFilter(HTMLStripCharFilterFactory.class)
                    .withTokenizer(ClassicTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern, "replacement", "", "replace", "all")
                    .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    TokenizerFactory tf = new LuceneTokenizerFactory(openNLPAnalyzer);

    word2vec = new Word2Vec.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .build();
    word2vec.fit();

    log.info("word2vec model fit");

    iterator.reset();

    ParagraphVectors issuesParagraphVectors = new ParagraphVectors.Builder()
        .iterate(iterator)
        .epochs(epochs)
        .layerSize(layerSize)
        .tokenizerFactory(tf)
        .useExistingWordVectors(word2vec)
        .build();
    issuesParagraphVectors.fit();

    log.info("paragraph vectors model fit");

    iterator.reset();

    if (hierarchicalVectors) {
      Par2Hier par2Hier = new Par2Hier(issuesParagraphVectors, Par2HierUtils.Method.SUM, 3);
      par2Hier.fit();

      log.info("par2hier model fit");

      paragraphVectors = par2Hier;
    } else {
      paragraphVectors = issuesParagraphVectors;
    }
  }


  @Override
  public Collection<String> extractTopics(int topN, String documentId) {
    INDArray issueVector = paragraphVectors.getLookupTable().vector(documentId);
    Collection<String> nearestWords = word2vec.wordsNearest(issueVector, topN);

    Collection<String> nearestLabelsWords = new LinkedList<>();
    for (String label : paragraphVectors.nearestLabels(issueVector, topN)) {
      INDArray nearestIssueVector = paragraphVectors.getLookupTable().vector(label);
      nearestLabelsWords.addAll(word2vec.wordsNearest(nearestIssueVector, topN));
    }

    nearestWords.addAll(nearestLabelsWords);

    INDArray wordVectorsMean = word2vec.getWordVectorsMean(nearestWords);
    return filterTopics(word2vec.wordsNearest(wordVectorsMean, topN));
  }

  private Collection<String> filterTopics(Collection<String> topics) {
    Collection<String> toRemove = new LinkedList<>();
    for (String t : topics) {
      String[] tags = tagger.tag(new String[] {t});
      String tag = tags[0];
      boolean stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
      List<String> labels = paragraphVectors.getLabelsSource().getLabels();
      if (stopTag || labels.contains(t.toUpperCase()) || labels.contains(t) || StringUtils.isNumeric(t)) {
        toRemove.add(t);
      }
    }
    topics.removeAll(toRemove);
    return topics;
  }

}
