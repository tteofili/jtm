package com.github.tteofili.jtm;

import com.google.common.base.Joiner;

public class JiraComment {
  private final String id;
  private final String author;
  private final String created;
  private final String text;

  public JiraComment(String id, String author, String created, String text) {
    this.id = id;
    this.author = author;
    this.created = created;
    this.text = text;
  }

  public String getId() {
    return id;
  }

  public String getAuthor() {
    return author;
  }

  public String getCreated() {
    return created;
  }

  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return Joiner.on(' ').join(text, author);
  }
}