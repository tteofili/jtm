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

import com.github.tteofili.jtm.feed.Issue;
import com.github.tteofili.jtm.pipeline.utils.UimaUtil;
import com.google.common.base.Joiner;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Function to create CAS
 */
public class CASCreationFunction extends RichMapFunction<Issue, CAS> {

  private TypeSystemDescription typeSystemDesc;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    typeSystemDesc = UimaUtil.createTypeSystemDescription(
        CASCreationFunction.class.getResourceAsStream("/TypeSystem.xml"));
  }

  @Override
  public CAS map(Issue issue) throws Exception {

    CAS cas = UimaUtil.createEmptyCAS(typeSystemDesc);
    UimaUtil.setIssueId(cas, issue);
    String documentText = Joiner.on(' ').join(
        issue.getTitle(),
        issue.getDescription(),
        issue.getSummary()
    );
    cas.setDocumentText(documentText);
    return cas;
  }
}
