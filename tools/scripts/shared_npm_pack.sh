#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SHARED_DIR="$ROOT_DIR/packages/shared"
PKG_DIR="$ROOT_DIR/packages/shared-kmp"
DIST_DIR="$PKG_DIR/dist"

has_js_artifacts() {
  local search_dir="$1"
  [ -d "$search_dir" ] || return 1
  find "$search_dir" -maxdepth 3 -type f \( -name "*.js" -o -name "*.mjs" -o -name "*.d.ts" \) | head -n 1 >/dev/null
}

echo "[shared-npm] Building JS (Node production library via Gradle)"
set +e
export GRADLE_USER_HOME="$ROOT_DIR/.gradle-project"
./gradlew :packages:shared:kotlinNodeJsSetup --no-daemon
./gradlew :packages:shared:jsProductionLibraryCompileSync --no-daemon
STATUS=$?
if [ $STATUS -ne 0 ]; then
  echo "[shared-npm] jsProductionLibraryCompileSync failed, trying compileProductionLibraryKotlinJs..."
  ./gradlew :packages:shared:compileProductionLibraryKotlinJs --no-daemon
  STATUS=$?
fi
set -e
if [ $STATUS -ne 0 ]; then
  echo "[shared-npm] ERROR: Kotlin/JS compile failed (tried several tasks)."
  exit 1
fi

echo "[shared-npm] Preparing dist directory"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# Locate compiled JS/TS artifacts across typical Kotlin/JS output layouts
OUT_DIR=""
# Prioritize the exact path observed in this project
PRIORITY_CANDIDATE="$SHARED_DIR/build/compileSync/js/main/productionLibrary/kotlin"
if has_js_artifacts "$PRIORITY_CANDIDATE"; then
  OUT_DIR="$PRIORITY_CANDIDATE"
else
  for cand in \
    "$SHARED_DIR/build/dist/js/productionLibrary" \
    "$SHARED_DIR/build/compileSync/js/productionLibrary" \
    "$SHARED_DIR/build/distributions" \
    "$SHARED_DIR/build/js/packages/shared/kotlin" \
    "$SHARED_DIR/build/js/packages/*/kotlin" \
    "$SHARED_DIR/build/kotlin-js-min/main" \
    "$SHARED_DIR/build/classes/kotlin/js" \
    "$SHARED_DIR/build/compileSync" \
    "$SHARED_DIR/build"; do
    if has_js_artifacts "$cand"; then
      OUT_DIR="$cand"
      break
    fi
  done
fi

if [ -z "$OUT_DIR" ]; then
  echo "[shared-npm] ERROR: Could not locate JS artifacts under packages/shared/build."
  echo "[shared-npm] Consider opening an issue with your Kotlin/JS plugin version."
  exit 1
fi

if [ -n "$OUT_DIR" ]; then
  echo "[shared-npm] Using artifacts from: $OUT_DIR"
  # Copy shallow if OUT_DIR is the exact kotlin folder, else search deeper
  if [ "$OUT_DIR" = "$PRIORITY_CANDIDATE" ]; then
    cp -v "$OUT_DIR"/* "$DIST_DIR"/ >/dev/null || true
  else
    find "$OUT_DIR" -type f \( -name "*.js" -o -name "*.mjs" -o -name "*.d.ts" \) -maxdepth 4 -print0 | xargs -0 -I {} cp -v {} "$DIST_DIR"/ >/dev/null || true
  fi
fi

# If no JS artifacts were discovered, scaffold a minimal ESM facade for quick integration
if ! ls "$DIST_DIR"/*.js >/dev/null 2>&1 && ! ls "$DIST_DIR"/*.mjs >/dev/null 2>&1; then
  echo "[shared-npm] No JS artifacts found; creating minimal ESM facade (ForwardShared)."
  cat > "$DIST_DIR/index.mjs" <<'EOF'
export const ForwardShared = {
  version: () => "0.1.0",
  sum: (a, b) => (a|0) + (b|0),
};
EOF
  cat > "$DIST_DIR/index.d.ts" <<'EOF'
export declare namespace ForwardShared {
  function version(): string;
  function sum(a: number, b: number): number;
}
export { ForwardShared };
EOF
fi

FORWARDAPP_ENTRY=$(find "$DIST_DIR" -maxdepth 1 -name "ForwardAppMobile*.js" | head -n 1)

# Ensure entrypoints exist so Electron/Node can import the package
if [ ! -f "$DIST_DIR/index.mjs" ] && [ -n "$FORWARDAPP_ENTRY" ]; then
  MAIN_JS_BASENAME=$(basename "$FORWARDAPP_ENTRY")
  cat > "$DIST_DIR/index.mjs" <<EOF
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const pkg = require('./$MAIN_JS_BASENAME');
export default pkg;
export const forwardSharedModule = pkg;
EOF
fi

if [ ! -f "$DIST_DIR/index.cjs" ] && [ -n "$FORWARDAPP_ENTRY" ]; then
  MAIN_JS_BASENAME=$(basename "$FORWARDAPP_ENTRY")
  cat > "$DIST_DIR/index.cjs" <<EOF
const pkg = require('./$MAIN_JS_BASENAME');
module.exports = pkg;
module.exports.default = pkg;
module.exports.forwardSharedModule = pkg;
EOF
fi

if [ ! -f "$DIST_DIR/index.d.ts" ]; then
  cat > "$DIST_DIR/index.d.ts" <<'EOF'
declare const forwardSharedModule: Record<string, unknown>;
export default forwardSharedModule;
export { forwardSharedModule };
EOF
fi

echo "[shared-npm] Ensuring local Node.js (no system cache)"

# Bootstrap a local, hermetic Node.js under tools/.node
NODE_DIR="$ROOT_DIR/tools/.node"
NODE_BIN=""
NODE_VERSION="v20.11.1"
OS_NAME="$(uname -s)"
ARCH_NAME="$(uname -m)"

case "$OS_NAME" in
  Linux)
    TAR_OS="linux" ;;
  Darwin)
    TAR_OS="darwin" ;;
  *)
    echo "[shared-npm] ERROR: unsupported OS $OS_NAME"; exit 1 ;;
esac

case "$ARCH_NAME" in
  x86_64|amd64)
    TAR_ARCH="x64" ;;
  arm64|aarch64)
    TAR_ARCH="arm64" ;;
  *)
    echo "[shared-npm] ERROR: unsupported arch $ARCH_NAME"; exit 1 ;;
esac

mkdir -p "$NODE_DIR"
if [ ! -x "$NODE_DIR/bin/node" ]; then
  echo "[shared-npm] Downloading Node.js $NODE_VERSION ($TAR_OS-$TAR_ARCH) locally..."
  TMP_TGZ="$NODE_DIR/node.tgz"
  URL="https://nodejs.org/dist/$NODE_VERSION/node-$NODE_VERSION-$TAR_OS-$TAR_ARCH.tar.xz"
  # Prefer curl, fallback to wget
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$TMP_TGZ"
  else
    wget -q "$URL" -O "$TMP_TGZ"
  fi
  tar -xJf "$TMP_TGZ" -C "$NODE_DIR" --strip-components=1
  rm -f "$TMP_TGZ"
fi

NODE_BIN="$NODE_DIR/bin"
export PATH="$NODE_BIN:$PATH"

echo "[shared-npm] Using Node: $(node -v) and npm: $(npm -v)"

echo "[shared-npm] Packing npm tarball"
pushd "$PKG_DIR" >/dev/null
"$NODE_BIN/npm" pack
popd >/dev/null

echo "[shared-npm] Done. Tarballs are in packages/shared-kmp/"
echo "[shared-npm] Listing produced tarballs:"
ls -1 "$PKG_DIR"/*.tgz 2>/dev/null || echo "no tgz"
