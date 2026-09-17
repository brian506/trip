---
name: git
description: 커밋, 이슈, JOURNAL 기록 규칙과 절차. 공개 저장소 금지어 규칙 포함. Trigger on '커밋해줘', '이슈 만들어줘', '기록해줘', 'JOURNAL 써줘'.
allowed-tools: AskUserQuestion Bash(git *) Bash(gh issue *) Bash(gh label *) Bash(sh scripts/*) Read Edit Write
argument-hint: "[commit | issue | journal] [설명]"
---

# Git · Issue · Journal

## 공개 저장소 규칙

저장소, 이슈, PR, 댓글이 모두 public이다.

- 회사명이나 작성 목적을 짐작하게 하는 단어를 쓰지 않는다. 목록은 `.githooks/forbidden-words.local`이며 커밋하지 않는다.
- 규칙이나 문서에 금지어를 예시로도 적지 않는다.
- 기획 문서와 공급사 API 스펙 원문을 넣지 않는다. 요약만 한다. Mock 응답 데이터는 구현물이라 허용한다.
- 금지어 판정은 `scripts/check-forbidden-words.sh` 하나로 한다.

| 경로 | 검사 |
|------|------|
| 커밋 | `pre-commit`: 원본 문서, 경로·추가 줄 금지어 / `commit-msg`: 형식, 자동 종료 키워드, 금지어 |
| PR | CI: 단위·통합 테스트. 통과해야 머지할 수 있고, 문서만 바뀐 PR은 테스트를 건너뛴다 |
| Claude의 `gh issue·pr create·edit·comment` | `.claude/hooks/check-gh-text.sh`가 실행 전 차단 |
| 웹에서 쓴 이슈·PR·댓글 | `sh scripts/audit-github-text.sh`로 사후 점검. 제출 전 필수 |

## commit

- 제목: `<type>[#<이슈번호>]: <한글 설명>`. type은 `feature`, `fix`, `refactor`, `test`, `docs`, `chore`.
- 작업 환경 파일(`.claude/`, `CLAUDE.md`, `docs/JOURNAL.md`, `.githooks/`, `scripts/`, `.github/ISSUE_TEMPLATE/`)만 바꾸면 이슈·PR 없이 `main`에서 `<type>: <한글 설명>`으로 커밋한다.
- 커밋 단위 기준은 미결정이다. 애매하면 묻는다.
- 커밋은 사용자가 요청할 때만 한다. `--no-verify`, `git add -f`는 쓰지 않는다.
- 커밋 메시지에 `closes #N`, `fixes #N`, `resolves #N`을 쓰지 않는다.
- JOURNAL·설계 기록은 별도 docs 이슈나 커밋으로 분리하지 않는다. 해당 구현·테스트 커밋에 함께 넣는다.

## branch · PR

- 작업 환경 파일 외의 변경은 이슈별 브랜치 `<type>/<이슈번호>`에서 작업한다. 예: `feature/5`
- 브랜치는 최신 `main`에서 만든다. 머지된 브랜치는 로컬·원격 모두 지우고, 같은 이슈 작업을 다시 하면 최신 `main`에서 같은 이름으로 새로 만든다.
- PR 제목은 `<type>[#<이슈번호>]: <한글 설명>`, 본문은 변경 요약과 `관련 이슈: #N`. 자동 종료 키워드는 쓰지 않는다.
- 머지는 Merge commit(`gh pr merge --merge`)으로 하고, 사용자가 요청할 때만 한다. CI `test`가 통과한 PR만 머지하고 `--admin`으로 우회하지 않는다.

## issue

- 템플릿: `.github/ISSUE_TEMPLATE/` (Feature, Bug, Refactor, Chore). 제목은 `[Feature]: 설명`.
- 본문: 목표, 작업 목록, 결정 요약 한 줄씩, JOURNAL 링크. 근거와 과정은 쓰지 않는다.
- 이슈는 푸시·머지 후에도 열어 둔다. 닫기는 사용자 요청 시에만, 삭제는 하지 않는다.
- 라벨은 저장소 생성 후 한 번 만든다: `gh label create feature`, `refactor`, `chore` (`bug`는 기본).

## journal

- `JOURNAL.md`가 결정 기록의 원본이다. 이슈에만 쓰고 끝내지 않는다.
- 기록 대상은 기술 선택과 구조 결정이다. 그 외 구현 세부는 적지 않는다.
- 구조와 섹션 템플릿은 `JOURNAL.md` 상단 주석에 있다. `## Day N (날짜)` 안에 `### #N 제목`.
- 의사결정은 상황, 채택과 근거, 다른 방법과의 비교, 판단 이유 순으로 쓴다. AI 활용은 질문, 수용·수정·거부, 이유를 쓴다.
- 이슈 작업을 마치면 JOURNAL 섹션을 먼저 쓰고, 이슈의 결정 요약과 링크를 채운다.
- JOURNAL 내용은 사용자 판단이 드러나야 한다. 초안을 쓰면 사용자 확인을 받는다.

## 새로 clone했을 때

```bash
git config core.hooksPath .githooks
# .githooks/forbidden-words.local 생성 (한 줄에 금지어 하나)
```
