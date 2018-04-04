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
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link JiraIssueXMLParser}
 */
@RunWith(Parameterized.class)
public class JiraIssueXMLParserTest {

  private final String resource;

  public JiraIssueXMLParserTest(String resource) {
    this.resource = resource;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"/opennlp-issues.xml"},
        {"/lucene-issues.xml"},
        {"/oak-issues.xml"},
    });
  }

  @Test
  public void testParse() throws Exception {
    URL resource = getClass().getResource(this.resource);
    File f = new File(resource.toURI());
    JiraIssueXMLParser parser = new JiraIssueXMLParser(f);
    final Map<String, JiraIssue> issues = parser.parse();
    assertEquals(1000, issues.size());
  }
}