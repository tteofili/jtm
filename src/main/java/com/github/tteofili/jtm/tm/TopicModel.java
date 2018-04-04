package com.github.tteofili.jtm.tm;

import java.util.Collection;

import com.github.tteofili.jtm.JiraIssue;

/**
 * Topic modeling API for training and inference
 */
public interface TopicModel {

  /**
   * Fit the model with respect to the given issues
   * @param issues the Jira issues
   */
  void fit(Collection<JiraIssue> issues);

  /**
   * Extract top {@code n} topics for the document having the given identifier
   * @param topN number of topics
   * @param documentId the document identifier
   * @return a collection of extracted topics
   */
  Collection<String> extractTopics(int topN, String documentId);
}
