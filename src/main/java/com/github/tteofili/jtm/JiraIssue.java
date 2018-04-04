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
package com.github.tteofili.jtm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * Pojo for a Jira issue
 */
public class JiraIssue {

  private final String title;
  private final String link;
  private final String projectId;
  private final String description;
  private final String id;
  private final String summary;
  private final String typeId;
  private final String reporter;
  private final String assignee;
  private final String resolution;
  private final List<String> labels;
  private final List<String> topics;
  private final String created;
  private final String updated;
  private final String component;
  private final List<JiraComment> comments;

  public JiraIssue(String title, String link, String projectId, String description, String id, String summary, String typeId, String reporter, String assignee, String resolution, List<String> labels, String created, String updated, String component, List<JiraComment> comments) {
    this.title = title;
    this.link = link;
    this.projectId = projectId;
    this.description = description;
    this.id = id;
    this.summary = summary;
    this.typeId = typeId;
    this.reporter = reporter;
    this.assignee = assignee;
    this.resolution = resolution;
    this.labels = labels;
    this.created = created;
    this.updated = updated;
    this.component = component;
    this.comments = comments;
    topics = new LinkedList<>();
  }

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDescription() {
    return description;
  }

  public String getId() {
    return id;
  }

  public String getSummary() {
    return summary;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getReporter() {
    return reporter;
  }

  public String getAssignee() {
    return assignee;
  }

  public String getResolution() {
    return resolution;
  }

  public List<String> getLabels() {
    return labels;
  }

  public String getCreated() {
    return created;
  }

  public String getUpdated() {
    return updated;
  }

  public String getComponent() {
    return component;
  }

  public List<JiraComment> getComments() {
    return comments;
  }

  public List<String> getTopics() {
    return topics;
  }

  public void addTopics(Collection<String> topic) {
    topics.addAll(topic);
  }

  public String asString() {
    return Joiner.on(' ').join(labels, title, description, summary);
  }



  @Override
  public String toString() {
    return "JiraIssue{" +
        "title='" + title + '\'' +
        ", link='" + link + '\'' +
        ", projectId='" + projectId + '\'' +
        ", description='" + description + '\'' +
        ", id='" + id + '\'' +
        ", summary='" + summary + '\'' +
        ", typeId='" + typeId + '\'' +
        ", reporter='" + reporter + '\'' +
        ", assignee='" + assignee + '\'' +
        ", resolution='" + resolution + '\'' +
        ", labels=" + labels +
        ", created='" + created + '\'' +
        ", updated='" + updated + '\'' +
        ", component='" + component + '\'' +
        ", comments=" + comments +
        '}';
  }
}
