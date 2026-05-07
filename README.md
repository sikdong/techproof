# TechProof

로컬에서 실행되는 Word(.docx) 기술 문서 오타/조사 오류 검출 프로그램 MVP입니다.

## 기능

- `.docx` 파일 불러오기
- 문단 및 표 안 텍스트 추출
- 괄호 안 도면부호를 원문에서 제거하지 않고 조사 판단 시에만 무시
- `은/는`, `이/가`, `을/를` 조사 오류 검출
- 사용자 오타 사전 기반 오타 검출
- 결과 테이블 표시
- CSV 저장

## 실행 방법

Java 21과 Gradle이 설치되어 있어야 합니다.

```bash
gradle run
```

## 사전 수정

기본 오타 사전은 아래 파일입니다.

```text
src/main/resources/dictionary/typo-dictionary.json
```

예:

```json
{
  "잇다": "있다",
  "됬다": "됐다"
}
```

## 패키징 예시

```bash
gradle clean build
jpackage --input build/libs --name TechProof --main-jar techproof-0.1.0.jar --type exe
```

JavaFX 런타임 포함 패키징은 별도 런타임 이미지 구성이 필요할 수 있습니다.
