---
name: push
description: 푸시 전 검증 후 푸시 (리뷰 게이트 → 판정 처리 → git push). 구현 코드는 review, 테스트 코드는 test-review 에이전트로 나눠 검증한다. Trigger on '푸시해줘', '검증하고 올려줘', 'push'.
allowed-tools: Agent Bash(sh scripts/*) Bash(git *) Read
---

# Push

리뷰 게이트는 커밋된 변경을 본다. 커밋이 끝난 뒤, 푸시 전에 실행한다.

## Step 0: 상태 확인

```bash
git status --short
git log --oneline HEAD --not --remotes
```

커밋하지 않은 변경이 있으면 사용자에게 알리고 멈춘다. 푸시할 커밋이 없으면 끝낸다.

## Step 1: 변경 분류

```bash
sh scripts/unpushed-changes.sh main   # 구현 코드와 빌드 설정
sh scripts/unpushed-changes.sh test   # 테스트 코드
```

## Step 2: 리뷰 게이트

결과가 있는 섹션만 실행한다. 둘 다 있으면 한 번에 병렬로 spawn한다. prompt에 해당 파일 목록과 커밋 제목의 이슈 번호를 넘긴다.

| 섹션 | 에이전트 | 차단 기준 |
|------|----------|----------|
| 구현 | `review` | 🔴 Critical 1건 이상 |
| 테스트 | `test-review` | `판정: 수정 필요` |

## Step 3: 판정 처리

- 근거 한 줄이 없는 지적은 채택하지 않는다.
- `test-review`의 누락 상황 제안은 바로 구현하지 않는다. 사용자에게 보여주고, 고른 것만 `/test-write`로 추가한다.
- 차단 기준에 걸리면 푸시하지 않는다. 수정 → 커밋 → Step 1부터 다시 한다.
- 차단 기준 미만은 사용자에게 보고하고 진행한다.
- 수용하지 않는 지적은 근거를 `JOURNAL.md` AI 활용 항목에 적는다.
- 같은 섹션이 3회 연속 차단되면 사용자에게 보고하고 멈춘다.

## Step 4: 푸시

- `main`: 작업 환경 파일 커밋만 푸시한다. 그 외 변경이 있으면 멈추고 브랜치로 옮길지 묻는다.
- `<type>/<이슈번호>` 브랜치: 푸시 후 PR이 없으면 `/git`의 branch · PR 규칙으로 만든다.

```bash
git push -u origin <현재 브랜치>
gh pr create --base main --title "<type>[#N]: <설명>" --body "<변경 요약>\n\n관련 이슈: #N"
```

`pre-push` 훅이 금지어 검사와 `./gradlew clean build`를 실행한다. 실패하면 원인을 고치고 다시 커밋한다.
`--no-verify`, `--force`는 쓰지 않는다.

## Step 5: 보고

- 푸시한 커밋, 리뷰 결과, PR 링크를 요약한다. 머지는 하지 않는다.
- 이슈는 닫지 않는다. 같은 이슈로 리팩토링·핫픽스가 이어질 수 있다.
