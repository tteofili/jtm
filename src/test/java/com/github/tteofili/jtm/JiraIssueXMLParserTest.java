package com.github.tteofili.jtm;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JiraIssueXMLParserTest {

  @Test
  public void testParse() throws Exception {
    JiraIssueXMLParser parser = new JiraIssueXMLParser("src/test/resources/issues.xml");
    final Map<String, JiraIssue> issues = parser.parse();
    assertEquals(1000, issues.size());
  }
}