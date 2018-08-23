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
package com.github.tteofili.jtm.pipeline.functions;

import java.io.InputStream;

import com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.CHUNK_TYPE;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.POS_FEATURE_NAME;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.TOKEN_TYPE;

/**
 *
 */
public class OpenNLPChunkFunction extends RichMapFunction<CAS, CAS> {

  private ChunkerModel chunkerModel;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    InputStream chunkerStream = StreamingIssuesTMPipeline.class.getResourceAsStream("/en-chunker.bin");
    chunkerModel = new ChunkerModel(chunkerStream);
  }

  @Override
  public CAS map(CAS cas) throws Exception {
    Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
    Type chunkType = cas.getTypeSystem().getType(CHUNK_TYPE);
    AnnotationIndex<AnnotationFS> index = cas.getAnnotationIndex(tokenType);
    FSIterator<AnnotationFS> iterator = index.iterator(false);
    String[] tokens = new String[index.size()];
    String[] tags = new String[index.size()];
    int i = 0;
    while (iterator.hasNext()) {
      AnnotationFS next = iterator.next();
      Feature pos = tokenType.getFeatureByBaseName(POS_FEATURE_NAME);
      String stringValue = next.getStringValue(pos);
      String text = next.getCoveredText();
      tokens[i] = text;
      tags[i] = stringValue;
      i++;
    }
    Chunker chunker = new ChunkerME(chunkerModel);
    Span[] chunks = chunker.chunkAsSpans(tokens, tags);
    for (Span s : chunks) {
      AnnotationFS annotation = cas.createAnnotation(chunkType, s.getStart(), s.getEnd());
      cas.addFsToIndexes(annotation);
    }

    return cas;
  }
}
