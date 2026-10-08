// STEP 1 — Analyzers: what actually goes into the inverted index?
//
// Run:  java -cp "lib/*" Analyze.java "The chefs were BAKING 3 chocolate-chip cookies!"
//
// An Analyzer = Tokenizer (split text into tokens) + TokenFilters
// (lowercase, remove stopwords, stem, ...). The SAME analyzer is applied
// to documents at index time and to the query at search time.

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.util.ArrayList;
import java.util.List;

public class Analyze {
    // Hide Lucene's JVM performance hints so the workshop output stays readable
    static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.apache.lucene");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    public static void main(String[] args) throws Exception {
        String text = args.length > 0 ? String.join(" ", args)
                : "The chefs were BAKING 3 chocolate-chip cookies!";
        System.out.println("Input: " + text + "\n");

        show("WhitespaceAnalyzer (split on spaces only)", new WhitespaceAnalyzer(), text);
        show("StandardAnalyzer   (Unicode tokenizer + lowercase)", new StandardAnalyzer(), text);
        show("EnglishAnalyzer    (+ stopwords + Porter stemming)", new EnglishAnalyzer(), text);
    }

    static void show(String label, Analyzer analyzer, String text) throws Exception {
        List<String> tokens = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("description", text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) tokens.add("[" + term + "]");
            ts.end();
        }
        System.out.println(label);
        System.out.println("  " + String.join(" ", tokens) + "\n");
    }
}
