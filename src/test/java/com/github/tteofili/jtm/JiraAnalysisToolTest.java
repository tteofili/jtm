package com.github.tteofili.jtm;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class JiraAnalysisToolTest {

  private final String resource;
  private final String epochs;
  private final String layerSize;
  private final String clusterCount;
  private final String maxIterationCount;
  private final String distanceFunction;
  private final String topN;

  public JiraAnalysisToolTest(String resource, String epochs, String layerSize, String clusterCount,
                              String maxIterationCount, String distanceFunction, String topN) {
    this.resource = resource;
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.clusterCount = clusterCount;
    this.maxIterationCount = maxIterationCount;
    this.distanceFunction = distanceFunction;
    this.topN = topN;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    // resource, epochs, layerSize, clusterCount, maxIterationCount, distanceFunction, topN
    return Arrays.asList(new Object[][] {
        {"/opennlp-issues.xml", "1", "50", "10", "5", "cosinesimilarity", "3"},
        {"/lucene-issues.xml", "1", "50", "10", "5", "cosinesimilarity", "3"},
        {"/oak-issues.xml", "1", "50", "10", "5", "cosinesimilarity", "3"},
    });
  }

  @Test
  public void testExecution() throws Exception {
    URL resource = getClass().getResource(this.resource);
    File f = new File(resource.toURI());
    String[] args = new String[] {f.getAbsolutePath(), epochs, layerSize, clusterCount,
        maxIterationCount, distanceFunction, topN};
    JiraAnalysisTool.main(args);
  }
}