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
package com.github.tteofili.jtm;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for {@link JiraAnalysisTool}
 */
@RunWith(Parameterized.class)
public class JiraAnalysisToolTest {

  private final String resource;
  private final int epochs;
  private final int layerSize;
  private final int topN;

  public JiraAnalysisToolTest(String resource, int epochs, int layerSize, int topN) {
    this.resource = resource;
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    // resource, epochs, layerSize, topN
    return Arrays.asList(new Object[][] {
        {"/opennlp-issues.xml", 2, 60, 3},
        {"/lucene-issues.xml", 2, 60, 3},
        {"/oak-issues.xml", 2, 60, 3},
    });
  }

  @Test
  public void testExecution() throws Exception {
    URL resource = getClass().getResource(this.resource);
    File f = new File(resource.toURI());
    JiraAnalysisTool jiraAnalysisTool = new JiraAnalysisTool(f, epochs, layerSize, topN, false, true, false);
    jiraAnalysisTool.execute();
  }
}