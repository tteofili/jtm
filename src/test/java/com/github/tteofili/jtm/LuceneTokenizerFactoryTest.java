package com.github.tteofili.jtm;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.TypeTokenFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.junit.Test;

/**
 *
 */
public class LuceneTokenizerFactoryTest {

  private String testString = "&lt;p&gt;When working on &lt;a href=&quot;https://issues.apache.org/jira/browse/OPENNLP-1154&quot; title=&quot;change the XML format for feature generator config in NameFinder and POS Tagger&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OPENNLP-1154&quot;&gt;&lt;del&gt;OPENNLP-1154&lt;/del&gt;&lt;/a&gt;, I noticed this.";

  @Test
  public void testOpenNLPAnalyzer() throws Exception {
    String sentenceModel = "en-sent.bin";
    String tokenizerModel = "en-token.bin";
    String posModel = "en-pos-maxent.bin";
    String chunkerModel = "en-chunker.bin";
    Analyzer openNLPAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
            sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
        .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL,
            chunkerModel)
        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
        .build();

    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(openNLPAnalyzer);
    List<String> tokens = tokenizerFactory.create(testString).getTokens();
    System.out.println(tokens);
  }

  @Test
  public void testSimpleAnalyzer() throws Exception {
    Analyzer simpleAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(StandardTokenizerFactory.class)
        .addTokenFilter(WordDelimiterGraphFilterFactory.class)
        .build();
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(simpleAnalyzer);
    List<String> tokens = tokenizerFactory.create(testString).getTokens();
    System.out.println(tokens);
  }

}