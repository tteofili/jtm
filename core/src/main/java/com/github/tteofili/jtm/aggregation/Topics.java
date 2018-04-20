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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class Topics {

    private final Map<String, TreeSet<String>> recurrences = new ConcurrentHashMap<>();

    public void add(String topic, String issueId) {
        recurrences.computeIfAbsent(topic, k -> new TreeSet<String>())
                   .add(issueId);
    }

    public Set<Entry<String, TreeSet<String>>> entrySet() {
        return recurrences.entrySet();
    }

    public boolean isEmpty() {
        return recurrences.isEmpty();
    }

}
