package com.github.tteofili.jtm.tm;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.github.tteofili.jtm.feed.Identifiable;
import com.github.tteofili.jtm.feed.Issue;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link EmbeddingsTopicModel}
 */
public class EmbeddingsTopicModelTest {

  @Test
  public void testFitWithEmptyIssues() throws Exception {
    int epochs = 1;
    int layerSize = 50;
    Analyzer analyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(ClassicTokenizerFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .build();
    String output = "target/etm-pv.zip";
    EmbeddingsTopicModel embeddingsTopicModel = new EmbeddingsTopicModel(epochs, layerSize, true, true, false, analyzer, output);

    Collection<Issue> issues = new LinkedList<>();
    Issue e = new Issue();
    issues.add(e);
    try {
      embeddingsTopicModel.fit(issues);
      fail("it should not be possible to fit over empty issues");
    } catch (Exception ex) {
      // all fine
    }

  }

  @Test
  public void testFitWithOneIssue() throws Exception {
    int epochs = 1;
    int layerSize = 50;
    Analyzer analyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(ClassicTokenizerFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .build();
    String output = "target/etm-pv.zip";
    EmbeddingsTopicModel embeddingsTopicModel = new EmbeddingsTopicModel(epochs, layerSize, true, true, false, analyzer, output);

    Collection<Issue> issues = new LinkedList<>();
    Issue e = new Issue();
    Identifiable key = new Identifiable();
    key.setId(123);
    key.setValue("JTM-123");
    e.setKey(key);
    e.setDescription("a dummy description");
    e.setLabels(Collections.singletonList("dummy"));
    e.setSummary("a dummy summary");
    e.setTitle("a dummy title");
    issues.add(e);
    embeddingsTopicModel.fit(issues);
  }

  @Ignore
  @Test
  public void testLoadFromFile() throws Exception {
    String[] testStrings = new String[] {"tommaso teofili", "tommaso", "francesco mari", "frustration", "expectations", "wdyt", "wtf", "antonio sanso", "simo tripodi", "simone tropodi",
    };
    int topN = 20;
    EmbeddingsTopicModel model = new EmbeddingsTopicModel(new File("target/issue-embeddings.zip"));
    for (String ts : testStrings) {
      Collection<String> topics = model.extractTopics(topN, ts);
      System.out.println(ts + " -> " + topics);
      assertNotNull(topics);
    }
  }

}