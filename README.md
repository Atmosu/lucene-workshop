# Apache Lucene Hands-On Workshop

**Information Retrieval: Search Engines and IR Frameworks**
Presented by **Suman KC** and **Shaurav Parajuli**

Today you will build a small search engine over 17 recipes with **Apache Lucene**,
the open-source Java library behind Elasticsearch, OpenSearch and Solr. You will
create an index, see what an analyzer does to text, run searches, and find out
*why* Lucene ranks one recipe above another.

You do not need to know Java. You only run commands and read the output.

---

## 0. Setup

### Option 1 (recommended): GitHub Codespaces, nothing to install

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Atmosu/lucene-workshop?quickstart=1)

1. Sign in to GitHub (a free account is enough).
2. Click the button above, then **Create codespace**.
3. Wait 1-2 minutes. VS Code opens in your browser with Java 21 installed and the index already built.
4. Click in the **Terminal** panel at the bottom and run the commands below exactly as written. Mac, Windows and Chromebook users all type the same thing.

### Option 2 (backup): on your own laptop, about 5 minutes

**1. Install Java 21 or newer.** Check with `java -version`.

| OS | Command |
|---|---|
| macOS | `brew install openjdk@21` (then follow the PATH hint it prints) |
| Windows | `winget install Microsoft.OpenJDK.21` |
| Ubuntu | `sudo apt install openjdk-21-jdk` |

**2. Lucene jars.** The zip already contains `lib/` with 5 jars. If yours is empty, run `./setup.sh` (on Windows, `.\setup.ps1`).

**3. Open a terminal inside the `lucene-workshop` folder** and run every command from there.

```
lucene-workshop/
├── data/recipes.tsv    17 short recipes (our document collection)
├── Analyze.java        Step 1: analyzers
├── BuildIndex.java     Step 2: build the index
├── InspectIndex.java   Step 3: look inside the inverted index
├── Search.java         Steps 4-5: search, BM25 scores, experiments
└── lib/                Lucene jars
```

Java 21 can run a `.java` file directly, so there is nothing to compile.

---

## The collection

17 recipes. Each has an `id`, a `title`, a `cuisine` (italian, indian, mexican,
japanese, french, american), the cooking time in `minutes`, and a short
`description`. We search the descriptions.

---

## Step 1: Analyzers decide what a "word" is (2 min)

```bash
java -cp "lib/*" Analyze.java "The chefs were BAKING 3 chocolate-chip cookies!"
```

```
WhitespaceAnalyzer   [The] [chefs] [were] [BAKING] [3] [chocolate-chip] [cookies!]
StandardAnalyzer     [the] [chefs] [were] [baking] [3] [chocolate] [chip] [cookies]
EnglishAnalyzer      [chef] [were] [bake] [3] [chocol] [chip] [cooki]
```

**Discuss:**
- Which analyzer lets a search for `cookie` find "cookies!"?
- `chocol` and `cooki` are not real words. Does that matter? (The query goes through the same analyzer, so `chocolate` also becomes `chocol` and they match.)
- `the` disappeared but `were` did not. Lucene's English stopword list has only 33 words.

## Step 2: Build the index (1 min)

```bash
java -cp "lib/*" BuildIndex.java
```

A new `index/` folder appears. That folder **is** the search index.
Open `BuildIndex.java` and look at the field types:

| Field | Lucene type | What happens to it |
|---|---|---|
| `description`, `title` | `TextField` | Analyzed into words, searchable, ranked with BM25 |
| `cuisine`, `id` | `StringField` | Kept as one exact value, used for filtering |
| `minutes` | `IntPoint` | Numeric, for ranges such as 0-30 minutes |
| all of them | `Field.Store.YES` / `StoredField` | The original value is kept so it can be displayed |

## Step 3: Look inside the inverted index (2 min)

```bash
java -cp "lib/*" InspectIndex.java chocolate
java -cp "lib/*" InspectIndex.java garlic
```

```
Word "chocolate" -> analyzed term "chocol" in field "description"
N = 17 docs, avg field length (avgdl) = 16.8 tokens
df(chocol) = 2   total occurrences = 7

  Chocolate Lava Cake          tf=1  length= 15  positions=[2]
  Triple Chocolate Brownies    tf=6  length= 14  positions=[0, 1, 4, 7, 11, 16]
```

This is a **postings list**: for one word, which documents contain it (df),
how many times (tf), how long each document is, and where the word appears.
Lucene ranks results using exactly these numbers.

## Step 4: Search and ask Lucene why (4 min)

**Predict first:** for the search `chocolate cake`, which recipe wins: the
Brownies (which say "chocolate" 6 times) or the Lava Cake (which says it once)?

```bash
java -cp "lib/*" Search.java "chocolate cake"
```

```
Parsed query : description:chocol description:cake   <- after analysis
1. [2.1180] Chocolate Lava Cake (french, 30 min)
2. [1.6797] Triple Chocolate Brownies (american, 45 min)
```

Now ask Lucene to explain the scores:

```bash
java -cp "lib/*" Search.java --explain "chocolate cake"
```

Each matching word adds **idf × tf** to the score:

- **idf** is how rare the word is: `ln(1 + (N - n + 0.5) / (n + 0.5))`. Rare words count more.
- **tf** is how often the word appears, with diminishing returns: `freq / (freq + k1 × (1 - b + b × dl / avgdl))`.

**✍️ Check one by hand.** For the word `cake` in the Lava Cake: N = 17, n = 1, freq = 1, dl = 15, avgdl = 16.76, k1 = 1.2, b = 0.75.
- idf = ln(1 + 16.5 / 1.5) = ?
- tf = 1 / (1 + 1.2 × (0.25 + 0.75 × 15 / 16.76)) = ?
- idf × tf = ? Does it match Lucene's 1.1803?

**Why the Lava Cake wins:** it matches *both* words, and `cake` is the rarest
word in the query. The Brownies say "chocolate" 6 times but get only about 1.8
times the credit of a single mention (tf 0.85 vs 0.475). This is BM25's
**saturation**: repeating a word helps less and less.

## Step 5: Experiments (4 min)

Run each one, then compare it with what you saw before.

| # | Command | What to look for |
|---|---|---|
| a | `java -cp "lib/*" Search.java --classic "chocolate cake"` | Old-style TF-IDF instead of BM25. Who wins now? |
| b | `java -cp "lib/*" Search.java --k1 100 "chocolate cake"` | Turn saturation almost off. Who wins now? |
| c | `java -cp "lib/*" Search.java chicken` then `"spicy chicken"` | Which word moved the ranking more: the common one or the rare one? |
| d | `java -cp "lib/*" Search.java garlic` then add `--b 0` | `b = 0` turns off the length penalty. Where does the long Sunday Lasagna go? |
| e | `java -cp "lib/*" Search.java "olive oil"` then `"\"olive oil\""` | A phrase query: the words must be next to each other |

More query syntax to try. On Windows PowerShell, write a phrase as `'\"olive oil\"'`.

```bash
java -cp "lib/*" Search.java baking
java -cp "lib/*" Search.java "chiken~"
java -cp "lib/*" Search.java --minutes 0-30 chicken
java -cp "lib/*" Search.java "pasta -italian"
java -cp "lib/*" Search.java --cuisine mexican chicken
java -cp "lib/*" Search.java "title:cake"
java -cp "lib/*" Search.java "spicy^3 chicken"
```

**Puzzles:**
1. `baking` finds recipes that say "baked". Why?
2. `pasta -italian` should hide Italian dishes, but Spaghetti Carbonara still appears. Why? What works instead?
3. `title:cake` finds the Lava Cake but not the *Cheesecake* or the *Pancakes*. Why?
4. Did the `--minutes` filter change any **scores**, or only which recipes appear?

## 🏆 Challenge

Find the **quickest spicy dish**: a query and filters that return only spicy
recipes that take 30 minutes or less.

---

## Lucene, Elasticsearch and OpenSearch

| What you did today in Lucene (Java) | The same thing in Elasticsearch / OpenSearch (REST + JSON) |
|---|---|
| An index folder on disk | An index split into **shards**. Each shard is a Lucene index |
| `TextField` / `StringField` / `IntPoint` | Mapping types `text` / `keyword` / `integer` |
| `EnglishAnalyzer` | `"analyzer": "english"` |
| `QueryParser` syntax | `query_string` query |
| `BooleanQuery` with MUST / FILTER | `bool` query with `must` / `filter` |
| `BM25Similarity(k1, b)` | `index.similarity` settings (BM25 by default) |
| `searcher.explain(...)` | `_explain` API |

Lucene is the **engine**. Elasticsearch and OpenSearch are **servers built
around it**: they add an HTTP/JSON API, spread data across many machines,
replicate it, and provide dashboards. OpenSearch is the Apache 2.0 fork of
Elasticsearch 7.10 that AWS created in 2021.
