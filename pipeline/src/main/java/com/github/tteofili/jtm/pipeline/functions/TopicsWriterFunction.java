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
package com.github.tteofili.jtm.pipeline.functions;

import java.io.File;
import java.io.FileOutputStream;
import java.security.SecureRandom;

import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.aggregation.TopicsWriter;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

/**
 * {@link SinkFunction} to write topics to file
 */
public class TopicsWriterFunction implements SinkFunction<Topics> {

  private final SecureRandom random = new SecureRandom();

  @Override
  public void invoke(Topics value, Context context) throws Exception {
    TopicsWriter topicsWriter = new TopicsWriter();
    topicsWriter.write(value, new FileOutputStream(new File("topics" + random.nextFloat() + ".json")));
  }

}
