package com.github.tteofili.jtm;

import java.io.File;
import java.net.URL;

import org.junit.Test;

/**
 *
 */
public class JiraAnalysisToolTest {

  @Test
  public void testExecution() throws Exception {
    URL resource = getClass().getResource("/issues.xml");
    File f = new File(resource.toURI());
    String[] args = new String[] {f.getAbsolutePath()};
//    String[] args = new String[] {"/Users/teofili/Desktop/issue-analysis/cq.xml"};
    JiraAnalysisTool.main(args);
  }
}