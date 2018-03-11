package com.github.tteofili.jtm.pipeline;

import java.io.InputStream;
import java.util.Map;

import com.github.tteofili.jtm.JiraAnalysisTool;
import com.github.tteofili.jtm.JiraIssue;
import com.github.tteofili.jtm.JiraIssueXMLParser;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class UimaTestPipeline {

  private static final String TOKEN_TYPE = "opennlp.uima.Token";
  private static final String CHUNK_TYPE = "opennlp.uima.Chunk";

  public static void main(String[] args) throws Exception {

    // opennlp models

    InputStream tokenizerModelStream = JiraAnalysisTool.class.getResourceAsStream("/en-token.bin");
    TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelStream);

    InputStream posStream = JiraAnalysisTool.class.getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel = new POSModel(posStream);

    InputStream chunkerStream = JiraAnalysisTool.class.getResourceAsStream("/en-chunker.bin");
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);

    final StreamExecutionEnvironment env =
        StreamExecutionEnvironment.getExecutionEnvironment();

    env.getConfig().registerTypeWithKryoSerializer(CASImpl.class, KryoCasSerializer.class);

    JiraIssueXMLParser jiraIssueXMLParser = new JiraIssueXMLParser("src/test/resources/opennlp-issues.xml");
    Map<String, JiraIssue> issues = jiraIssueXMLParser.parse();

    DataStreamSource<JiraIssue> jiraIssueDataStreamSource = env.fromCollection(issues.values());

    TypeSystemDescription typeSystemDesc = UimaUtil.createTypeSystemDescription(
        UimaTestPipeline.class.getResourceAsStream("/TypeSystem.xml"));

    // create stream on CAS
    DataStream<CAS> casStream = jiraIssueDataStreamSource.map((MapFunction<JiraIssue, CAS>) issue -> {
      CAS cas = UimaUtil.createEmptyCAS(typeSystemDesc);
      cas.setDocumentText(issue.asString());
      return cas;
    });

    casStream
        .map((MapFunction<CAS, CAS>) cas -> { // tokenize
      Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
      Tokenizer tokenizer = new TokenizerME(tokenizerModel);
      String text = cas.getDocumentText();
      Span[] spans = tokenizer.tokenizePos(text);
      for (Span s : spans) {
        AnnotationFS annotation = cas.createAnnotation(tokenType, s.getStart(), s.getEnd());
        cas.addFsToIndexes(annotation);
      }
      return cas;
    })
        .map((MapFunction<CAS, CAS>) cas -> { // pos tag
      Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
      POSTaggerME tagger = new POSTaggerME(posModel);
      int i = 0;
      FSIterator<AnnotationFS> iterator = cas.getAnnotationIndex(tokenType).iterator(false);
      while (iterator.hasNext()) {
        AnnotationFS next = iterator.next();
        next.setStringValue(tokenType.getFeatureByBaseName("pos"), tagger.tag(new String[] {next.getCoveredText()})[0]);
      }
      return cas;
    })
        .map((MapFunction<CAS, CAS>) cas -> { // chunk
      Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
      Type chunkType = cas.getTypeSystem().getType(CHUNK_TYPE);
      AnnotationIndex<AnnotationFS> index = cas.getAnnotationIndex(tokenType);
      FSIterator<AnnotationFS> iterator = index.iterator(false);
      String[] tokens = new String[index.size()];
      String[] tags = new String[index.size()];
      int i = 0;
      while (iterator.hasNext()) {
        AnnotationFS next = iterator.next();
        Feature pos = tokenType.getFeatureByBaseName("pos");
        String stringValue = next.getStringValue(pos);
        String text = next.getCoveredText();
        tokens[i] = text;
        tags[i] = stringValue;
        i++;
      }
      Chunker chunker = new ChunkerME(chunkerModel);
      Span[] chunks = chunker.chunkAsSpans(tokens, tags);
      for (Span s : chunks) {
        AnnotationFS annotation = cas.createAnnotation(chunkType, s.getStart(), s.getEnd());
        cas.addFsToIndexes(annotation);
      }

      return cas;
    })
        .map((MapFunction<CAS, Integer>) cas -> { // print ann no.
      Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
      return cas.getAnnotationIndex(tokenType).size();
    }).print();

    env.execute();
  }
}
