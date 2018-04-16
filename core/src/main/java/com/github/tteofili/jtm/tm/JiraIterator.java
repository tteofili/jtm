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
package com.github.tteofili.jtm.tm;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;

import com.github.tteofili.jtm.feed.Comment;
import com.github.tteofili.jtm.feed.Issue;
import com.google.common.base.Joiner;

/**
 * DL4J {@link LabelAwareIterator} over {@link Issue}s
 */
public class JiraIterator implements LabelAwareIterator {

  private final Collection<Issue> issues;
  private final boolean includeComments;
  private final List<Comment> commentsList;

  private Issue currentIssue;
  private Iterator<Issue> issuesIterator;
  private Iterator<Comment> commentIterator;

  private String currentLabel = null;
  private LabelsSource labelSource;


  public JiraIterator(Collection<Issue> issues, boolean includeComments) {
    this.issues = issues;
    this.includeComments = includeComments;
    this.commentsList = new LinkedList<>();
    this.labelSource = extractLabels();
    issuesIterator = issues.iterator();
  }

  private LabelsSource extractLabels() {
    List<String> labels = new LinkedList<>();

    for (Issue issue : issues) {
      labels.add(issue.getKey().getValue());
      if (includeComments) {
        for (Comment jiraComment : issue.getComments()) {
          labels.add(jiraComment.getId());
        }
      }
    }
    return new LabelsSource(labels);
  }

  private String currentLabel() {
    return currentLabel;
  }

  private List<String> currentLabels() {
    return Collections.singletonList(currentLabel());
  }

  private String nextSentence() {
    String sentence;
    if (includeComments && commentIterator != null && commentIterator.hasNext()) {
      Comment jiraComment = commentIterator.next();
      currentLabel = jiraComment.getId();
      sentence = jiraComment.toString();
    } else {
      currentIssue = issuesIterator.next();
      List<Comment> comments = currentIssue.getComments();
      this.commentsList.addAll(comments);
      commentIterator = comments.iterator();
      currentLabel = currentIssue.getKey().getValue();
      sentence = Joiner.on(' ').join(currentIssue.getLabels(),
                                     currentIssue.getTitle(),
                                     currentIssue.getDescription(),
                                     currentIssue.getSummary());
    }

    return sentence;
  }

  @Override
  public boolean hasNext() {
    return issuesIterator != null && issuesIterator.hasNext() || (includeComments && commentIterator != null && commentIterator.hasNext());
  }

  @Override
  public LabelledDocument next() {
    String s = nextSentence();
    LabelledDocument labelledDocument = new LabelledDocument();
    labelledDocument.setLabels(currentLabels());
    labelledDocument.setContent(s);
    return labelledDocument;
  }

  @Override
  public boolean hasNextDocument() {
    return hasNext();
  }

  @Override
  public LabelledDocument nextDocument() {
    return next();
  }

  @Override
  public void reset() {
    issuesIterator = issues.iterator();
    commentIterator = null;
    currentLabel = null;
    currentIssue = null;
  }

  @Override
  public LabelsSource getLabelsSource() {
    return this.labelSource;
  }

  @Override
  public void shutdown() {
    issuesIterator = null;
    commentIterator = null;
    currentLabel = null;
    currentIssue = null;
  }

  public Collection<Issue> getIssues() {
    return issues;
  }

  LabelAwareIterator commentsIterator() {
    final Iterator<Comment> iterator = commentsList.iterator();
    return new LabelAwareIterator() {

      @Override
      public boolean hasNextDocument() {
        return iterator.hasNext();
      }

      @Override
      public LabelledDocument nextDocument() {
        Comment next = iterator.next();
        LabelledDocument document = new LabelledDocument();
        String id = next.getId();
        labelSource.storeLabel(id);
        document.setLabels(Collections.singletonList(id));
        document.setContent(next.toString());
        return document;
      }

      @Override
      public void reset() {

      }

      @Override
      public LabelsSource getLabelsSource() {
        return labelSource;
      }

      @Override
      public void shutdown() {

      }

      @Override
      public boolean hasNext() {
        return hasNextDocument();
      }

      @Override
      public LabelledDocument next() {
        return nextDocument();
      }
    };
  }
}
