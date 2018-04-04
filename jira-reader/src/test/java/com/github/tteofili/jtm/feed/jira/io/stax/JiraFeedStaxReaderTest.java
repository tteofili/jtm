package com.github.tteofili.jtm.feed.jira.io.stax;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.tteofili.jtm.feed.jira.Feed;
import com.github.tteofili.jtm.feed.jira.IssuesCollection;
import com.github.tteofili.jtm.feed.jira.Range;

@RunWith(Parameterized.class)
public class JiraFeedStaxReaderTest{

    private final String resource;

    public JiraFeedStaxReaderTest(String resource) {
        this.resource = resource;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          {"cq.xml"},
          {"granite1.xml"},
          {"npr.xml"},
      });
    }

    @Test
    @Ignore
    public void testParse() throws Exception {
        InputStream input = getClass().getResourceAsStream(resource);
        Feed feed = new JiraFeedStaxReader().read(input, false);
        input.close();

        IssuesCollection issuesCollection = feed.getIssues();
        Range range = issuesCollection.getRange();

        assertEquals(range.getEnd() - range.getStart(), issuesCollection.getIssues().size());
    }
}
