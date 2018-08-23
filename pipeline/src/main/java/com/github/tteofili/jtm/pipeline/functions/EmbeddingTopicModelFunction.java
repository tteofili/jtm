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
package com.github.tteofili.jtm.pipeline.functions;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.pipeline.ModelDataChunk;
import com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline;
import com.github.tteofili.jtm.pipeline.utils.UimaUtil;
import com.github.tteofili.jtm.tm.LuceneTokenizerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.uima.cas.CAS;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.nd4j.linalg.api.ndarray.INDArray;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.stopTags;


/**
 * Function for topic modelling algorithm based on embeddings
 */
public class EmbeddingTopicModelFunction extends RichMapFunction<ModelDataChunk, Topics> {

  private POSModel posModel;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    InputStream posStream = StreamingIssuesTMPipeline.class.getResourceAsStream("/en-pos-maxent.bin");
    posModel = new POSModel(posStream);
  }

  @Override
  public Topics map(ModelDataChunk modelDataChunk) throws Exception {
    POSTaggerME tagger = new POSTaggerME(posModel);
    int topN = 3;
    ParagraphVectors paragraphVectors = WordVectorSerializer.readParagraphVectors(modelDataChunk.getPathToParagraphVectors());
    paragraphVectors.setTokenizerFactory(new LuceneTokenizerFactory());

    Topics topics = new Topics();
    for (CAS cas : modelDataChunk.getValues()) {
      String key = UimaUtil.getIssueId(cas);

      INDArray issueVector;
      try {
        issueVector = paragraphVectors.getLookupTable().vector(key);
      } catch (Exception e) {
        issueVector = paragraphVectors.inferVector(cas.getDocumentText());
      }

      Collection<String> nearestWords = paragraphVectors.wordsNearest(issueVector, topN);

      Collection<String> nearestLabelsWords = new LinkedList<>();
      for (String label : paragraphVectors.nearestLabels(issueVector, topN)) {
        INDArray nearestIssueVector = paragraphVectors.getLookupTable().vector(label);
        nearestLabelsWords.addAll(paragraphVectors.wordsNearest(nearestIssueVector, topN));
      }

      nearestWords.addAll(nearestLabelsWords);

      INDArray wordVectorsMean = paragraphVectors.getWordVectorsMean(nearestWords);
      Collection<String> extractedTopics = paragraphVectors.wordsNearest(wordVectorsMean, topN);

      Collection<String> toRemove = new LinkedList<>();
      for (String t : extractedTopics) {
        String[] tags = tagger.tag(new String[] {t});
        String tag = tags[0];
        boolean stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
        if (stopTag || StringUtils.isNumeric(t)) {
          toRemove.add(t);
        }
      }
      extractedTopics.removeAll(toRemove);

      for (String topic : extractedTopics) {
        topics.add(topic, key);
      }
    }


    return topics;
  }
}
