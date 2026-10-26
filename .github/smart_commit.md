---
description: (Fixed) 테트리스 프로젝트 전용 Git Flow 변경사항 분석, 커밋, 푸시 후 PR 상태를 확인하여 생성하거나 최신화합니다. 다중 주제 자동 분할 및 AI 리뷰 코멘트 분리 등록 포함.
---

> **참고:** 브랜치명과 커밋/PR 제목은 아래 6가지 유형만 사용합니다.
>
> | 유형 | 용도 | 브랜치명 | 커밋/PR 제목 |
> | :--- | :--- | :--- | :--- |
> | `feat` | 새로운 기능 구현 | `feat/TET-12-<작업명>` | `[TET-12] feat: <작업 내용>` |
> | `fix` | 버그 수정 | `fix/TET-12-<작업명>` | `[TET-12] fix: <작업 내용>` |
> | `refactor` | 코드 구조 개선 (로직 불변) | `refactor/TET-12-<작업명>` | `[TET-12] refactor: <작업 내용>` |
> | `test` | 단위/통합 테스트 추가 및 코드 커버리지 확보 | `test/TET-12-<작업명>` | `[TET-12] test: <작업 내용>` |
> | `docs` | 문서 작성 및 수정 (README, 명세서 등) | `docs/TET-12-<작업명>` | `[TET-12] docs: <작업 내용>` |
> | `chore` | 빌드/환경 설정, 라이브러리 추가, `.gitignore` 등 | `chore/TET-12-<작업명>` | `[TET-12] chore: <작업 내용>` |
>
> 브랜치는 `<type>/<Jira-Key>-<kebab-case-작업명>` 형식으로 작성합니다. Jira Key는 `[A-Z0-9]+-[0-9]+` 형식을 사용하며, 커밋 메시지와 PR 제목은 반드시 `[Jira-Key] <type>: <설명>` 형식으로 통일합니다.
> **언어:** 모든 결과 보고 및 PR 본문/댓글은 **한글**로 작성합니다.
> **협업 기준:** 프로젝트, Git 및 Jira 운영 규칙은 `docs/project-conventions.md`를 따릅니다.

// turbo-all

---

## 0. GitHub CLI 환경 설정 및 인증 확인 (필수!)

```bash
# GH CLI 경로 설정 (필요시)
if ! command -v gh &> /dev/null; then
    if [ -f "/opt/homebrew/bin/gh" ]; then
        export PATH="/opt/homebrew/bin:$PATH"
    elif [ -f "/usr/local/bin/gh" ]; then
        export PATH="/usr/local/bin:$PATH"
    fi
fi

# GH CLI 설치 확인
if ! command -v gh &> /dev/null; then
    echo "❌ Error: 'gh' command not found. Please install GitHub CLI."
    exit 1
fi

# Auth Status 확인
if ! gh auth status &> /dev/null; then
    echo "❌ Error: GitHub CLI is not authenticated. Please run 'gh auth login'."
    exit 1
fi
```

---

## 0-1. 브랜치 전략 준수 확인 (Git Flow 변형, 필수!)

**⚠️ 직접 push 금지 브랜치**: `main`, `dev`

```bash
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

case "$CURRENT_BRANCH" in
  main|dev)
    echo "❌ $CURRENT_BRANCH 브랜치에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요."
    exit 1
    ;;
  *)
    if [[ ! "$CURRENT_BRANCH" =~ ^(feat|fix|refactor|test|docs|chore)/[A-Z0-9]+-[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
      echo "❌ 허용되지 않는 브랜치명입니다. <type>/<Jira-Key>-<kebab-case-작업명> 형식을 사용하세요."
      exit 1
    fi
    ;;
esac

BRANCH_TYPE="${CURRENT_BRANCH%%/*}"
JIRA_KEY=$(echo "$CURRENT_BRANCH" | grep -oE '[A-Z0-9]+-[0-9]+')
TARGET_BRANCH="dev"
```

| 현재 브랜치 | push 가능? | 조치 |
| :--- | :--- | :--- |
| `main` | ❌ 금지 | "main에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요." 안내 후 **중단** |
| `dev` | ❌ 금지 | "dev에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요." 안내 후 **중단** |
| `feat/TET-12-*`, `fix/TET-12-*`, `refactor/TET-12-*`, `test/TET-12-*`, `docs/TET-12-*`, `chore/TET-12-*` | ✅ 허용 (Target: `dev`) | 계속 진행 |
| 그 외 패턴 | ❌ 금지 | 변경사항에 맞는 허용 브랜치명으로 변경 또는 새 브랜치 생성 후 계속 진행 |

**main 또는 dev에 있는 경우 → 새 브랜치 생성 제안**:
```bash
# 새로운 기능 구현 시 (dev 기반)
git checkout -b feat/TET-12-<기능명> dev

# 버그 수정 시 (dev 기반)
git checkout -b fix/TET-12-<이슈설명> dev

# 테스트 추가 및 커버리지 확보 시 (dev 기반)
git checkout -b test/TET-12-<작업명> dev
```

---

## 0-2. Jira 이슈 존재 확인 (필수!)

브랜치명에서 Jira Key를 추출한 뒤 Atlassian Jira 연결을 사용하여 해당 이슈가 `TET` 프로젝트에 실제로 존재하는지 읽기 전용으로 확인합니다.

- 이슈가 존재하고 프로젝트 Key가 `TET`이면 계속 진행합니다.
- 이슈가 없거나 접근할 수 없으면 임의의 Key로 대체하지 말고 작업을 중단합니다.
- 이 단계에서는 Jira 이슈를 생성하거나 수정하지 않습니다.
- 새 이슈가 필요하면 `.github/jira_issue_template.md`로 기존 이슈 검색 및 승인 기반 생성 절차를 먼저 수행합니다.

---

## 0-3. 대응 GitHub Issue 확인 및 등록 (필수!)

Jira 이슈의 제목, 배경, 작업 범위, 완료 조건과 URL을 읽고, 같은 Jira Key를 가진 GitHub Issue가 있는지 열린 이슈와 닫힌 이슈 모두에서 먼저 검색합니다. 새 Issue 생성은 1-A단계에서 Jira와 실제 변경 범위가 일치하는지 확인한 뒤 진행합니다.

```bash
gh issue list --state all --search "$JIRA_KEY" --json number,title,body,state,url --limit 100
```

- 검색 결과의 제목과 본문에 적힌 Jira 링크를 직접 확인합니다. 같은 Jira 이슈에 대응하는 Issue가 있으면 번호와 URL을 재사용하며 중복 생성하지 않습니다. 닫힌 Issue는 임의로 다시 열지 않습니다.
- 기존 Issue의 내용이 템플릿 형식과 다르면 수정안을 제시하고 승인 후 갱신합니다. 후보가 여럿이면 연결할 번호를 확인받습니다.
- 대응 Issue가 없으면 `.github/github_issue_template.md`를 참고해 Jira의 목적과 작업 범위를 간결하게 요약하여 보여주고, 사용자 승인 후 생성합니다. 작업 규모에 따라 필요한 만큼 작성하며 줄 수를 고정하지 않습니다. Jira URL은 `Jira: [TET-12](https://kyun648.atlassian.net/browse/TET-12)` 형식으로 넣습니다. Jira의 상세 설명을 그대로 복사하거나 확인되지 않은 완료 기준을 만들지 않습니다.

템플릿의 HTML 안내 주석과 미작성 예시는 최종 Issue 본문에서 제거합니다. 담당자가 확인된 경우에만 GitHub assignee를 지정합니다. 제목은 `[<Jira-Key>] <Jira 이슈 제목>`으로 작성합니다. 웹에서 생성할 때는 작성한 요약과 Jira 링크를 이슈 입력란에 넣고, CLI에서는 실제 줄바꿈이 있는 `.github_issue_body_temp.md`에 작성하여 `gh issue create --title "[$JIRA_KEY] <Jira 이슈 제목>" --body-file .github_issue_body_temp.md`로 생성합니다. 담당 GitHub 계정을 확인했다면 CLI 명령에 `--assignee <GitHub 로그인>`도 추가합니다. 생성 후 번호와 URL을 확인하고 임시 파일을 삭제합니다. 승인이나 조회가 불가능하면 Issue를 추측해 만들지 말고 이 단계를 중단합니다.

---

## 1. 현재 상태 및 변경사항 분석 (주제별 분리 필수!)

```bash
git status -s
```

변경된 파일들의 도메인/주제(Scope)를 분석하여 **단일 주제**인지 **다중 주제**인지 판단합니다:
- 🎮 **게임 로직 / 블럭:** `tetris/src/main/java/**` (보드 판정, 7종 테트로미노, 충돌, 자동 낙하, 회전, 스코어/점수 계산, 설정 영속성)
- 🖥️ **UI / 메뉴 / 렌더러:** `tetris/src/main/java/**` (메인 메뉴, 게임 화면, 색맹 모드 렌더링, 키 설정 화면, 다이얼로그)
- ⚙️ **빌드 / 설정 / CI:** `build.gradle`, `settings.gradle`, `gradle/**`, `.github/**`
- 📝 **문서 및 산출물:** `README.md`, `docs/**`
- 🧪 **단위/통합 테스트:** `tetris/src/test/**`

### 🔀 분기 판단:
1. **단일 주제인 경우:**
   - 현재 브랜치에서 변경사항을 원자적으로 커밋하고 푸시하여 **단일 PR**을 생성합니다. (아래 **2단계** 진행)
2. **다중 주제(예: 게임 로직 버그 수정 + UI 메뉴 개편 + 문서 작업 등 성격이 다른 변경이 공존)인 경우:**
   - ⚠️ **단순히 커밋만 나누는 것으로는 부족하며, 반드시 주제별로 별도의 독립 브랜치를 생성하여 각각 독립된 PR로 분리해야 합니다!**
   - 아래 **"1-B. 다중 주제 브랜치 및 다중 PR 분할 워크플로우"**에 따라 작업을 분리하여 수행합니다.

---

## 1-A. Jira 이슈와 실제 작업 범위 정합성 확인 (필수!)

Jira 이슈의 제목, 배경, 작업 범위, 제외 범위 및 완료 조건을 현재 브랜치의 전체 변경사항과 비교합니다.

```bash
git fetch origin dev
git log origin/dev..$CURRENT_BRANCH --oneline
git diff --stat origin/dev...$CURRENT_BRANCH
git diff origin/dev...$CURRENT_BRANCH
```

> **역할 분리:** Jira에는 작업의 목적, 범위와 완료 조건을 기록하고, PR에는 실제 구현 내용, 변경 파일과 검증 결과를 기록합니다. Jira 제목을 `메뉴 작업 중`과 같은 상태 문구로 사용하지 않습니다. 진행 상태는 Jira 상태값으로 관리합니다.

| 비교 결과 | 처리 |
| :--- | :--- |
| Jira 범위와 실제 변경이 일치 | Jira를 변경하지 않고 계속 진행 |
| 동일한 목표 안에서 필요한 작은 범위가 추가됨 | Jira 작업 범위 또는 완료 조건 보완안을 제시하고 중단. 승인 후 `.github/jira_issue_template.md`의 기존 이슈 보완 절차를 수행한 다음 재개 |
| 원래 이슈와 독립적인 작업이 포함됨 | 기존 Jira 범위를 억지로 확장하지 않고 새 Jira 이슈와 별도 브랜치/PR로 분리 |
| 실제 변경이 Jira 이슈와 관련 없음 | 즉시 중단하고 올바른 기존 Jira Key를 선택하거나 새 이슈를 생성한 뒤 브랜치명을 변경 |

이 단계에서는 Jira 이슈를 직접 수정하지 않습니다. 단순 구현 세부사항, 변경 파일 목록 및 테스트 결과는 Jira에 중복 기록하지 않고 PR 본문에 작성합니다.

### 1-A-1. Jira 완료 조건 검증 (필수!)

Jira 설명의 `완료 조건`을 항목별로 읽고, 전체 diff와 실제 검증 결과를 근거로 다음과 같이 판정합니다. Jira의 완료 조건은 일반 목록으로 유지하고, 각 항목을 동일한 문구의 체크박스로 PR 본문에 복사합니다.

| 판정 | 처리 |
| :--- | :--- |
| 충족 | 확인에 사용한 파일, 명령 또는 원격 상태를 기록하고 PR에서 체크 |
| 미충족 | PR에서 체크하지 않고 필요한 후속 작업을 보고 |
| 확인 불가 | 추측하여 체크하지 않고 사용자 확인이 필요한 이유를 PR에 기록 |
| PR 생성·병합 등 이후 단계에서 충족 | 해당 단계가 실제 완료된 뒤 다시 확인 |

- Jira 완료 조건의 문구를 PR 체크리스트에서 임의로 바꾸거나 삭제하지 않습니다.
- 테스트를 실행하지 못했거나 실패했다면 관련 완료 조건을 체크하지 않습니다.
- `완료 조건`이 없거나 검증할 수 없는 표현뿐이면 구체적인 완료 조건 보완안을 제시하고 중단합니다.
- Jira 설명은 완료 여부 기록을 위해 수정하지 않습니다. 완료 조건의 충족 상태와 검증 근거는 PR에서 관리합니다.

---

## 1-B. 다중 주제 브랜치 및 다중 PR 분할 워크플로우 (Multi-Topic Branch & PR Split)

성격이 다른 변경사항이 섞여 있으면 각 주제를 다음 순서로 독립 처리합니다.

1. 주제마다 기존 Jira 이슈를 선택하거나 `.github/jira_issue_template.md`로 새 이슈를 생성합니다.
2. `dev` 기준으로 `<type>/<Jira-Key>-<작업명>` 브랜치를 각각 생성합니다.
3. 해당 주제의 파일만 선택적으로 스테이징하고 `[<Jira-Key>] <type>: <설명>` 형식으로 커밋합니다.
4. 각 Jira 이슈에 대응하는 GitHub Issue를 0-3단계에 따라 확인하거나 생성합니다.
5. 각 브랜치를 push하고 `dev` 대상 PR과 최신 HEAD 기준 AI 리뷰 댓글을 각각 생성합니다.
6. Jira Key, GitHub Issue 번호, 브랜치와 PR URL을 주제별로 최종 보고합니다.

작업 파일을 다른 브랜치로 분리할 때는 워킹 트리의 사용자 변경을 보존하고, 한 주제의 변경을 다른 Jira 이슈나 PR에 섞지 않습니다.

---

## 2. 조건부 커밋 및 푸시 (단일 주제인 경우)

**단일 주제 변경사항이 있는 경우에만 실행**:

> [!IMPORTANT]
> **원자적 커밋(Atomic Commits)**: 변경사항이 여러 기능이나 서로 다른 수정 사항을 포함하고 있다면, `git add -p` 등을 사용하여 **기능별로 커밋을 나누어** 진행하십시오. 한 번에 모든 변경사항을 하나의 커밋으로 묶지 마십시오.

```bash
# 기능별로 나누어 스테이징 및 커밋 (필요시 반복)
# git add <file_functional_group>
# git commit -m "[<Jira-Key>] <type>: <설명>"

# Conventional Commit 메시지 생성 (0-1단계에서 추출한 값 사용)
git commit -m "[$JIRA_KEY] $BRANCH_TYPE: <설명>"

# 원격에 푸시
git push origin $CURRENT_BRANCH
```

> [!IMPORTANT]
> **PR 브랜치에 push한 경우 AI 코드 리뷰 댓글 등록은 필수입니다.** 기존 리뷰 댓글이 있더라도 최신 HEAD 전체 diff를 다시 분석하여 새 리뷰 댓글을 남겨야 하며, 리뷰 결과가 `치명적 0건`이어도 생략할 수 없습니다. 코드 리뷰 내용은 PR 본문에 포함하지 않습니다.

---

## 3. Target Branch 확인 (Git Flow 변형)

| 현재 브랜치 패턴 | Target Branch | 설명 |
| :--- | :--- | :--- |
| `<type>/<Jira-Key>-<작업명>` | **`dev`** | 기능 개발, 수정, 테스트, 문서 및 환경 작업 |

`BRANCH_TYPE`, `JIRA_KEY`, `TARGET_BRANCH`는 0-1단계에서 한 번만 정의한 값을 계속 사용합니다.

---

## 4. PR 존재 여부 확인 (필수!)

```bash
PR_URL=$(gh pr view --json url,state --jq 'select(.state == "OPEN") | .url' 2>/dev/null || echo "")
```

| 결과 | 상태 |
| :--- | :--- |
| URL 있음 | PR이 이미 존재 → **4-B로** (업데이트) |
| 비어있음 | PR 없음 → **4-A로** (신규 생성) |

---

## 4-A. PR 신규 생성 (PR이 없는 경우)

**반드시 실행해야 하는 단계**:

### Step 1: 원격과의 차이 확인

```bash
git fetch origin $TARGET_BRANCH
COMMITS=$(git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline)
```

- 커밋이 없으면: "base 브랜치($TARGET_BRANCH) 대비 새로운 커밋이 없습니다." 보고 후 종료

### Step 2: PR 본문 및 AI 코드 리뷰 분리 작성

1. **PR 본문 작성:** 반드시 **`.github/pull_request_template.md`** 파일의 양식을 기반으로 `.pr_body_temp.md`를 작성합니다:
   - 📌 배경 및 목적 (Background)
   - 📋 주요 변경 사항 (Change Summary)
   - 📁 변경된 파일 (Changed Files)
   - 🧪 검증 절차 및 결과 (Testing Procedure: `.\gradlew.bat test`, Java 21 게임 동작, 기능/비기능 체크)
   - ✅ Jira 완료 조건 체크리스트 (Jira 문구를 그대로 복사하고 충족된 항목만 체크)
   - 📝 추가 참고사항 (Additional Notes)
   - 🔗 연관 이슈 (Jira의 `$JIRA_KEY` 및 URL, 대응 GitHub Issue 번호. 이 PR이 해결하면 `Closes #<번호>`)
   - 🔀 Merge 가이드 (Target: `<TARGET_BRANCH>`, Squash and Merge 권장)

2. **AI 코드 리뷰 작성:** 반드시 **`.github/code_review_template.md`** 파일의 테트리스 체크포인트(게임플레이, 20x10 보드, 7종 블럭, 색맹 모드, 점수판 영속성, Java 21 호환 등)와 3단계 우선순위(🔴치명적 / ⚠️경고 / 💡제안)를 확인합니다.
   - `git diff origin/$TARGET_BRANCH...$CURRENT_BRANCH`를 분석하여 `.pr_review_temp.md`에 작성합니다.
   - 리뷰에는 검토한 최신 HEAD SHA와 검증 결과를 포함합니다.
   - `.pr_review_temp.md`의 내용은 `.pr_body_temp.md`에 복사하거나 합치지 않습니다. PR 본문과 코드 리뷰 댓글은 반드시 분리합니다:
   ```markdown
   ## 🤖 AI Code Reviewer Report (.github/code_review_template.md 기반)

   > 본 리뷰는 코드 품질 참고용으로 자동 분석된 사전 검토 피드백이며, PR 머지를 차단하지 않습니다 (Non-blocking Report).

   ### 📊 종합 요약
   <diff 기반 주요 변경 및 도메인 영향 요약>

   ### 🔴 치명적 (Critical)
   *(없으면 '해당 없음')*
   - **파일:라인** - 이슈 설명 및 수정 권고안

   ### ⚠️ 경고 (Warning)
   *(없으면 '해당 없음')*
   - **파일:라인** - 이슈 설명

   ### 💡 제안 (Suggestion)
   - (개선 제안 1-3개, 없으면 생략)
   ```

### Step 3: Jira Key 기반 PR 제목 정의

브랜치명에서 Jira Key와 브랜치 유형을 추출하여 커밋 메시지와 동일한 형식의 PR 제목을 만듭니다. Jira 이슈는 0-2단계에서 존재가 확인되어 있어야 하고, 대응 GitHub Issue는 0-3단계에서 확정되어 있어야 합니다. PR 본문에 Jira 링크와 GitHub Issue 번호를 함께 적고, 이 PR이 해당 작업을 해결할 때만 `Closes #<번호>`를 사용합니다. GitHub-Jira 연동이 활성화된 환경에서는 브랜치, 커밋, PR 제목의 Jira Key를 기준으로 개발 정보가 자동 연결됩니다.

```bash
# 예: feat/TET-12-menu-colorblind-ui -> [TET-12] feat: 색맹 모드 메뉴 UI 구현
PR_TITLE="[$JIRA_KEY] $BRANCH_TYPE: <종합된 변경 제목>"
```

### Step 4: PR 생성 및 AI 코드 리뷰 코멘트 분리 등록

```bash
# 1. PR 생성 (.github/pull_request_template.md 기반 본문)
PR_URL=$(gh pr create \
  --title "$PR_TITLE" \
  --body-file .pr_body_temp.md \
  --base $TARGET_BRANCH)

echo "✅ PR 생성 완료: $PR_URL"

# 2. PR 생성 직후 별도 댓글(Comment)로 AI 코드 리뷰 필수 등록
test -s .pr_review_temp.md || {
  echo "❌ Error: AI 코드 리뷰 파일이 없거나 비어 있습니다. PR 본문이 아닌 별도 댓글용 리뷰를 작성하세요."
  exit 1
}

REVIEW_COMMENT_URL=$(gh pr comment "$PR_URL" --body-file .pr_review_temp.md)
echo "🤖 AI 코드 리뷰 코멘트 등록 완료: $REVIEW_COMMENT_URL"
```

### Step 5: PR 완료 조건 체크 및 Jira 상태 반영

PR과 리뷰 댓글 생성이 끝나면 Jira 완료 조건을 다시 확인하고 PR 본문의 체크리스트를 갱신합니다. Jira 설명의 완료 조건은 수정하지 않습니다.

1. 1-A-1단계에서 준비한 항목별 판정과 검증 근거를 다시 확인합니다.
2. PR 생성 자체가 조건인 항목은 실제 PR URL과 상태를 확인한 뒤 PR에서 체크합니다.
3. 병합, 배포 또는 사람의 수동 확인이 필요한 항목은 해당 결과가 확인되기 전까지 체크하지 않습니다.
4. PR 본문을 갱신하여 충족 수, 미완료 항목과 확인 불가 사유가 리뷰어에게 보이도록 합니다.
5. Jira 이슈는 `검토 중`으로 전환하고, 완료 조건 자체와 체크 상태는 PR에서 관리합니다.

PR 본문 갱신에 실패하면 한 번만 최신 PR 상태를 다시 조회해 재시도합니다. 그래도 실패하면 체크되지 않은 항목과 오류를 사용자에게 보고합니다.

### Step 6: 임시 파일 정리

```bash
rm -f .pr_body_temp.md .pr_review_temp.md
```

---

## 4-B. 기존 PR 업데이트 (PR이 있는 경우)

### Step 1: 변경 내역 분석

```bash
git fetch origin $TARGET_BRANCH
git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline
```

### Step 2: PR 본문 및 AI 코드 리뷰 재작성

`.github/pull_request_template.md` 및 `.github/code_review_template.md`를 참조하여 최신 변경사항을 `.pr_body_temp.md` 및 `.pr_review_temp.md`에 재작성합니다.

> **필수:** PR 브랜치에 새 커밋을 push할 때마다 최신 HEAD 기준 전체 diff를 다시 검토하고 새 AI 코드 리뷰 댓글을 등록합니다. 이전 리뷰가 존재하거나 지적 사항이 0건이어도 생략하지 않습니다. 리뷰에는 검토한 HEAD SHA와 검증 결과를 포함하며, 리뷰 내용을 PR 본문에 넣지 않습니다.

### Step 3: PR 본문 업데이트 및 새 리뷰 코멘트 등록

```bash
# 1. PR 본문 갱신
PR_TITLE="[$JIRA_KEY] $BRANCH_TYPE: <종합된 변경 제목>"

gh pr edit \
  --title "$PR_TITLE" \
  --body-file .pr_body_temp.md

# 2. 최신 HEAD 기준 AI 코드 리뷰 댓글 필수 등록
test -s .pr_review_temp.md || {
  echo "❌ Error: AI 코드 리뷰 파일이 없거나 비어 있습니다. PR 본문이 아닌 별도 댓글용 리뷰를 작성하세요."
  exit 1
}

REVIEW_COMMENT_URL=$(gh pr comment "$PR_URL" --body-file .pr_review_temp.md)
echo "🤖 최신 AI 코드 리뷰 코멘트 등록 완료: $REVIEW_COMMENT_URL"
```

새 커밋의 변경사항과 검증 결과가 Jira 완료 조건에 영향을 주는지 다시 판정합니다. Jira의 완료 조건 문구를 기준으로 PR 체크리스트를 최신 상태로 갱신하고, 더 이상 충족되지 않는 항목은 PR에서 체크를 해제한 뒤 사유를 기록합니다.

### Step 4: 정리

```bash
rm -f .pr_body_temp.md .pr_review_temp.md
```

---

## 5. 최종 보고

반드시 아래 내용을 보고:

| 항목 | 값 |
| :--- | :--- |
| 브랜치 | `$CURRENT_BRANCH` |
| 커밋 | O / X (커밋 메시지) |
| 푸시 | O / X |
| PR 상태 | 신규 생성 / 업데이트 / 변경없음 |
| PR URL | `<URL>` |
| Jira | `$JIRA_KEY` |
| GitHub Issue | `#<번호>` 및 URL |
| Jira 정합성 | 일치 / 보완 후 일치 / 별도 이슈로 분리 |
| PR의 Jira 완료 조건 | `<충족 수>/<전체 수>` 및 미완료·확인 불가 사유 |
| Target | `$TARGET_BRANCH` (`dev`) |
| AI 리뷰 | O / X (최신 HEAD SHA 및 댓글 URL) |

---

## ⚠️ 주의사항

1. **main 및 dev에 직접 push 절대 금지** - 반드시 허용 브랜치(`feat/*`, `fix/*`, `refactor/*`, `test/*`, `docs/*`, `chore/*`)에서 PR을 통해 `dev`에 병합합니다.
2. **PR 본문과 AI 리뷰 분리** - PR 본문은 `.github/pull_request_template.md` 기반으로 작성하고, AI 리뷰는 `.github/code_review_template.md` 기반으로 분석하여 별도 댓글(`gh pr comment`)로 분리 등록합니다.
3. **PR 존재 확인 필수** - `gh pr view`로 먼저 확인 후 신규 생성 또는 업데이트를 결정합니다.
4. **다중 주제 분할 필수** - 변경사항에 둘 이상의 성격(예: 게임 로직 + UI 개편)이 섞인 경우, 반드시 브랜치를 분할하여 개별 PR을 생성합니다.
5. **Jira Key 및 실제 이슈 필수**: 브랜치명에는 `[A-Z0-9]+-[0-9]+` 형식의 Jira Key가 반드시 포함되어야 하며, `TET` 프로젝트에 실제로 존재하는 이슈여야 합니다. 대응 GitHub Issue는 중복 확인 후 간결한 요약과 Jira 링크로 작성합니다.
6. **Jira-작업 범위 정합성 필수**: 커밋 및 push 전에 Jira의 목적·범위·완료 조건과 전체 diff를 비교합니다. 독립 작업을 기존 Jira에 억지로 포함하지 않습니다.
7. **Jira 완료 조건은 PR에서 체크**: Jira의 완료 조건을 PR 본문에 그대로 복사하고, 실제 diff, 검증 결과와 원격 상태로 확인된 항목만 체크합니다. Jira 설명은 체크 상태 기록을 위해 수정하지 않습니다.
8. **커밋/PR 제목 형식 일치**: 커밋 메시지와 PR 제목은 `[Jira-Key] <type>: <설명>` 형식을 사용하고, `<type>`은 현재 브랜치 접두사와 일치해야 합니다. (예: `test/TET-12-score-coverage` → `[TET-12] test: 점수 계산 테스트 및 커버리지 확보`)
9. **코드 리뷰는 별도 댓글로 필수 등록**: AI 코드 리뷰를 PR 본문에 포함하지 말고, `gh pr comment --body-file .pr_review_temp.md`로 등록합니다. push 후에는 최신 HEAD 전체 diff를 다시 검토하며, 지적 사항이 없어도 댓글 등록을 생략하지 않습니다.
