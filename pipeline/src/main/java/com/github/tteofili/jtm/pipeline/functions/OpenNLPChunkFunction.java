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

import com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.CHUNK_FEATURE_NAME;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.CHUNK_TYPE;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.POS_FEATURE_NAME;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.TOKEN_TYPE;

/**
 * Chunk function
 */
public class OpenNLPChunkFunction extends RichMapFunction<CAS, CAS> {

  private Chunker chunker;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    InputStream chunkerStream = StreamingIssuesTMPipeline.class.getResourceAsStream("/en-chunker.bin");
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
    chunker = new ChunkerME(chunkerModel);
  }

  @Override
  public CAS map(CAS cas) throws Exception {
    Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
    Feature mPosFeature = tokenType.getFeatureByBaseName(POS_FEATURE_NAME);

    FSIndex<AnnotationFS> tokenAnnotationIndex = cas.getAnnotationIndex(tokenType);

    String[] tokens = new String[tokenAnnotationIndex.size()];
    String[] pos = new String[tokenAnnotationIndex.size()];
    AnnotationFS[] tokenAnnotations = new AnnotationFS[tokenAnnotationIndex
        .size()];

    int index = 0;

    for (AnnotationFS tokenAnnotation : tokenAnnotationIndex) {

      tokenAnnotations[index] = tokenAnnotation;

      tokens[index] = tokenAnnotation.getCoveredText();

      pos[index++] = tokenAnnotation.getFeatureValueAsString(
          mPosFeature);
    }

    String[] result = chunker.chunk(tokens, pos);

    int start = -1;
    int end = -1;
    for (int i = 0; i < result.length; i++) {

      String chunkTag = result[i];

      if (chunkTag.startsWith("B")) {
        if (start != -1) {
          addChunkAnnotation(cas, tokenAnnotations, result[i - 1].substring(2),
              start, end);
        }

        start = i;
        end = i + 1;
      } else if (chunkTag.startsWith("I")) {
        end = i + 1;
      } else if (chunkTag.startsWith("O")) {
        if (start != -1) {

          addChunkAnnotation(cas, tokenAnnotations, result[i - 1].substring(2), start, end);

          start = -1;
          end = -1;
        }
      } else {
        System.out.println("Unexpected tag: " + result[i]);
      }
    }

    if (start != -1) {
      addChunkAnnotation(cas, tokenAnnotations, result[result.length - 1].substring(2), start, end);
    }

    return cas;
  }

  private void addChunkAnnotation(CAS cas, AnnotationFS[] tokenAnnotations,
                                  String tag, int start, int end) {
    Type chunkType = cas.getTypeSystem().getType(CHUNK_TYPE);
    Feature mChunkFeature = chunkType.getFeatureByBaseName(CHUNK_FEATURE_NAME);
    AnnotationFS chunk = cas.createAnnotation(chunkType,
        tokenAnnotations[start].getBegin(), tokenAnnotations[end - 1].getEnd());

    chunk.setStringValue(mChunkFeature, tag);

    cas.getIndexRepository().addFS(chunk);
  }

}
