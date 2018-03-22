package com.github.tteofili.jtm.feed.io.stax;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.tteofili.jtm.feed.Feed;

@RunWith(Parameterized.class)
public class JiraFeedStaxReaderTest{

    private final String resource;

    public JiraFeedStaxReaderTest(String resource) {
        this.resource = resource;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          {"/opennlp-issues.xml"},
          {"/lucene-issues.xml"},
          {"/oak-issues.xml"},
      });
    }

    @Test
    public void testParse() throws Exception {
        InputStream input = getClass().getResourceAsStream(resource);
        Feed feed = new JiraFeedStaxReader().read(input, false);
        input.close();
        assertEquals(1000, feed.getIssues().getIssues().size());
    }
}
