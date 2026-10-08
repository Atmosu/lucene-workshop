// STEP 3 — Look inside the inverted index.
//
// Run:  java -cp "lib/*" InspectIndex.java chocolate
//
// Prints the postings list for a term: document frequency (df), and for
// each matching doc its term frequency (tf), length, and token positions.
// These are exactly the statistics BM25 uses.

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.NIOFSDirectory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InspectIndex {
    // Hide Lucene's JVM performance hints so the workshop output stays readable
    static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.apache.lucene");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    public static void main(String[] args) throws Exception {
        String word = args.length > 0 ? args[0] : "chocolate";
        String field = args.length > 1 ? args[1] : "description";

        // The word must go through the same analyzer used at index time
        String term = analyze(new EnglishAnalyzer(), field, word);
        System.out.println("Word \"" + word + "\" -> analyzed term \"" + term + "\" in field \"" + field + "\"\n");

        try (DirectoryReader reader = DirectoryReader.open(new NIOFSDirectory(Path.of("index")))) {
            Term t = new Term(field, term);
            int n = reader.numDocs();
            long totalTokens = reader.getSumTotalTermFreq(field);
            System.out.printf("N = %d docs, avg field length (avgdl) = %.1f tokens%n", n, (double) totalTokens / n);
            System.out.printf("df(%s) = %d   total occurrences = %d%n%n", term, reader.docFreq(t), reader.totalTermFreq(t));

            StoredFields stored = reader.storedFields();
            for (LeafReaderContext leaf : reader.leaves()) {
                PostingsEnum postings = leaf.reader().postings(t, PostingsEnum.POSITIONS);
                if (postings == null) continue;
                while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int docId = leaf.docBase + postings.docID();
                    int tf = postings.freq();
                    List<Integer> positions = new ArrayList<>();
                    for (int i = 0; i < tf; i++) positions.add(postings.nextPosition());
                    String title = stored.document(docId).get("title");
                    int len = fieldLength(stored.document(docId).get(field));
                    System.out.printf("  %-28s tf=%d  length=%3d  positions=%s%n", title, tf, len, positions);
                }
            }
        }
    }

    static String analyze(Analyzer a, String field, String text) throws Exception {
        try (TokenStream ts = a.tokenStream(field, text)) {
            CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            String out = ts.incrementToken() ? attr.toString() : text;
            ts.end();
            return out;
        }
    }

    static int fieldLength(String text) throws Exception {
        int count = 0;
        try (TokenStream ts = new EnglishAnalyzer().tokenStream("x", text)) {
            ts.reset();
            while (ts.incrementToken()) count++;
            ts.end();
        }
        return count;
    }
}
