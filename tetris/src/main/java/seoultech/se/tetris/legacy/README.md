# 참조 코드 (임시)

교수님이 제공한 참조 코드 (SeoulTech-SE-Tetris-Ref).
**이 코드는 구현 베이스가 아니라 참고용이다. 구현이 완료되면 legacy 폴더 전체를 삭제한다.**

- 원본 README가 "그대로 베이스로 쓰지 말 것"이라고 명시
- 계층 규칙 2 위반 (블럭이 색을 들고 있음, `docs/project-conventions.md` 8장)
- 게임 로직이 JFrame 안에 섞여 있음
- 알려진 버그: O 블럭 미생성(`nextInt(6)`), 회전 미구현

현재 Gradle 기본 진입점은 `seoultech.se.tetris.ui.TetrisApplication`이며, 이 참조 코드의 실행 클래스는 사용하지 않는다.
