#!/bin/sh
# Reads text from stdin and reports lines containing forbidden words.
# exit 0: clean / 1: forbidden word found / 2: word list missing or empty

ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
WORDS_FILE="$ROOT/.githooks/forbidden-words.local"

if [ ! -f "$WORDS_FILE" ]; then
    echo "FAIL  금지어 목록 파일이 없다: .githooks/forbidden-words.local" >&2
    exit 2
fi

PATTERNS=$(mktemp)
trap 'rm -f "$PATTERNS"' EXIT
grep -v '^[[:space:]]*#' "$WORDS_FILE" | grep -v '^[[:space:]]*$' > "$PATTERNS"
if [ ! -s "$PATTERNS" ]; then
    echo "FAIL  금지어 목록이 비어 있다: .githooks/forbidden-words.local" >&2
    exit 2
fi

hit=$(grep -i -F -f "$PATTERNS")
if [ -n "$hit" ]; then
    printf '%s\n' "$hit"
    exit 1
fi
exit 0
