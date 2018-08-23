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
package com.github.tteofili.jtm.pipeline;

import java.io.FileInputStream;

import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.Issue;
import com.github.tteofili.jtm.feed.jira.JiraFeedReader;
import com.github.tteofili.jtm.pipeline.functions.AverageEmbeddingsFunction;
import com.github.tteofili.jtm.pipeline.functions.CASCreationFunction;
import com.github.tteofili.jtm.pipeline.functions.EmbeddingTopicModelFunction;
import com.github.tteofili.jtm.pipeline.functions.OpenNLPChunkFunction;
import com.github.tteofili.jtm.pipeline.functions.OpenNLPPosTagFunction;
import com.github.tteofili.jtm.pipeline.functions.OpenNLPTokenizeFunction;
import com.github.tteofili.jtm.pipeline.functions.TopicsWriterFunction;
import com.github.tteofili.jtm.pipeline.functions.UpdateEmbeddingsFunction;
import com.github.tteofili.jtm.pipeline.utils.KryoCasSerializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;

/**
 * Streaming analysis for topic modelling of Jira issues
 */
public class StreamingIssuesTMPipeline {

  public static final String TOKEN_TYPE = "opennlp.uima.Token";
  public static final String CHUNK_TYPE = "opennlp.uima.Chunk";
  public static final String POS_FEATURE_NAME = "pos";
  public static final String CHUNK_FEATURE_NAME = "chunkType";
  public static final String[] stopTags = new String[] {"CD", "VB", "RB", "JJ", "VBN", "VBG", ".", "JJS", "FW", "VBD"};

  public static void main(String[] args) throws Exception {

    String path = args[0];
    if (path == null) {
      System.err.println("please supply the absolute path to a Jira XML export file");
      System.exit(-1);
    }
    Feed feed = new JiraFeedReader().read(new FileInputStream(path));

    final StreamExecutionEnvironment env =
        StreamExecutionEnvironment.getExecutionEnvironment();

    env.getConfig().registerTypeWithKryoSerializer(CASImpl.class, KryoCasSerializer.class);

    DataStreamSource<Issue> jiraIssueDataStreamSource = env.fromCollection(feed.getIssues().getIssues()).setParallelism(1);

    // create stream on CAS
    DataStream<CAS> casStream = jiraIssueDataStreamSource.map(new CASCreationFunction());

    // perform analysis
    casStream
        .map(new OpenNLPTokenizeFunction())
        .map(new OpenNLPPosTagFunction())
        .map(new OpenNLPChunkFunction())
        .countWindowAll(100).apply(new UpdateEmbeddingsFunction())
        .countWindowAll(2).apply(new AverageEmbeddingsFunction())
        .map(new EmbeddingTopicModelFunction()).addSink(new TopicsWriterFunction());

    env.execute();

  }

}
