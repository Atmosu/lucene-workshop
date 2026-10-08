// STEP 4 — Searching: parse a query, rank with BM25, explain the scores.
//
// Run:  java -cp "lib/*" Search.java "chocolate cake"
//
// Options (put them BEFORE the query):
//   --explain          show Lucene's full BM25 breakdown for each hit
//   --top N            number of results (default 5)
//   --k1 X --b Y       change BM25 parameters (defaults k1=1.2, b=0.75)
//   --classic          use classic TF-IDF (ClassicSimilarity) instead of BM25
//   --cuisine C        filter: only this cuisine (italian, indian, mexican, japanese, french, american)
//   --minutes A-B      filter: only recipes that take between A and B minutes
//
// Query syntax (Lucene classic QueryParser), default field = description:
//   chocolate cake            OR of both words (matching more words = higher score)
//   +chicken +spicy           both words required
//   chicken -rice             'rice' must NOT appear in the description
//   "olive oil"               phrase (the words must be next to each other)
//   title:pancakes            search another field
//   spicy^3 chicken           boost a word
//   choclate~                 fuzzy match (tolerates typos)

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.NIOFSDirectory;

import java.nio.file.Path;

public class Search {
    // Hide Lucene's JVM performance hints so the workshop output stays readable
    static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.apache.lucene");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    public static void main(String[] args) throws Exception {
        boolean explain = false, classic = false;
        int top = 5;
        float k1 = 1.2f, b = 0.75f;
        String cuisine = null, minutes = null, queryText = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--explain" -> explain = true;
                case "--classic" -> classic = true;
                case "--top" -> top = Integer.parseInt(args[++i]);
                case "--k1" -> k1 = Float.parseFloat(args[++i]);
                case "--b" -> b = Float.parseFloat(args[++i]);
                case "--cuisine" -> cuisine = args[++i];
                case "--minutes" -> minutes = args[++i];
                default -> queryText = (queryText == null) ? args[i] : queryText + " " + args[i];
            }
        }
        if (queryText == null) {
            System.out.println("Usage: java -cp \"lib/*\" Search.java [options] \"your query\"");
            return;
        }

        try (DirectoryReader reader = DirectoryReader.open(new NIOFSDirectory(Path.of("index")))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(classic ? new ClassicSimilarity() : new BM25Similarity(k1, b));

            // 1. Parse: the query text goes through the SAME analyzer as the documents
            Analyzer analyzer = new EnglishAnalyzer();
            Query userQuery = new QueryParser("description", analyzer).parse(queryText);

            // 2. Optional filters: they restrict WHICH docs match but do not change scores
            BooleanQuery.Builder builder = new BooleanQuery.Builder().add(userQuery, BooleanClause.Occur.MUST);
            if (cuisine != null)
                builder.add(new TermQuery(new Term("cuisine", cuisine)), BooleanClause.Occur.FILTER);
            if (minutes != null) {
                String[] m = minutes.split("-");
                builder.add(IntPoint.newRangeQuery("minutes", Integer.parseInt(m[0]), Integer.parseInt(m[1])),
                        BooleanClause.Occur.FILTER);
            }
            Query query = builder.build();

            System.out.println("Query text   : " + queryText);
            System.out.println("Parsed query : " + userQuery + "   <- after analysis");
            System.out.println("Scoring      : " + (classic ? "ClassicSimilarity (TF-IDF)" : "BM25 (k1=" + k1 + ", b=" + b + ")"));
            if (cuisine != null || minutes != null)
                System.out.println("Filters      : " + (cuisine != null ? "cuisine=" + cuisine + " " : "") + (minutes != null ? "minutes=" + minutes : ""));

            // 3. Search and print the ranked list
            TopDocs results = searcher.search(query, top);
            System.out.println("Matches      : " + searcher.count(query) + " documents\n");

            StoredFields stored = searcher.storedFields();
            int rank = 1;
            for (ScoreDoc hit : results.scoreDocs) {
                Document d = stored.document(hit.doc);
                System.out.printf("%d. [%.4f] %s (%s, %s min)%n", rank++, hit.score, d.get("title"), d.get("cuisine"), d.get("minutes"));
                if (explain) {
                    System.out.println(indent(searcher.explain(query, hit.doc).toString()));
                }
            }
        }
    }

    static String indent(String s) {
        return "      " + s.trim().replace("\n", "\n      ");
    }
}
