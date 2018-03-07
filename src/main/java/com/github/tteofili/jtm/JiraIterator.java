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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;

/**
 * DL4J {@link LabelAwareIterator} over {@link JiraIssue}s
 */
public class JiraIterator implements LabelAwareIterator {

  private final Map<String, JiraIssue> issues;
  private final boolean includeComments;
  private final List<JiraComment> commentsList;

  private JiraIssue currentIssue;
  private Iterator<Map.Entry<String, JiraIssue>> issuesIterator;
  private Iterator<JiraComment> commentIterator;

  private String currentLabel = null;
  private LabelsSource labelSource;


  JiraIterator(Map<String, JiraIssue> issues, boolean includeComments) {
    this.issues = issues;
    this.includeComments = includeComments;
    this.commentsList = new LinkedList<>();
    this.labelSource = extractLabels();
    issuesIterator = issues.entrySet().iterator();
  }

  private LabelsSource extractLabels() {
    List<String> labels = new LinkedList<>();
    labels.addAll(issues.keySet());
    if (includeComments) {
      for (JiraIssue issue : issues.values()) {
        for (JiraComment jiraComment : issue.getComments()) {
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
      JiraComment jiraComment = commentIterator.next();
      currentLabel = jiraComment.getId();
      sentence = jiraComment.toString();
    } else {
      currentIssue = issuesIterator.next().getValue();
      List<JiraComment> comments = currentIssue.getComments();
      this.commentsList.addAll(comments);
      commentIterator = comments.iterator();
      currentLabel = currentIssue.getId();
      sentence = currentIssue.asString();
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
    issuesIterator = issues.entrySet().iterator();
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

  public Map<String, JiraIssue> getIssues() {
    return issues;
  }

  LabelAwareIterator commentsIterator() {
    final Iterator<JiraComment> iterator = commentsList.iterator();
    return new LabelAwareIterator() {

      LabelsSource labelsSource = new LabelsSource();

      @Override
      public boolean hasNextDocument() {
        return iterator.hasNext();
      }

      @Override
      public LabelledDocument nextDocument() {
        JiraComment next = iterator.next();
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
