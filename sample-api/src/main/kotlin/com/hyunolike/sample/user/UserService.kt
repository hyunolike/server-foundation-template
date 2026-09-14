package com.hyunolike.sample.user

import com.hyunolike.foundation.core.error.BusinessException
import com.hyunolike.foundation.core.response.PageResponse
import com.hyunolike.foundation.web.resolver.PageQuery
import com.hyunolike.sample.user.dto.CreateUserRequest
import com.hyunolike.sample.user.dto.UserResponse
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 저장소 선택은 파운데이션의 관심사가 아니다 — 참조 구현은 메모리 맵을 쓴다.
 * 서비스 코드에 봉투나 HTTP 가 등장하지 않는 것만 봐 두면 된다.
 */
@Service
class UserService(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val sequence = AtomicLong(1_000)
    private val store = ConcurrentHashMap<Long, UserResponse>()

    init {
        create(CreateUserRequest(email = "hyunho@example.com", password = "correct-horse"))
    }

    fun get(userId: Long): UserResponse = store[userId] ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    fun list(query: PageQuery): PageResponse<UserResponse> {
        val all = store.values.sortedBy { it.userId }
        val window = all.drop(query.offset().toInt()).take(query.size)
        return PageResponse.of(
            content = window,
            number = query.page,
            size = query.size,
            totalElements = all.size.toLong(),
            sort = query.sort,
        )
    }

    fun create(request: CreateUserRequest): UserResponse {
        if (store.values.any { it.email == request.email }) {
            throw BusinessException(UserErrorCode.USER_EMAIL_DUPLICATED)
        }
        val user =
            UserResponse(
                userId = sequence.incrementAndGet(),
                email = request.email,
                createdAt = TIMESTAMP_FORMAT.format(Instant.now(clock)),
            )
        store[user.userId] = user
        return user
    }

    fun delete(userId: Long) {
        store.remove(userId) ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
    }

    companion object {
        private val TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC)
    }
}
