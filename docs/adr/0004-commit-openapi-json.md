# 0004. openapi.json을 저장소에 커밋하고 CI에서 검증한다

- 상태: 수용됨
- 관련: [설계 문서 §7.5](../design/architecture.md)

## 맥락

OpenAPI 문서는 빌드 산출물이다. 보통 생성물은 커밋하지 않는다. 그러나 커밋하지 않으면 스펙 변경이 PR diff에 보이지 않고, 클라이언트를 깨뜨리는 변경이 리뷰를 그냥 통과한다.

## 결정

`docs/openapi/openapi.json`을 커밋한다. `OpenApiSnapshotTest`가 생성된 스펙과 커밋된 파일을 비교하고, 다르면 빌드를 실패시킨다. 갱신은 `./gradlew :sample-api:updateOpenApi`.

## 결과

- 응답 필드를 하나 바꾸고 스펙 갱신을 잊으면 CI가 막는다. 실제로 확인했다.
- 스펙 생성이 결정적이어야 한다 — 키를 정렬하고, 에러 응답 설명은 요청자의 `Accept-Language`가 아니라 고정된 문서 로케일로 만든다.
- diff가 커질 수 있다. 그 대가로 호환성 검사기가 기준 스펙을 가질 수 있게 된다.
