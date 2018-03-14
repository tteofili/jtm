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

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.junit.Test;

/**
 * Tests for {@link LuceneTokenizerFactory}
 */
public class LuceneTokenizerFactoryTest {

  private String[] testStrings = new String[] {
      "&lt;p&gt;When working on &lt;a href=&quot;https://issues.apache.org/jira/browse/OPENNLP-1154&quot; title=&quot;change the XML format for feature generator config in NameFinder and POS Tagger&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OPENNLP-1154&quot;&gt;&lt;del&gt;OPENNLP-1154&lt;/del&gt;&lt;/a&gt;, I noticed this.",
      "&lt;p&gt;With &lt;a href=&quot;https://issues.apache.org/jira/browse/OAK-4940&quot; title=&quot;Consider collecting grand-parent changes in ChangeSet&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OAK-4940&quot;&gt;&lt;del&gt;OAK-4940&lt;/del&gt;&lt;/a&gt; the ChangeSet now contains all node types up to root that are related to a change. This fact could be used by the nodeType-aggregate-filter (&lt;a href=&quot;https://issues.apache.org/jira/browse/OAK-5021&quot; title=&quot;Improve observation of files&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OAK-5021&quot;&gt;&lt;del&gt;OAK-5021&lt;/del&gt;&lt;/a&gt;), which would likely speed up this type of filter.&lt;/p&gt;"};

//  @Test
//  public void testOpenNLPAnalyzer() throws Exception {
//    String sentenceModel = "en-sent.bin";
//    String tokenizerModel = "en-token.bin";
//    String posModel = "en-pos-maxent.bin";
//    String chunkerModel = "en-chunker.bin";
//    Analyzer openNLPAnalyzer = CustomAnalyzer.builder()
//        .addCharFilter(HTMLStripCharFilterFactory.class)
//        .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
//            sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
//        .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
//        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL,
//            chunkerModel)
//        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
//        .build();
//
//    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(openNLPAnalyzer);
//    for (String testString : testStrings) {
//      List<String> tokens = tokenizerFactory.create(testString).getTokens();
//      System.out.println(tokens);
//    }
//  }

  @Test
  public void testSimpleAnalyzer() throws Exception {
    Analyzer simpleAnalyzer = CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(StandardTokenizerFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
//        .addTokenFilter(WordDelimiterGraphFilterFactory.class)
        .build();
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(simpleAnalyzer);
    for (String testString : testStrings) {
      List<String> tokens = tokenizerFactory.create(testString).getTokens();
      System.out.println(tokens);
    }
  }

}