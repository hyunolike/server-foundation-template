package com.hyunolike.foundation.test

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers
import org.springframework.test.web.servlet.ResultMatcher

/**
 * 검증의 두 번째 겹. (설계 문서 §7.4)
 *
 * REST Docs 는 "문서의 예시가 실재하는 응답에서 나왔다"만 보장하고, springdoc 이 만든
 * openapi.json 의 존재조차 모른다. 실제 응답이 생성된 스펙을 만족하는지는 이쪽이 본다 —
 * 커스터마이저가 봉투를 잘못 주입하면 여기서 잡힌다.
 */
class OpenApiResponseValidator private constructor(
    private val validator: OpenApiInteractionValidator,
) {
    /** MockMvc 결과가 스펙을 만족하는지 검사하는 매처. */
    fun matcher(): ResultMatcher = OpenApiValidationMatchers.openApi().isValid(validator)

    companion object {
        fun fromSpec(specJson: String): OpenApiResponseValidator =
            OpenApiResponseValidator(
                OpenApiInteractionValidator.createForInlineApiSpecification(specJson).build(),
            )
    }
}
