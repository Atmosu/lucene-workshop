#!/usr/bin/env bash
# Downloads the Apache Lucene jars this workshop needs into ./lib
# Usage:  ./setup.sh            (Lucene 10, needs Java 21+)
#         LUCENE_VERSION=9.12.3 ./setup.sh   (if you only have Java 17-20)
set -e
V="${LUCENE_VERSION:-10.5.2}"
BASE="https://repo1.maven.org/maven2/org/apache/lucene"
mkdir -p lib
for m in lucene-core lucene-analysis-common lucene-queryparser lucene-queries lucene-sandbox; do
  echo "Downloading $m-$V.jar"
  curl -fsSL -o "lib/$m-$V.jar" "$BASE/$m/$V/$m-$V.jar"
done
echo "Done. Jars in ./lib:"; ls lib
java -version
