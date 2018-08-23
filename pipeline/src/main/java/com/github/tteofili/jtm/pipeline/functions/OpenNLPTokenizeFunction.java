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

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.TOKEN_TYPE;


/**
 * Tokenize function
 */
public class OpenNLPTokenizeFunction extends RichMapFunction<CAS, CAS> {

  private TokenizerModel tokenizerModel;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    InputStream tokenizerModelStream = OpenNLPTokenizeFunction.class.getResourceAsStream("/en-token.bin");
    tokenizerModel = new TokenizerModel(tokenizerModelStream);
  }

  @Override
  public CAS map(CAS cas) throws Exception {
    Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
    Tokenizer tokenizer = new TokenizerME(tokenizerModel);
    String text = cas.getDocumentText();
    Span[] spans = tokenizer.tokenizePos(text);
    for (Span s : spans) {
      AnnotationFS annotation = cas.createAnnotation(tokenType, s.getStart(), s.getEnd());
      cas.addFsToIndexes(annotation);
    }
    return cas;
  }
}
