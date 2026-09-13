#!/bin/sh
# Claude Code PreToolUse(Bash) hook.
# Blocks `gh issue|pr create|edit|comment` when the title, body, or body file contains forbidden words
# or an issue closing keyword (close/fix/resolve #N).
# exit 0: allow / exit 2: block (stderr is shown to Claude)

INPUT=$(cat)

TEXT=$(printf '%s' "$INPUT" | python3 -c '
import json, os, re, shlex, sys

data = json.load(sys.stdin)
command = data.get("tool_input", {}).get("command", "")
if not re.search(r"\bgh\s+(issue|pr)\s+(create|edit|comment)\b", command):
    sys.exit(3)

print(command)

cwd = data.get("cwd") or os.getcwd()
try:
    tokens = shlex.split(command)
except ValueError:
    tokens = []

for i, token in enumerate(tokens):
    path = None
    if token in ("--body-file", "-F") and i + 1 < len(tokens):
        path = tokens[i + 1]
    elif token.startswith("--body-file="):
        path = token.split("=", 1)[1]
    if path and path != "-":
        full = path if os.path.isabs(path) else os.path.join(cwd, path)
        try:
            with open(full, encoding="utf-8") as f:
                print(f.read())
        except OSError:
            pass
')
case $? in
    3) exit 0 ;;
    0) ;;
    *) echo "gh 텍스트 검사 훅: 입력을 해석하지 못했다" >&2; exit 2 ;;
esac

closing=$(printf '%s\n' "$TEXT" | grep -inE '(^|[^[:alnum:]])(close[sd]?|fix(e[sd])?|resolve[sd]?):?[[:space:]]+([[:alnum:]_.-]+/[[:alnum:]_.-]+)?#[0-9]+')
if [ -n "$closing" ]; then
    echo "차단: 이슈 자동 종료 키워드(close/fix/resolve #N)를 쓰지 않는다. 이슈는 사용자가 직접 닫는다." >&2
    printf '%s\n' "$closing" | sed 's/^/  /' >&2
    exit 2
fi

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
hit=$(printf '%s\n' "$TEXT" | sh "$ROOT/scripts/check-forbidden-words.sh" 2>&1)
case $? in
    0) exit 0 ;;
    1)
        echo "차단: 이슈·PR 텍스트에 금지어가 있다. 표현을 바꿔 다시 작성한다." >&2
        printf '%s\n' "$hit" | sed 's/^/  /' >&2
        exit 2
        ;;
    *)
        echo "차단: 금지어 검사를 실행하지 못했다." >&2
        printf '%s\n' "$hit" >&2
        exit 2
        ;;
esac
