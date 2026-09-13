---
name: feature
description: 기능 구현 절차 (이슈 확인 → 규칙 로드 → 설계 확인 → 구현 → 빌드). 테스트는 쓰지 않는다 — /test-write가 맡는다. Trigger on '~구현해줘', '~API 만들어줘', '~기능 추가해줘'.
allowed-tools: AskUserQuestion Bash(./gradlew *) Bash(git *) Bash(gh issue list *) Bash(gh issue view *) Bash(sh scripts/*) Read Glob Grep Edit Write
argument-hint: "[구현할 기능 설명]"
---

# Feature

> `src/test/`는 건드리지 않는다.

## Step 0: 이슈 확인

```bash
gh issue list --state open --limit 50
```

관련 이슈가 없으면 `/git` 스킬의 이슈 생성 절차로 만든다. 이슈 번호 N을 이후 커밋에 쓴다.

## Step 1: 규칙 로드

순서대로 읽는다.

1. `.claude/references/decisions.md` — 확정·미결정 사항
2. `references/forbidden.md` — 코드 금지 규칙
3. `references/architecture.md` — 계층, 패키지, Manager·Client, 공급사 경계
4. `references/patterns.md` — VO 검증, Request, Command, 응답, Swagger
5. `references/error-codes.md` — ErrorType·ErrorCode 추가 절차

## Step 2: 설계 확인 (AskUserQuestion)

이미 대화에서 합의된 설계면 생략한다. 아니면 아래를 정리해 승인을 받는다.

- 도메인 패키지
- API: method, path, request, response 초안
- VO와 검증할 제약
- Manager·Client 구성, 엔티티
- 새 ErrorType

설계에 미결정 항목이 걸리면 선택지와 트레이드오프를 제시하고 사용자가 고르게 한다. 결정되면 `decisions.md`를 갱신한다.

## Step 3: 구현

- 기획 범위가 정해진 소규모 프로젝트다. 확장 대비 추상화를 만들지 않는다.
- 새로 도입하는 패턴이나 API는 사용자에게 설명한다. 사용자가 설명할 수 없는 코드를 남기지 않는다.

## Step 4: 빌드

```bash
./gradlew build
```

실패하면 원인을 고친다. 반복 실패하면 사용자에게 보고하고 멈춘다.

## Step 5: 마무리

- 테스트가 필요하면 `/test-write`를 안내한다.
- 커밋과 JOURNAL 기록은 `/git` 절차를 따른다.
