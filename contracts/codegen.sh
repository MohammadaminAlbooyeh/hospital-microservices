#!/usr/bin/env bash
#
# codegen.sh - regenerate cross-language code from the contracts in this
# directory.
#
#   contracts/proto/*.proto  -> Java protobuf messages + gRPC stubs
#                               (services-java/shared-kernel/src/main/java)
#   contracts/avro/*.avsc    -> Java Avro records (via avro-tools when present)
#   contracts/avro/*.avsc    -> Pydantic v2 models
#                               (services-python/shared/hospital_shared/events/generated.py)
#
# The script is intentionally best-effort: a missing optional tool is reported
# and skipped with exit status 0, so `make contracts` works on a fresh machine
# before protoc/avro-tools/datamodel-codegen have been installed. A tool that
# is present but fails makes the script exit non-zero.
#
# Usage:
#   ./contracts/codegen.sh
#   AVRO_TOOLS_JAR=/path/to/avro-tools.jar ./contracts/codegen.sh
#
# Environment:
#   PROTOC_INCLUDE     Extra include directory for protoc (well-known types)
#   AVRO_TOOLS_JAR     Path to avro-tools-*.jar when no `avro-tools` wrapper
#                      is on PATH
#   PYTHON             Python interpreter used for pip-installed tools
#                      (default: python3)
#
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"

PROTO_DIR="$SCRIPT_DIR/proto"
AVRO_DIR="$SCRIPT_DIR/avro"
JAVA_OUT="$ROOT_DIR/services-java/shared-kernel/src/main/java"
PY_OUT="$ROOT_DIR/services-python/shared/hospital_shared/events/generated.py"
SHARED_KERNEL_POM="$ROOT_DIR/services-java/shared-kernel/pom.xml"

PYTHON="${PYTHON:-python3}"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/hospital-contracts.XXXXXX")"
FAILURES=0

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

say() {
  printf '==> %s\n' "$*"
}

skip() {
  printf '    %s\n' "$*"
}

fail() {
  printf '    ERROR: %s\n' "$*" >&2
  FAILURES=$((FAILURES + 1))
}

mkdir -p "$JAVA_OUT"
mkdir -p "$(dirname "$PY_OUT")"

# ---------------------------------------------------------------------------
# 1. Protobuf + gRPC Java stubs
# ---------------------------------------------------------------------------
say "Protobuf: $PROTO_DIR -> $JAVA_OUT"

if ! command -v protoc >/dev/null 2>&1; then
  skip "protoc not found; skipping Java protobuf/gRPC generation."
  skip "Install it with 'brew install protobuf' (macOS) or from"
  skip "https://github.com/protocolbuffers/protobuf/releases, then rerun"
  skip "'make contracts'."
else
  proto_includes=(-I "$PROTO_DIR")
  for include_dir in "${PROTOC_INCLUDE:-}" /usr/local/include /opt/homebrew/include /usr/include; do
    if [[ -n "$include_dir" && -d "$include_dir" ]]; then
      proto_includes+=(-I "$include_dir")
    fi
  done

  proto_files=("$PROTO_DIR"/*.proto)
  if [[ ! -e "${proto_files[0]}" ]]; then
    skip "no .proto files found in $PROTO_DIR."
  elif command -v protoc-gen-grpc-java >/dev/null 2>&1; then
    if protoc "${proto_includes[@]}" \
        --java_out="$JAVA_OUT" \
        --grpc-java_out="$JAVA_OUT" \
        "${proto_files[@]}"; then
      skip "generated protobuf messages and gRPC stubs."
    else
      fail "protoc failed while generating Java messages and gRPC stubs."
    fi
  else
    skip "protoc-gen-grpc-java not found; generating protobuf messages only."
    skip "Install the gRPC Java plugin from Maven Central (protoc-gen-grpc-java)"
    skip "and put it on PATH to regenerate service stubs."
    if protoc "${proto_includes[@]}" --java_out="$JAVA_OUT" "${proto_files[@]}"; then
      skip "generated protobuf messages (no service stubs)."
    else
      fail "protoc failed while generating Java messages."
    fi
  fi
fi

# ---------------------------------------------------------------------------
# 2. Java Avro-specific classes
# ---------------------------------------------------------------------------
say "Avro Java: $AVRO_DIR -> $JAVA_OUT"

avro_files=("$AVRO_DIR"/*.avsc)
if [[ ! -e "${avro_files[0]}" ]]; then
  skip "no .avsc files found in $AVRO_DIR."
elif command -v avro-tools >/dev/null 2>&1; then
  if avro-tools compile -string schema "${avro_files[@]}" "$JAVA_OUT"; then
    skip "generated Java Avro records with avro-tools."
  else
    fail "avro-tools failed while generating Java Avro records."
  fi
elif [[ -n "${AVRO_TOOLS_JAR:-}" && -f "${AVRO_TOOLS_JAR:-}" ]] && command -v java >/dev/null 2>&1; then
  if java -jar "$AVRO_TOOLS_JAR" compile -string schema "${avro_files[@]}" "$JAVA_OUT"; then
    skip "generated Java Avro records with AVRO_TOOLS_JAR."
  else
    fail "avro-tools jar failed while generating Java Avro records."
  fi
else
  skip "avro-tools not found; skipping Java Avro record generation."
  skip "Either install avro-tools (for example 'brew install avro-tools' or"
  skip "download avro-tools-1.11.3.jar and set AVRO_TOOLS_JAR=/path/to/jar),"
  skip "or add the avro-maven-plugin to services-java/shared-kernel/pom.xml"
  skip "and run:"
  skip "    mvn -f services-java/shared-kernel/pom.xml generate-sources"
  if [[ -f "$SHARED_KERNEL_POM" ]] && grep -q 'avro-maven-plugin' "$SHARED_KERNEL_POM" \
      && command -v mvn >/dev/null 2>&1; then
    skip "avro-maven-plugin detected; delegating to Maven."
    if mvn -q -f "$SHARED_KERNEL_POM" generate-sources; then
      skip "Maven generated Java Avro records."
    else
      fail "Maven avro:generate-sources failed."
    fi
  fi
fi

# ---------------------------------------------------------------------------
# 3. Pydantic models from Avro schemas
# ---------------------------------------------------------------------------
say "Pydantic: $AVRO_DIR -> $PY_OUT"

if ! command -v datamodel-codegen >/dev/null 2>&1; then
  skip "datamodel-codegen not found; skipping Pydantic model generation."
  skip "Install it with:"
  skip "    $PYTHON -m pip install 'datamodel-code-generator[http]'"
  skip "then rerun 'make contracts'."
elif [[ ! -e "${avro_files[0]}" ]]; then
  skip "no .avsc files found in $AVRO_DIR."
else
  generated_tmp="$TMP_DIR/generated.py"

  # Preferred path: let datamodel-codegen read the whole Avro directory so it
  # can resolve shared types and emit one module.
  if datamodel-codegen \
      --input "$AVRO_DIR" \
      --input-file-type avro \
      --output "$generated_tmp" \
      --output-model-type pydantic_v2.BaseModel \
      --target-python-version 3.12 \
      --use-standard-collections \
      --use-union-operator \
      --disable-timestamp \
      >/dev/null 2>&1 && [[ -s "$generated_tmp" ]]; then
    skip "generated $PY_OUT from the Avro directory."
  else
    # Fallback for older datamodel-codegen releases that only accept a single
    # Avro file. Generate each record separately, then merge the modules while
    # keeping a single __future__ import at the top.
    skip "directory input failed; generating per-file and merging."
    rm -f "$generated_tmp"
    per_file_ok=1
    for schema in "${avro_files[@]}"; do
      stem="$(basename "$schema" .avsc)"
      if ! datamodel-codegen \
          --input "$schema" \
          --input-file-type avro \
          --output "$TMP_DIR/$stem.py" \
          --output-model-type pydantic_v2.BaseModel \
          --target-python-version 3.12 \
          --use-standard-collections \
          --use-union-operator \
          --disable-timestamp \
          >/dev/null 2>&1; then
        per_file_ok=0
        fail "datamodel-codegen failed for $stem.avsc."
        break
      fi
    done

    if [[ "$per_file_ok" -eq 1 ]]; then
      {
        printf '%s\n' \
          '"""Generated from contracts/avro/*.avsc by contracts/codegen.sh.' \
          '' \
          'Do not edit by hand. Run `make contracts` after changing a schema.' \
          '"""'
        first=1
        for generated in "$TMP_DIR"/*.py; do
          [[ "$(basename "$generated")" == "generated.py" ]] && continue
          if [[ "$first" -eq 1 ]]; then
            cat "$generated"
            first=0
          else
            sed '/^from __future__ import annotations$/d' "$generated"
          fi
          printf '\n'
        done
      } > "$TMP_DIR/merged.py"
      generated_tmp="$TMP_DIR/merged.py"
      skip "merged per-file modules."
    fi
  fi

  if [[ -s "$generated_tmp" ]]; then
    mv "$generated_tmp" "$PY_OUT"
    skip "wrote $PY_OUT."
  elif [[ "$FAILURES" -eq 0 ]]; then
    fail "datamodel-codegen produced no output."
  fi
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
if [[ "$FAILURES" -gt 0 ]]; then
  printf '\nContract generation finished with %d failing step(s).\n' "$FAILURES" >&2
  exit 1
fi

printf '\nContract generation finished. Missing tools were skipped; see messages above.\n'
