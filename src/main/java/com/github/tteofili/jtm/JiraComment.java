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
    return Joiner.on(' ').join(author, text);
  }
}