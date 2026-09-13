#!/bin/sh
# Scans issue titles, bodies, comments and PR titles, bodies, comments on GitHub for forbidden words.
# Text written on the web cannot be blocked before posting, so run this after posting and before submission.
# exit 0: clean / 1: forbidden word found / 2: gh failure or word list problem

ROOT=$(git rev-parse --show-toplevel)
CHECK="$ROOT/scripts/check-forbidden-words.sh"
TEXT=$(mktemp)
trap 'rm -f "$TEXT"' EXIT

JQ_FILTER='.[] | .number as $n
    | ("#\($n) title: \(.title)"),
      ((.body // "") | split("\n")[] | "#\($n) body: \(.)"),
      (.comments[]? | (.body // "") | split("\n")[] | "#\($n) comment: \(.)")'

gh issue list --state all --limit 1000 --json number,title,body,comments --jq "$JQ_FILTER" > "$TEXT" || exit 2
gh pr list --state all --limit 1000 --json number,title,body,comments --jq "$JQ_FILTER" >> "$TEXT" || exit 2

hit=$(sh "$CHECK" < "$TEXT")
case $? in
    0) echo "OK    이슈·PR 텍스트에 금지어가 없다"; exit 0 ;;
    1) echo "FAIL  이슈·PR 텍스트에 금지어가 있다. 웹에서 해당 항목을 수정한다"; printf '%s\n' "$hit" | sed 's/^/      /'; exit 1 ;;
    *) exit 2 ;;
esac
