#!/usr/bin/env bash
# Mooctest delivery packager — run AFTER a green `mvn test` + PIT run.
# Usage: package-delivery.sh <project-dir> <test-file-basename>
# Produces: <project-dir>_deliverables/<ts>/StudentTest.java + <name>.zip + delivery-record.md
set -euo pipefail

PROJ="$1"
NAME="$(basename "$PROJ")"
TS="$(date +%Y-%m-%d)"
OUT="$(cd "$(dirname "$PROJ")" && pwd)/$(basename "$PROJ")-deliverables/$TS"
TESTFILE="src/test/java/net/mooctest/StudentTest.java"

mkdir -p "$OUT"

# 1. the standalone test file (byte-identical to the one inside the project)
cp "$PROJ/$TESTFILE" "$OUT/StudentTest.java"

# 2. verify source baseline before zipping
BASELINE="$(cd "$(dirname "$PROJ")" && pwd)/_baseline/$NAME.sha256"
cd "$PROJ"
find pom.xml src/main src/test -type f -print0 | sort -z | xargs -0 sha256sum | grep -v "src/test/java/net/mooctest/StudentTest.java" > /tmp/baseline-now.sha256
grep -v "src/test/java/net/mooctest/StudentTest.java" "$BASELINE" > /tmp/baseline-official.sha256
if ! diff -q /tmp/baseline-now.sha256 /tmp/baseline-official.sha256 >/dev/null 2>&1; then
  echo "!! baseline mismatch vs official package — check src/main integrity before shipping"
  diff /tmp/baseline-now.sha256 /tmp/baseline-official.sha256 | head -20
  exit 1
fi

# 3. verify required reports exist
test -f target/pit-reports/index.html || { echo "!! missing target/pit-reports/index.html"; exit 1; }
test -f target/site/jacoco/index.html || { echo "!! missing target/site/jacoco/index.html"; exit 1; }

# 4. zip flat (pom.xml at zip root, no wrapper dir)
STAGE="$(mktemp -d)"
rm -rf "$STAGE/$NAME"; mkdir -p "$STAGE/$NAME"
cp pom.xml "开发者测试比赛要求.md" "$STAGE/$NAME/" 2>/dev/null || cp pom.xml "$STAGE/$NAME/"
cp -r src "$STAGE/$NAME/src"
cp -r target "$STAGE/$NAME/target"
rm -rf "$STAGE/$NAME/target/cp.txt" "$STAGE/$NAME/target/surefire-reports" "$STAGE/$NAME/target/jacoco.exec" \
       "$STAGE/$NAME/target/classes" "$STAGE/$NAME/target/test-classes" "$STAGE/$NAME/target/generated-sources" \
       "$STAGE/$NAME/target/generated-test-sources" "$STAGE/$NAME/target/maven-status" "$STAGE/$NAME/target/maven-archiver"
cd "$STAGE/$NAME" && zip -q -r -X "$OUT/$NAME.zip" pom.xml "开发者测试比赛要求.md" src target 2>/dev/null || \
  (cd "$STAGE/$NAME" && zip -q -r -X "$OUT/$NAME.zip" pom.xml src target)
cd - >/dev/null
rm -rf "$STAGE"

# 5. record hashes + sizes
{
  echo "# Delivery record — $NAME — $TS"
  echo
  echo "| 文件 | 字节 | SHA-256 |"
  echo "|---|---|---|"
  for f in "$OUT/StudentTest.java" "$OUT/$NAME.zip"; do
    sz=$(stat -c %s "$f"); h=$(sha256sum "$f" | cut -d' ' -f1)
    echo "| $(basename "$f") | $sz | $h |"
  done
  echo
  echo "ZIP 检查: 第一层含 pom.xml / 要求md / src / target(pit-reports + site/jacoco 完整报告)"
  echo "基线: src/main 与 pom.xml 哈希与官方题包一致(脚本已校验)"
} > "$OUT/delivery-record.md"

echo "OK -> $OUT"
ls -la "$OUT"
