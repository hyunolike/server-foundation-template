## 무엇을 바꿨나

<!-- 한두 문장. 왜 필요한지가 제목에서 안 보이면 여기 적는다. -->

## API 계약에 영향이 있나

- [ ] 응답 형태가 바뀐다 (`docs/openapi/openapi.json` diff 확인)
- [ ] 에러 코드를 추가/변경/삭제했다
- [ ] 영향 없음

> 스펙을 바꿨다면 `./gradlew :sample-api:updateOpenApi` 로 갱신해 함께 커밋한다.

## 확인한 것

- [ ] `./gradlew check` 통과
- [ ] 결정이 필요한 변경이면 `docs/adr/` 에 ADR 을 추가했다
