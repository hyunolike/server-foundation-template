package com.hyunolike.sample.user

import com.hyunolike.foundation.core.response.PageResponse
import com.hyunolike.foundation.docs.ApiErrorCodes
import com.hyunolike.foundation.web.resolver.PageQuery
import com.hyunolike.sample.user.dto.CreateUserRequest
import com.hyunolike.sample.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 봉투를 모르는 유일한 구간. 도메인 DTO 를 반환하거나 BusinessException 을 던진다.
 * (설계 문서 §3.1)
 */
@Tag(name = "User", description = "사용자")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @Operation(summary = "사용자 단건 조회")
    @ApiErrorCodes("USER_NOT_FOUND")
    @GetMapping("/{userId}")
    fun get(
        @PathVariable userId: Long,
    ): UserResponse = userService.get(userId)

    @Operation(summary = "사용자 목록 조회")
    @GetMapping
    fun list(query: PageQuery): PageResponse<UserResponse> = userService.list(query)

    @Operation(summary = "사용자 등록")
    @ApiErrorCodes("USER_EMAIL_DUPLICATED")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): UserResponse = userService.create(request)

    /**
     * 봉투 규칙의 유일한 예외: 본문 없는 성공은 204 로 내보내고 봉투를 쓰지 않는다.
     *
     * 핸들러가 본문을 만들지 않으면 메시지 컨버터가 아예 호출되지 않아
     * `ResponseBodyAdvice` 가 돌 기회가 없다. 봉투를 강제하려면 빈 객체를 지어내야 하는데,
     * 그건 "키는 항상 존재한다"를 지키려다 없는 데이터를 만드는 일이다. (설계 문서 §6.5)
     */
    @Operation(summary = "사용자 삭제")
    @ApiErrorCodes("USER_NOT_FOUND")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable userId: Long,
    ) {
        userService.delete(userId)
    }
}
