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
package com.github.tteofili.jtm.feed.indexing;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import com.github.tteofili.jtm.feed.Issue;
import com.github.tteofili.jtm.feed.User;

public class IndexWriter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E, d MMM yyyy hh:mm:ss Z");

    private final JsonGenerator generator;

    public IndexWriter(OutputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Target stream cannot be null");
        }
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonGeneratorFactory jf = Json.createGeneratorFactory(properties);
        generator = jf.createGenerator(stream);
    }

    public void write(Collection<Issue> issues) {
        if (issues == null) {
            throw new IllegalArgumentException("Issues can not be null");
        }

        generator.writeStartArray();

        issues.forEach(issue -> {
            generator.writeStartObject()
                     .write("id", issue.getKey().getValue())
                     .write("description", issue.getDescription())
                     .write("link", issue.getLink())
                     .write("projectId", issue.getProject().getName())
                     .write("resolution", issue.getResolution())
                     .write("summary", issue.getSummary())
                     .write("title", issue.getTitle())
                     .write("typeId", issue.getType());

            write("created", issue.getCreated());
            write("updated", issue.getUpdated());
            write("assignee", issue.getAssignee());
            write("reporter", issue.getReporter());
            write("components", issue.getComponents());
            write("labels", issue.getLabels());
            write("topics", issue.getTopics());

            generator.writeEnd();
        });

        generator.writeEnd().close();
    }

    private void write(String name, User user) {
        generator.writeStartObject(name)
                 .write("name", user.getName())
                 .write("username", user.getUsername())
                 .writeEnd();
    }

    private void write(String name, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        generator.writeStartArray(name);
        values.forEach(value -> generator.write(value));
        generator.writeEnd();
    }

    private void write(String name, Date date) {
        generator.write(name, DATE_FORMAT.format(date));
    }

}
