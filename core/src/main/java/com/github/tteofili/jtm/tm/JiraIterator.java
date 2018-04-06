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

import com.github.tteofili.jtm.JiraComment;
import com.github.tteofili.jtm.JiraIssue;

/**
 * DL4J {@link LabelAwareIterator} over {@link JiraIssue}s
 */
public class JiraIterator implements LabelAwareIterator {

  private final Collection<JiraIssue> issues;
  private final boolean includeComments;
  private final List<JiraComment> commentsList;

  private JiraIssue currentIssue;
  private Iterator<JiraIssue> issuesIterator;
  private Iterator<JiraComment> commentIterator;

  private String currentLabel = null;
  private LabelsSource labelSource;


  public JiraIterator(Collection<JiraIssue> issues, boolean includeComments) {
    this.issues = issues;
    this.includeComments = includeComments;
    this.commentsList = new LinkedList<>();
    this.labelSource = extractLabels();
    issuesIterator = issues.iterator();
  }

  private LabelsSource extractLabels() {
    List<String> labels = new LinkedList<>();

    for (JiraIssue issue : issues) {
      labels.add(issue.getId());
      if (includeComments) {
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
      currentIssue = issuesIterator.next();
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

  public Collection<JiraIssue> getIssues() {
    return issues;
  }

  LabelAwareIterator commentsIterator() {
    final Iterator<JiraComment> iterator = commentsList.iterator();
    return new LabelAwareIterator() {

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
