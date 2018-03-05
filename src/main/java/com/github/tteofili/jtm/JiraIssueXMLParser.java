package com.github.tteofili.jtm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parser for Jira issues XML export
 */
public class JiraIssueXMLParser {

  private static final String ELEMENT_ITEM = "item";
  private static final String ELEMENT_TITLE = "title";
  private static final String ELEMENT_ID = "key";
  private static final String ELEMENT_LINK = "link";
  private static final String ELEMENT_PROJECT = "project";
  private static final String ELEMENT_SUMMARY = "summary";
  private static final String ELEMENT_DESCRIPTION = "description";
  private static final String ELEMENT_TYPE = "type";
  private static final String ELEMENT_REPORTER = "reporter";
  private static final String ELEMENT_ASSIGNEE = "assignee";
  private static final String ELEMENT_RESOLUTION = "resolution";
  private static final String ELEMENT_LABEL = "label";
  private static final String ELEMENT_CREATED = "created";
  private static final String ELEMENT_UPDATED = "updated";
  private static final String ELEMENT_COMPONENT = "component";
  private static final String ELEMENT_COMMENT = "comment";

  private final String file;
  private final XMLInputFactory factory = XMLInputFactory.newInstance();
  private final Map<String, JiraIssue> issues;

  JiraIssueXMLParser(final String file) {
    this.file = file;
    this.issues = new HashMap<>();
  }

  public Map<String, JiraIssue> parse() throws IOException, XMLStreamException {
    try (final InputStream stream = new FileInputStream(file)) {
      final XMLEventReader reader = factory.createXMLEventReader(stream);
      while (reader.hasNext()) {
        final XMLEvent event = reader.nextEvent();
        if (event.isStartElement() && event.asStartElement().getName()
            .getLocalPart().equals(ELEMENT_ITEM)) {
          parseItem(reader);
        }
      }
    }
    return issues;
  }

  private void parseItem(final XMLEventReader reader) throws XMLStreamException {
    String title = null;
    String link = null;
    String projectId = null;
    String description = null;
    String id = null;
    String summary = null;
    String type = null;
    String reporter = null;
    String assignee = null;
    String resolution = null;
    List<String> labels = new LinkedList<>();
    String created = null;
    String updated = null;
    String component = null;
    List<JiraComment> comments = new LinkedList<>();


    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_ITEM)) {
        final JiraIssue issue = new JiraIssue(title, link, projectId, description, id, summary, type, reporter, assignee, resolution, labels, created, updated, component, comments);
        issues.put(id, issue);
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        switch (elementName) {
          case ELEMENT_ASSIGNEE:
            assignee = reader.getElementText();
            break;
          case ELEMENT_TITLE:
            title = reader.getElementText();
            break;
          case ELEMENT_ID:
            id = reader.getElementText();
            break;
          case ELEMENT_LINK:
            link = reader.getElementText();
            break;
          case ELEMENT_PROJECT:
            projectId = reader.getElementText();
            break;
          case ELEMENT_DESCRIPTION:
            description = reader.getElementText();
            break;
          case ELEMENT_SUMMARY:
            summary = reader.getElementText();
            break;
          case ELEMENT_TYPE:
            type = reader.getElementText();
            break;
          case ELEMENT_REPORTER:
            reporter = reader.getElementText();
            break;
          case ELEMENT_RESOLUTION:
            resolution = reader.getElementText();
            break;
          case ELEMENT_LABEL:
            labels.add(reader.getElementText());
            break;
          case ELEMENT_CREATED:
            created = reader.getElementText();
            break;
          case ELEMENT_UPDATED:
            updated = reader.getElementText();
            break;
          case ELEMENT_COMMENT:
            String commentId = null;
            String author = null;
            String commentCreated = null;
            Iterator attributes = element.getAttributes();
            while (attributes.hasNext()) {
              Attribute next = (Attribute) attributes.next();
              if ("id".equals(next.getName().toString())) {
                commentId = next.getValue();
              } else if ("author".equals(next.getName().toString())) {
                author = next.getValue();
              } else if ("created".equals(next.getName().toString())) {
                commentCreated = next.getValue();
              }
            }
            String text = reader.getElementText();
            comments.add(new JiraComment(commentId, author, commentCreated, text));
            break;
          case ELEMENT_COMPONENT:
            component = reader.getElementText();
            break;
        }
      }
    }

  }
}