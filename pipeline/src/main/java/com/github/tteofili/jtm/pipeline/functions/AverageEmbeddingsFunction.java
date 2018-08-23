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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;

import com.github.tteofili.jtm.pipeline.ModelDataChunk;
import com.github.tteofili.jtm.tm.LuceneTokenizerFactory;
import com.google.common.collect.Iterables;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.uima.cas.CAS;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tteofili.jtm.pipeline.functions.UpdateEmbeddingsFunction.PV_PATH;


/**
 * Function to average multiple embeddings models
 */
public class AverageEmbeddingsFunction implements AllWindowFunction<ModelDataChunk, ModelDataChunk, GlobalWindow> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void apply(GlobalWindow window, Iterable<ModelDataChunk> values, Collector<ModelDataChunk> out) throws Exception {
    ParagraphVectors paragraphVectors = null;
    Collection<CAS> cases = new LinkedList<>();
    int i = 0;
    for (ModelDataChunk chunk : values) {
      Iterables.addAll(cases, chunk.getValues());
      String pathToParagraphVectors = chunk.getPathToParagraphVectors();
      log.debug("averaging model at {}", pathToParagraphVectors);
      ParagraphVectors current = WordVectorSerializer.readParagraphVectors(pathToParagraphVectors);
      if (paragraphVectors == null) {
        paragraphVectors = current;
        paragraphVectors.setTokenizerFactory(new LuceneTokenizerFactory());
      } else {
        averageModels(paragraphVectors, current);
      }
      Files.delete(Paths.get(pathToParagraphVectors));
      i++;
    }
    log.debug("averaged {} models", i);
    if (paragraphVectors != null) {
      WordVectorSerializer.writeParagraphVectors(paragraphVectors, PV_PATH);
    }
    ModelDataChunk chunk = new ModelDataChunk(PV_PATH, cases);

    out.collect(chunk);
  }

  private void averageModels(ParagraphVectors master, ParagraphVectors slave) {
    log.debug("initial vocabs {},{}", master.vocab().numWords(), slave.vocab().numWords());
    VocabCache<VocabWord> v = slave.getVocab();
    for (VocabWord t : v.vocabWords()) {
      if (!master.getVocab().containsWord(t.getWord())) {
        master.getVocab().incrementTotalDocCount();
        master.getVocab().incrementWordCount(t.getWord());
        master.getVocab().addWordToIndex(master.getVocab().numWords(), t.getWord());
        master.getLookupTable().putVector(t.getWord(), slave.getLookupTable().vector(t.getWord()));
        master.vocab().saveVocab();
      } else {
        INDArray denseDocumentVector = Nd4j.zeros(master.getLookupTable().layerSize());
        denseDocumentVector.addi(master.getLookupTable().vector(t.getWord()));
        denseDocumentVector.addi(slave.getLookupTable().vector(t.getWord()));
        master.getLookupTable().putVector(t.getWord(), denseDocumentVector.divi(2d));
        master.vocab().saveVocab();
      }
    }
    log.debug("merged vocab {}", master.vocab().numWords());
  }

}
