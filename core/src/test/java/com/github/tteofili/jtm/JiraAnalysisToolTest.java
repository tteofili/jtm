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
package com.github.tteofili.jtm;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.jira.JiraFeedReader;

/**
 * Tests for {@link JiraAnalysisTool}
 */
@RunWith(Parameterized.class)
public class JiraAnalysisToolTest {

  private final String resource;
  private final int epochs;
  private final int layerSize;
  private final int topN;
  private final String analyzerType;

  public JiraAnalysisToolTest(String resource, int epochs, int layerSize, int topN, String analyzerType) {
    this.resource = resource;
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.analyzerType = analyzerType;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    // resource, epochs, layerSize, topN, analyzerType
    return Arrays.asList(new Object[][] {
        {"/opennlp-issues.xml", 2, 60, 3, "simple"},
//        {"/lucene-issues.xml", 2, 60, 3, "simple"},
//        {"/oak-issues.xml", 2, 60, 3, "simple"},
    });
  }

  @Test
  public void testExecution() throws Exception {
    JiraAnalysisTool jiraAnalysisTool = new JiraAnalysisTool(epochs, layerSize, topN, false, true, false, analyzerType);
    InputStream inputStream = getClass().getResourceAsStream(resource);
    Feed feed = new JiraFeedReader().read(inputStream);
    jiraAnalysisTool.analyze(feed);
    inputStream.close();
  }
}