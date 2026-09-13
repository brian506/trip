#!/bin/sh
# Lists files changed by commits that are not on any remote yet. Deleted files are excluded.
# Usage: sh scripts/unpushed-changes.sh [main|test|all]
#   main: implementation code and build settings / test: test code / all: every file

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT" || exit 1
SCOPE="${1:-all}"

REVS=$(git rev-list HEAD --not --remotes 2>/dev/null)
[ -z "$REVS" ] && exit 0

FILES=$(printf '%s\n' "$REVS" \
    | while read -r rev; do git show --name-only --diff-filter=d --format= "$rev"; done \
    | grep -v '^$' | sort -u)

case "$SCOPE" in
    main) printf '%s\n' "$FILES" | grep -E '^(src/main/|build\.gradle$|settings\.gradle$|lombok\.config$)' ;;
    test) printf '%s\n' "$FILES" | grep -E '^src/test/' ;;
    all)  printf '%s\n' "$FILES" ;;
    *)    echo "Usage: sh scripts/unpushed-changes.sh [main|test|all]" >&2; exit 2 ;;
esac
exit 0
