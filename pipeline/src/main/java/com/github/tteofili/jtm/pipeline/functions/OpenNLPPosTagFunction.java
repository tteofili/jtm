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
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.POS_FEATURE_NAME;
import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.TOKEN_TYPE;

/**
 * PoS tag function
 */
public class OpenNLPPosTagFunction extends RichMapFunction<CAS, CAS> {

  private POSModel posModel;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    InputStream posStream = OpenNLPPosTagFunction.class.getResourceAsStream("/en-pos-maxent.bin");
    posModel = new POSModel(posStream);
  }

  @Override
  public CAS map(CAS cas) throws Exception {
    Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
    POSTaggerME tagger = new POSTaggerME(posModel);
    FSIterator<AnnotationFS> iterator = cas.getAnnotationIndex(tokenType).iterator(false);
    while (iterator.hasNext()) {
      AnnotationFS next = iterator.next();
      next.setStringValue(tokenType.getFeatureByBaseName(POS_FEATURE_NAME), tagger.tag(new String[] {next.getCoveredText()})[0]);
    }
    return cas;
  }
}
