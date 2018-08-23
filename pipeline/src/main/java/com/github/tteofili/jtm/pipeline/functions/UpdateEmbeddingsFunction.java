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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Iterator;

import com.github.tteofili.jtm.pipeline.ModelDataChunk;
import org.apache.flink.streaming.api.functions.windowing.RichAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.uima.cas.CAS;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.sequencevectors.interfaces.SequenceIterator;
import org.deeplearning4j.models.sequencevectors.sequence.Sequence;
import org.deeplearning4j.models.sequencevectors.transformers.impl.SentenceTransformer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.CHUNK_TYPE;


/**
 * Embeddings update function
 */
public class UpdateEmbeddingsFunction extends RichAllWindowFunction<CAS, ModelDataChunk, GlobalWindow> {

  private final SecureRandom random = new SecureRandom();

  private static final String prefix = "/Users/teofili/Desktop/tm/";
  static final String PV_PATH = prefix + "pv";

  @Override
  public void apply(GlobalWindow window, Iterable<CAS> values, Collector<ModelDataChunk> out) {

    LabelAwareIterator iterator = new CASLabelAwareIterator(values, CHUNK_TYPE);

    int epochs = 1;
    int layerSize = 60;

    // fetch previously trained embeddings from file system
    ParagraphVectors oldPV = null;
    try {
      oldPV = WordVectorSerializer.readParagraphVectors(PV_PATH);
    } catch (IOException e) {
      // do nothing
    }

    ParagraphVectors paragraphVectors;

    if (oldPV != null) {
      oldPV.setLabelAwareIterator(iterator);
      SentenceTransformer transformer = new SentenceTransformer.Builder()
          .iterator(iterator).tokenizerFactory(new DefaultTokenizerFactory()).build();
      Iterator<Sequence<VocabWord>> seqIterator = transformer.iterator();
      SequenceIterator<VocabWord> sequenceIterator = new SequenceIterator<VocabWord>() {
        @Override
        public boolean hasMoreSequences() {
          return seqIterator.hasNext();
        }

        @Override
        public Sequence<VocabWord> nextSequence() {
          return seqIterator.next();
        }

        @Override
        public void reset() {
          transformer.iterator();
        }
      };

      oldPV.setSequenceIterator(sequenceIterator);
      oldPV.setTokenizerFactory(new DefaultTokenizerFactory());
      oldPV.fit();
      paragraphVectors = oldPV;
    } else {
      paragraphVectors = new ParagraphVectors.Builder()
          .iterate(iterator)
          .epochs(epochs)
          .layerSize(layerSize)
          .tokenizerFactory(new DefaultTokenizerFactory())
          .useUnknown(true)
          .trainWordVectors(true)
          .build();
      paragraphVectors.fit();
    }

    System.err.println(paragraphVectors.getLookupTable().getVocabCache().numWords());

    // store back updated embeddings on filesystem
    String outputFile = PV_PATH + "_" + random.nextFloat();
    WordVectorSerializer.writeParagraphVectors(paragraphVectors, outputFile);

    ModelDataChunk embeddings = new ModelDataChunk(outputFile, values);

    out.collect(embeddings);
  }

}
