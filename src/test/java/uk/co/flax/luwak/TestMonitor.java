package uk.co.flax.luwak;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import uk.co.flax.luwak.impl.MatchAllPresearcher;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.flax.luwak.util.MatchesAssert.assertThat;

/**
 * Copyright (c) 2013 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TestMonitor {

    static final String textfield = "textfield";

    private Monitor monitor = new Monitor(new MatchAllPresearcher());

    @Before
    public void setUp() {
        monitor.reset();
    }

    @Test
    public void singleTermQueryMatchesSingleDocument() {

        String document = "This is a test document";

        Query query = new TermQuery(new Term(textfield, "test"));
        MonitorQuery mq = new MonitorQuery("query1", query);

        InputDocument doc = InputDocument.builder("doc1")
                .addField(textfield, document, WHITESPACE)
                .build();

        monitor.update(mq);

        assertThat(monitor.match(doc))
                .matches("doc1")
                .hasMatchCount(1)
                .matchesQuery("query1");

    }

    @Test(expected = IllegalStateException.class)
    public void monitorWithNoQueriesThrowsException() {
        InputDocument doc = InputDocument.builder("doc1").build();
        monitor.match(doc);
        fail("Monitor with no queries should have thrown an IllegalStateException");
    }

    @Test
    public void updatesOverwriteOldQueries() {
        MonitorQuery mq = new MonitorQuery("query1", new TermQuery(new Term(textfield, "this")));
        monitor.update(mq);

        MonitorQuery mq2 = new MonitorQuery("query1", new TermQuery(new Term(textfield, "that")));
        monitor.update(mq2);

        InputDocument doc = InputDocument.builder("doc1").addField(textfield, "that", WHITESPACE).build();
        assertThat(monitor.match(doc))
                .hasQueriesRunCount(1)
                .matchesQuery("query1");
    }

    static final Analyzer WHITESPACE = new WhitespaceAnalyzer(Version.LUCENE_50);

    @Test
    public void canAddMonitorQuerySubclasses() {

        class TestQuery extends MonitorQuery {
            public TestQuery(String id, String query) {
                super(id, new TermQuery(new Term("field", query)));
            }
        };

        List<TestQuery> queries = ImmutableList.of(new TestQuery("1", "foo"),
                                                   new TestQuery("2", "bar"));

        monitor.update(queries);

        Assertions.assertThat(monitor.getQuery("1"))
                .isNotNull()
                .isInstanceOf(TestQuery.class);

    }

    /**
    @Test
    public void testHandleUncommitedAddQueries() {
        String document = "This is a test document";

        Query query = new TermQuery(new Term(textfield, "test"));
        MonitorQuery mq = new MonitorQuery("query1", query);
        monitor = new Monitor(new MatchAllPresearcher(), 10);
        monitor.update(mq);

        Assertions.assertThat(monitor.getQueryCount()).isEqualTo(0);
        Assertions.assertThat(monitor.getUncommitedQueryCount()).isEqualTo(1);
        Assertions.assertThat(monitor.getQuery("query1"))
                .isNotNull();

        InputDocument doc = InputDocument.builder("doc1")
                .addField(textfield, document, WHITESPACE)
                .build();

        assertThat(monitor.match(doc))
                .matches("doc1")
                .hasMatchCount(1)
                .matchesQuery("query1")
                    .withHitCount(1)
                    .inField(textfield)
                        .withHit(new QueryMatch.Hit(3, 10, 3, 14));
    }

    @Test
    public void testFlushUncommitedAddQueries() {
        String document = "This is a test document";

        MonitorQuery mq1 = new MonitorQuery("query1", new TermQuery(new Term(textfield, "test")));
        MonitorQuery mq2 = new MonitorQuery("query2", new TermQuery(new Term(textfield, "This")));
        MonitorQuery mq3 = new MonitorQuery("query3", new TermQuery(new Term(textfield, "is")));
        monitor = new Monitor(new MatchAllPresearcher(), 1);
        monitor.update(mq1);
        monitor.update(mq2);
        monitor.update(mq3);

        Assertions.assertThat(monitor.getQueryCount()).isEqualTo(2);
        Assertions.assertThat(monitor.getUncommitedQueryCount()).isEqualTo(1);
        Assertions.assertThat(monitor.getQuery("query1"))
                .isNotNull();

        InputDocument doc = InputDocument.builder("doc1")
                .addField(textfield, document, WHITESPACE)
                .build();

        assertThat(monitor.match(doc))
                .hasQueriesRunCount(3)
                .matches("doc1")
                .hasMatchCount(3)
                .matchesQuery("query1")
                .withHitCount(1)
                .inField(textfield)
                .withHit(new QueryMatch.Hit(3, 10, 3, 14));

        assertThat(monitor.match(doc))
                .hasQueriesRunCount(3)
                .matches("doc1")
                .hasMatchCount(3)
                .matchesQuery("query2")
                .withHitCount(1)
                .inField(textfield)
                .withHit(new QueryMatch.Hit(0, 0, 0, 4));

        assertThat(monitor.match(doc))
                .hasQueriesRunCount(3)
                .matches("doc1")
                .hasMatchCount(3)
                .matchesQuery("query3")
                .withHitCount(1)
                .inField(textfield)
                .withHit(new QueryMatch.Hit(1, 5, 1, 7));
    }
    **/

    @Test
    public void testHandleUncommitedDeleteQueries() {
        String document = "This is a test document";

        MonitorQuery mq1 = new MonitorQuery("query1", new TermQuery(new Term(textfield, "test")));
        MonitorQuery mq2 = new MonitorQuery("query2", new TermQuery(new Term(textfield, "This")));
        MonitorQuery mq3 = new MonitorQuery("query3", new TermQuery(new Term(textfield, "is")));
        monitor = new Monitor(new MatchAllPresearcher(), 1);
        monitor.update(mq1);
        monitor.update(mq2);
        monitor.update(mq3);
        List<MonitorQuery> listToDelete = new ArrayList<>();
        listToDelete.add(new MonitorQuery("query3", null));
        monitor.update(new ArrayList<MonitorQuery>(), listToDelete);

        Assertions.assertThat(monitor.getQueryCount()).isEqualTo(2);
        Assertions.assertThat(monitor.getUncommitedQueryCount()).isEqualTo(1);

        InputDocument doc = InputDocument.builder("doc1")
                .addField(textfield, document, WHITESPACE)
                .build();

        assertThat(monitor.match(doc))
                .hasQueriesRunCount(2)
                .matches("doc1")
                .hasMatchCount(2);

        listToDelete = new ArrayList<>();
        listToDelete.add(new MonitorQuery("query2", null));
        monitor.update(new ArrayList<MonitorQuery>(), listToDelete);

        assertThat(monitor.match(doc))
                .hasQueriesRunCount(1)
                .matches("doc1")
                .hasMatchCount(1);
    }

    public void errorsAreHandled() throws Exception {

        Query badquery = mock(Query.class);
        when(badquery.rewrite(any(IndexReader.class))).thenThrow(new IOException("Error rewriting!"));

        monitor.update(new MonitorQuery("badquery", badquery));
        monitor.update(new MonitorQuery("goodquery", new TermQuery(new Term(textfield, "goodquery"))));

        InputDocument doc = InputDocument.builder("doc1").addField(textfield, "goodquery", WHITESPACE).build();
        DocumentMatches matches = monitor.match(doc);

        Assertions.assertThat(matches.matches()).hasSize(1);
        Assertions.assertThat(matches.errors()).hasSize(1);
        Assertions.assertThat(matches.errors().get(0).error)
                .hasMessage("Error rewriting!")
                .isInstanceOf(IOException.class);

        Assertions.assertThat((long)matches.getMatchStats().querycount).isEqualTo(monitor.getQueryCount());

    }
}
