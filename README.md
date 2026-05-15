# TechProof

TechProof는 Word(`.docx`, `.doc`) 기술 문서에서 조사/맞춤법 오류를 점검해 주는 JavaFX 기반 데스크톱 앱입니다.

## 다운로드

- [Windows 설치파일 다운로드 (TechProof-0.2.5.exe)](https://github.com/sikdong/techproof/releases/download/v0.2.5/TechProof-0.2.5.exe)

## 버전별 변경 사항

<details open>
<summary>v0.2.5</summary>

- 도면부호 명칭 추출 범위를 최대 4어절로 확장
- `버퍼층(BF)`, `제1 버퍼층(BF1)`, `제2 버퍼층(BF2)`처럼 서수형 명칭을 별개 도면부호로 구분
- 도면부호 앞 후보 어절에 조사가 있으면 조사 뒤 어절부터 명칭으로 인식
- 최신 버전 실행 중에도 업데이트 알림이 뜨는 fallback 버전 불일치 수정

</details>

<details>
<summary>v0.2.4</summary>

- 도면부호 명칭 추출 시 괄호 앞 1~2어절 문맥은 유지하되, 앞쪽 어절 말미의 `은/는/이/가/을/를` 조사는 명칭에서 제외
- `제3`, `제10` 등 다양한 서수형 명칭이 도면부호 명칭에서 제거되지 않도록 보존

</details>

<details>
<summary>v0.2.3</summary>

- 조사 검사에서 `은/는/이/가`에 더해 `을/를` 받침 오류 인식 지원
- 괄호 도면부호가 붙은 표현의 `을/를` 조사 오류도 감지하도록 개선
- 앱 전체 기본 글자 크기를 키우고 한글/영문/숫자 폰트를 통일

</details>

<details>
<summary>v0.2.2</summary>

- 도면부호 목록에서 일치/불일치 상태, 기준 도면부호, 문맥 정보를 별도 항목으로 표시
- 숫자/영문 조합 또는 영문 코드형 도면부호 인식 지원
- 외래어를 영어로 병기한 괄호 표기는 도면부호 검사에서 제외
- 서로 다른 복합 명칭이 공통 끝 단어만으로 오탐지되지 않도록 개선

</details>

<details>
<summary>v0.2.1</summary>

- Word 구형 문서(`.doc`) 업로드 지원 추가
- `Run Check` 실행 중 진행 문구와 진행률 표시 추가
- 검사 완료 시 사용자에게 완료 팝업 표시
- 검사 결과 화면을 `오타 검사 항목`과 `도면부호 검사 항목` 탭으로 분리

</details>

<details>
<summary>v0.2.0</summary>

- 도면부호 오류 탐지 기능 추가
- 업로드된 파일 전체 기준으로 같은 용어의 도면부호 불일치 탐지
- 괄호 앞 최대 3어절 명칭 기준 도면부호 비교

</details>

<details>
<summary>v0.1.0</summary>

- Word(`.docx`) 기술 문서 업로드 및 문단 단위 검사
- 조사/맞춤법/띄어쓰기/문법 오류 후보 탐지
- Windows 설치파일 배포

</details>

## 설치 방법 (Windows)

1. `TechProof-0.2.5.exe`을 실행합니다.
2. 설치 마법사 안내에 따라 설치를 완료합니다.
3. 시작 메뉴 또는 바탕화면 바로가기에서 `TechProof`를 실행합니다.
