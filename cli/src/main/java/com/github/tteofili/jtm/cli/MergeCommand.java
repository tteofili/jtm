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
package com.github.tteofili.jtm.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import com.github.tteofili.jtm.feed.jira.BuildInfo;
import com.github.tteofili.jtm.feed.jira.Feed;
import com.github.tteofili.jtm.feed.jira.IssuesCollection;
import com.github.tteofili.jtm.feed.jira.io.stax.JiraFeedStaxWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "merge", description = "Merges all the input feeds in a single one")
public class MergeCommand extends AbstractCommand {

    @Option(names = { "-o", "--output" }, description = "The output file where writing the resulting feed.")
    private File output;

    private Feed target;

    @Override
    protected void setUp() throws Exception {
        log.info("Initializing target feed...");

        target = new Feed();
        target.setVersion("1.0");

        IssuesCollection issuesCollection = new IssuesCollection();
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setBuildDate(new Date());
        buildInfo.setBuildNumber(System.currentTimeMillis());
        buildInfo.setVersion(System.getProperty("project.version"));
        issuesCollection.setBuildInfo(buildInfo);
        //issuesCollection.setLanguage("en");
        issuesCollection.setDescription("Auto-generated feed by JTM");
        target.setIssues(issuesCollection);
    }

    @Override
    public void analyze(Feed feed) throws Exception {
        log.info("Merging {} to the target feed...", feed.getIssues().getTitle());
        target.getIssues().getIssues().addAll(feed.getIssues().getIssues());
        log.info("{} issues successfully merged to the target feed", feed.getIssues().getIssues().size());
    }

    @Override
    protected void tearDown() throws Exception {
        File parentDir = output.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        log.info("Writing merge result to '{}' output file...", this.output);

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(this.output);
            new JiraFeedStaxWriter().write(stream, target);
        } finally {
            log.info("Merge written to '{}'", this.output);

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // swallow it
                }
            }
        }
    }

}
