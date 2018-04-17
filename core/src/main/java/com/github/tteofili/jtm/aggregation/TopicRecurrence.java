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
package com.github.tteofili.jtm.aggregation;

import java.util.Collection;
import java.util.TreeSet;

public final class TopicRecurrence implements Comparable<TopicRecurrence> {

    private final Collection<String> recurringIssues = new TreeSet<>();

    private final String topic;

    public TopicRecurrence(String topic) {
        this.topic = topic;
    }

    public void addRecurringIssue(String issueId) {
        recurringIssues.add(issueId);
    }

    public void addRecurringIssues(Collection<String> issuesId) {
        recurringIssues.addAll(issuesId);
    }

    public Collection<String> getRecurringIssues() {
        return recurringIssues;
    }

    public String getTopic() {
        return topic;
    }

    public int getRecurencesCount() {
        return recurringIssues.size();
    }

    @Override
    public int compareTo(TopicRecurrence o) {
        return getRecurencesCount() - o.getRecurencesCount();
    }

    @Override
    public String toString() {
        return topic + ": " + recurringIssues;
    }

}
