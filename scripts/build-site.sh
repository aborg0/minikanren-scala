#!/bin/bash
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$REPO_ROOT/site-dist"
WEBSITE_SOURCE="$REPO_ROOT/website"
WEBSITE_TARGET_DIR="$REPO_ROOT/website-demo/target/scala-3.8.3"

if [ -d "$DIST_DIR" ]; then
  rm -rf "$DIST_DIR"
fi

mkdir -p "$DIST_DIR"

cd "$REPO_ROOT"

export SBT_OPTS="-Dsbt.server.autostart=false"
sbt -batch "++3.8.3" "miniKanrenExamplesJVM/mdoc" "miniKanrenScala3DSLCrossJVM/mdoc" "miniKanrenLangCrossJVM/mdoc" "miniKanrenWebsite/fullLinkJS"

cp -r "$WEBSITE_SOURCE"/* "$DIST_DIR/"

SITE_APP=$(find "$WEBSITE_TARGET_DIR" -name 'main.js' -path '*/opt/*' -print -quit)
if [ -z "$SITE_APP" ]; then
  echo "Error: Could not locate the linked website Scala.js output." >&2
  exit 1
fi

mkdir -p "$DIST_DIR/assets"
cp "$SITE_APP" "$DIST_DIR/assets/site-app.js"

declare -a docs_dirs=(
  "examples:jvm"
  "scala3dsl"
  "lang"
)

for doc_spec in "${docs_dirs[@]}"; do
  IFS=':' read -r doc_name doc_suffix <<< "$doc_spec"
  if [ -z "$doc_suffix" ]; then
    doc_suffix="$doc_name"
  fi
  
  SOURCE_DIR="$REPO_ROOT/$doc_name/target/mdoc"
  TARGET_DIR="$DIST_DIR/docs/$doc_suffix"
  
  if [ ! -d "$SOURCE_DIR" ]; then
    echo "Error: Missing generated docs directory: $SOURCE_DIR" >&2
    exit 1
  fi
  
  mkdir -p "$TARGET_DIR"
  cp -r "$SOURCE_DIR"/* "$TARGET_DIR/"
done

echo "Built site artifact at $DIST_DIR"
