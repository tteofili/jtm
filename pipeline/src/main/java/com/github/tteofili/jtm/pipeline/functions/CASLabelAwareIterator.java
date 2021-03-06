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

import java.util.Collections;
import java.util.Iterator;

import com.github.tteofili.jtm.pipeline.utils.UimaUtil;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;

import static com.github.tteofili.jtm.pipeline.StreamingIssuesTMPipeline.CHUNK_FEATURE_NAME;

/**
 * {@link LabelAwareIterator} for CAS
 */
public class CASLabelAwareIterator implements LabelAwareIterator {

  private final Iterable<CAS> cases;
  private Iterator<CAS> iterator;
  private final String type;
  private final LabelsSource labels = new LabelsSource();

  CASLabelAwareIterator(Iterable<CAS> cases, String type) {
    this.cases = cases;
    for (CAS cas : cases) {
      labels.storeLabel(cas.toString());
    }
    this.iterator = cases.iterator();
    this.type = type;
  }

  @Override
  public boolean hasNextDocument() {
    return iterator != null && iterator.hasNext();
  }

  @Override
  public LabelledDocument nextDocument() {
    CAS cas = iterator.next();
    LabelledDocument document = new LabelledDocument();
    document.setLabels(Collections.singletonList(UimaUtil.getIssueId(cas)));
    Type annotationType = cas.getTypeSystem().getType(type);
    Feature mChunkFeature = annotationType.getFeatureByBaseName(CHUNK_FEATURE_NAME);
    AnnotationIndex<AnnotationFS> annotationIndex = cas.getAnnotationIndex(annotationType);
    StringBuilder text = new StringBuilder();
    for (AnnotationFS annotationFS : annotationIndex) {
      String chunkType = annotationFS.getFeatureValueAsString(mChunkFeature);
      if ("NP".equals(chunkType)) {
        if (text.length() > 0) {
          text.append(' ');
        }
        text.append(annotationFS.getCoveredText());
      }
    }
    document.setContent(text.toString());

    return document;
  }

  @Override
  public void reset() {
    iterator = cases.iterator();
  }

  @Override
  public LabelsSource getLabelsSource() {
    return labels;
  }

  @Override
  public void shutdown() {
    iterator = null;
  }

  @Override
  public boolean hasNext() {
    return hasNextDocument();
  }

  @Override
  public LabelledDocument next() {
    return nextDocument();
  }
}
