// STEP 2 — Indexing: turn data/recipes.tsv into a Lucene index on disk.
//
// Run:  java -cp "lib/*" BuildIndex.java
//
// A Lucene Document is a set of Fields. The field TYPE decides what
// happens to the value:
//   TextField   -> analyzed (split into words, stemmed) -> searchable by words, ranked by BM25
//   StringField -> NOT analyzed, indexed as one exact token (ids, categories)
//   IntPoint    -> numeric index for fast range filters (e.g. 0-30 minutes)
//   StoredField -> not searchable, just stored so we can display it

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.NIOFSDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BuildIndex {
    // Hide Lucene's JVM performance hints so the workshop output stays readable
    static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.apache.lucene");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    public static void main(String[] args) throws Exception {
        Path indexDir = Path.of("index");
        Analyzer analyzer = new EnglishAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);   // rebuild from scratch each run
        config.setSimilarity(new BM25Similarity());              // the default anyway (k1=1.2, b=0.75)

        List<String> lines = Files.readAllLines(Path.of("data/recipes.tsv"));
        try (NIOFSDirectory dir = new NIOFSDirectory(indexDir);
             IndexWriter writer = new IndexWriter(dir, config)) {

            for (String line : lines.subList(1, lines.size())) {      // skip header row
                String[] c = line.split("\t");
                Document doc = new Document();
                doc.add(new StringField("id", c[0], Field.Store.YES));
                doc.add(new TextField("title", c[1], Field.Store.YES));
                doc.add(new StringField("cuisine", c[2], Field.Store.YES));
                doc.add(new IntPoint("minutes", Integer.parseInt(c[3])));
                doc.add(new StoredField("minutes", Integer.parseInt(c[3])));
                doc.add(new TextField("description", c[4], Field.Store.YES));
                writer.addDocument(doc);
                System.out.printf("Indexed doc %-2s %s%n", c[0], c[1]);
            }
            writer.commit();
            System.out.println("\nIndexed " + writer.getDocStats().numDocs + " documents into ./" + indexDir);
        }
    }
}
