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
 *
 */
public class JiraIterator implements LabelAwareIterator {

  private final Map<String, JiraIssue> issues;

  private JiraIssue currentIssue;
  private Iterator<Map.Entry<String, JiraIssue>> issuesIterator;
  private Iterator<JiraComment> commentIterator;

  private String currentLabel = null;
  private LabelsSource labelSource;


  JiraIterator(Map<String, JiraIssue> issues) {
    this.issues = issues;
    this.labelSource = extractLabels();
    issuesIterator = issues.entrySet().iterator();
  }

  private LabelsSource extractLabels() {
    List<String> labels = new LinkedList<>();
    labels.addAll(issues.keySet());
    for (JiraIssue issue : issues.values()) {
      for (JiraComment jiraComment : issue.getComments()) {
        labels.add(jiraComment.getId());
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
    if (commentIterator != null && commentIterator.hasNext()) {
      JiraComment jiraComment = commentIterator.next();
      currentLabel = jiraComment.getId();
      sentence = jiraComment.toString();

    } else {
      currentIssue = issuesIterator.next().getValue();
      commentIterator = currentIssue.getComments().iterator();
      currentLabel = currentIssue.getId();
      sentence = currentIssue.asString();
    }

    return sentence;
  }

  @Override
  public boolean hasNext() {
    return issuesIterator != null && issuesIterator.hasNext() || (commentIterator != null && commentIterator.hasNext());
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
}
