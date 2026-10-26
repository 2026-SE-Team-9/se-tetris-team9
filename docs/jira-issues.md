# Jira 이슈 목록

> 팀의 작업 계획과 진행 상태는 Jira `TET` 프로젝트에서 관리했다.
> Jira는 팀원만 볼 수 있어서, 이 문서에 이슈 목록과 연결된 GitHub Issue·PR을 옮겨 둔다.

- 범위: 2차 구현까지의 TET-5~TET-56 (2026-10-25 기준). 아래 52건은 모두 완료 상태다.
- 제목과 유형은 Jira에 기록된 값을 그대로 옮겼다.
- 날짜는 Jira의 생성·완료 시각(KST)이고, 연도는 모두 2026년이다.

---

## 1. 요약

| 담당 | 스토리 | 작업 | 합계 |
|---|---:|---:|---:|
| 전재균 | 14 | 11 | 25 |
| 김현서 | 5 | 11 | 16 |
| Junhyuk Park | 0 | 11 | 11 |
| **합계** | **19** | **33** | **52** |

---

## 2. 전체 목록

| 키 | 제목 | 유형 | 담당 | 생성 | 완료 | Issue | PR |
|---|---|---|---|---|---|---|---|
| TET-5 | chore: Jira 연동 테스트 | 작업 | 전재균 | 09-26 | 09-27 | — | — |
| TET-6 | docs: 팀 프로젝트 협업 규칙 작성 | 작업 | 전재균 | 09-26 | 09-28 | — | [#16](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/16) |
| TET-7 | docs: 4주차 이전 회의기록 작성 | 작업 | 전재균 | 09-26 | 09-27 | — | [#15](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/15) |
| TET-8 | chore: 1차 요구사항 세부내역 정리 문서 작성 | 작업 | 전재균 | 09-26 | 09-27 | — | [#14](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/14) |
| TET-9 | chore: GitHub 작업 흐름을 Jira 이슈 기반으로 정비 | 작업 | 김현서 | 09-26 | 09-27 | — | [#13](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/13) |
| TET-10 | chore: PR 검증 절차 템플릿 및 Gradle 빌드 설정 개선 | 작업 | 김현서 | 09-28 | 09-29 | — | [#17](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/17) |
| TET-11 | refactor: 참조 소스 코드를 legacy 패키지로 격리 | 작업 | 전재균 | 09-29 | 09-29 | — | [#18](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/18) |
| TET-12 | feat: GameState 인터페이스에 레벨 조회 추가 및 보드 주석 명확화 | 스토리 | 전재균 | 09-29 | 09-29 | — | [#19](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/19) |
| TET-13 | feat: 시작 화면 및 메뉴 UI 구현 | 작업 | Junhyuk Park | 10-01 | 10-04 | — | [#27](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/27) |
| TET-14 | feat: 키 설정 저장 서비스 구현 | 작업 | 김현서 | 10-01 | 10-04 | — | [#20](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/20) |
| TET-15 | feat: 블럭 타입·블럭·보드 모델 구현 | 스토리 | 전재균 | 10-04 | 10-04 | — | [#21](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/21) |
| TET-16 | chore: dev·main PR의 JaCoCo 라인 커버리지 CI 구축 | 작업 | 김현서 | 10-04 | 10-05 | — | [#22](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/22) |
| TET-17 | feat: 게임 조작 키 입력을 컨트롤러에 연결 | 스토리 | 김현서 | 10-04 | 10-06 | — | [#24](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/24) |
| TET-18 | docs: 계층 규칙의 모델 범위 명시 | 작업 | 전재균 | 10-04 | 10-04 | — | [#23](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/23) |
| TET-19 | refactor: 메인·서브 키 설정과 전체 임시 편집·저장 흐름 정리 | 작업 | 김현서 | 10-05 | 10-12 | — | [#25](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/25) |
| TET-20 | chore: CODEOWNERS로 PR 리뷰 담당자 자동 지정 | 작업 | 김현서 | 10-05 | 10-06 | — | [#26](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/26) |
| TET-21 | docs: 3차 회의기록 작성 | 작업 | 전재균 | 10-06 | 10-07 | — | [#28](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/28) |
| TET-22 | feat: 블럭 생성·레벨·점수 계산 정책 구현 | 스토리 | 전재균 | 10-07 | 10-08 | — | [#29](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/29) |
| TET-23 | chore: PR CI에 JUnit 테스트 단계 추가 | 작업 | 김현서 | 10-07 | 10-08 | — | [#30](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/30) |
| TET-24 | chore: GitHub Issue 연계 운영과 2차 요구사항 원문 등록 | 작업 | 김현서 | 10-08 | 10-12 | [#31](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/31) | [#32](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/32) |
| TET-25 | feat: 게임 진행 엔진(TetrisEngine) 구현 | 스토리 | 전재균 | 10-08 | 10-11 | [#34](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/34) | [#33](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/33) |
| TET-26 | feat: 스코어보드 순위 관리(ScoreBoard) 구현 | 스토리 | 전재균 | 10-12 | 10-12 | [#35](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/35) | [#36](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/36) |
| TET-27 | feat: 인게임 화면 UI 및 보드 렌더링 구현 | 작업 | Junhyuk Park | 10-12 | 10-12 | [#96](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/96) | [#37](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/37) |
| TET-28 | feat: 시작 메뉴에서 게임 화면으로 진입하고 기본 실행점 연결 | 스토리 | 김현서 | 10-12 | 10-12 | [#38](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/38) | [#39](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/39) |
| TET-29 | chore: Windows 실행 파일에 사용자 지정 아이콘 적용 | 작업 | 김현서 | 10-13 | 10-15 | [#40](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/40) | [#41](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/41) |
| TET-30 | docs: 2차 요구사항 세부내역 정리 문서 작성 | 작업 | 전재균 | 10-13 | 10-14 | [#42](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/42) | [#43](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/43) |
| TET-31 | docs: 4차 회의기록 작성 | 작업 | 전재균 | 10-14 | 10-15 | [#44](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/44) | [#45](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/45) |
| TET-32 | feat: 모드별 스코어보드와 CSV 기록 저장소 구현 | 스토리 | 전재균 | 10-15 | 10-15 | [#46](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/46) | [#47](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/47) |
| TET-33 | feat: 종료 화면 구현 | 작업 | Junhyuk Park | 10-15 | 10-15 | [#97](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/97) | [#54](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/54) |
| TET-34 | refactor: 게임 진행 타이머 분리 및 상태 연동 | 작업 | 김현서 | 10-15 | 10-15 | [#48](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/48) | [#49](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/49) |
| TET-35 | feat: 설정 화면과 사용자 설정 저장 | 작업 | 김현서 | 10-15 | 10-16 | [#52](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/52) | [#53](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/53) |
| TET-36 | feat: 스코어보드 화면 구현 | 작업 | Junhyuk Park | 10-16 | 10-16 | [#98](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/98) | [#59](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/59) |
| TET-37 | feat: 설정 화면 키보드 탐색과 Esc 저장 확인 지원 | 스토리 | 김현서 | 10-16 | 10-18 | [#55](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/55) | [#57](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/57) |
| TET-38 | feat: 일시정지 메뉴와 설정·타이틀 복귀 연결 | 스토리 | 김현서 | 10-16 | 10-18 | [#56](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/56) | [#58](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/58) |
| TET-39 | feat: 설정 화면에서 난이도 설정 제거 | 작업 | Junhyuk Park | 10-16 | 10-17 | [#99](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/99) | [#60](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/60) |
| TET-40 | docs: 2차 요구사항 문서에 게임 중 난이도 변경 제외 결정 추가 | 작업 | 전재균 | 10-16 | 10-17 | [#61](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/61) | [#63](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/63) |
| TET-41 | feat: 2차 요구사항에 맞춘 블럭 표시 문자 변경 (O형 O, L형 V) | 스토리 | 전재균 | 10-16 | 10-17 | [#62](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/62) | [#64](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/64) |
| TET-42 | feat: 난이도별 블럭 생성 확률과 낙하 속도 규칙 구현 | 스토리 | 전재균 | 10-17 | 10-18 | [#65](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/65) | [#66](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/66) |
| TET-43 | feat: 화면 크기 설정 구현 | 작업 | Junhyuk Park | 10-17 | 10-18 | [#67](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/67) | [#68](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/68) |
| TET-44 | feat: 시작 메뉴 스코어보드 화면 구현 | 작업 | Junhyuk Park | 10-17 | 10-18 | [#69](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/69) | [#70](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/70) |
| TET-45 | feat: 설정한 난이도를 게임 진행과 점수 기록에 적용 | 스토리 | 전재균 | 10-18 | 10-18 | [#71](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/71) | [#72](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/72) |
| TET-46 | feat: 줄 삭제 애니메이션 상태와 진행 제어 구현 | 스토리 | 전재균 | 10-19 | 10-19 | [#73](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/73) | [#74](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/74) |
| TET-47 | feat: 아이템 모드의 아이템 종류와 블럭·보드 칸 번호 확장 | 스토리 | 전재균 | 10-19 | 10-21 | [#75](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/75) | [#76](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/76) |
| TET-48 | feat: 아이템 모드에서 10줄마다 아이템 블럭 등장 | 스토리 | 전재균 | 10-21 | 10-22 | [#77](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/77) | [#78](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/78) |
| TET-49 | feat: 줄 삭제 애니메이션 구현 | 작업 | Junhyuk Park | 10-22 | 10-24 | [#79](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/79) | [#80](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/80) |
| TET-50 | feat: 아이템 블럭 화면 구현 | 작업 | Junhyuk Park | 10-22 | 10-24 | [#85](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/85) | [#86](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/86) |
| TET-51 | docs: 2차 요구사항 문서의 아이템 효과 이후 처리 규칙 보완 | 작업 | 전재균 | 10-22 | 10-24 | [#81](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/81) | [#83](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/83) |
| TET-52 | feat: 아이템 5종의 효과 구현 | 스토리 | 전재균 | 10-22 | 10-24 | [#82](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/82) | [#84](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/84) |
| TET-53 | feat: 스코어보드별 난이도 표시 | 작업 | Junhyuk Park | 10-24 | 10-24 | [#87](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/87) | [#88](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/88) |
| TET-54 | feat: 시작 메뉴 아이템 모드 선택 추가 | 작업 | Junhyuk Park | 10-24 | 10-24 | [#89](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/89) | [#92](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/92) |
| TET-55 | feat: 고스트 블럭 착지 위치 표시 | 스토리 | 김현서 | 10-24 | 10-24 | [#90](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/90) | [#95](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/95) |
| TET-56 | feat: 게임 화면에 색맹 모드 색표 적용 | 스토리 | 전재균 | 10-24 | 10-24 | [#93](https://github.com/2026-SE-Team-9/se-tetris-team9/issues/93) | [#94](https://github.com/2026-SE-Team-9/se-tetris-team9/pull/94) |

---

## 3. 참고

- TET-5는 Jira와 GitHub 연동을 확인하려고 만든 이슈라 PR이 없다.
- GitHub Issue는 TET-24부터 Jira 이슈마다 만들었다. 빠져 있던 TET-27·33·36·39는 나중에 #96~#99로 보완했다.
- PR 열에는 병합된 PR만 적었다. TET-33은 #50·#51을 닫고 #54로, TET-55는 #91을 닫고 #95로 병합했다.
- Jira를 도입하기 전의 PR(#1, #4, #5, #8, #10, #12)은 이 목록에 없다.
- 이 문서를 추가한 TET-57도 목록에 없다.
