package com.hyunolike.foundation.web.advice

import com.hyunolike.foundation.core.response.ApiError
import com.hyunolike.foundation.core.response.ApiResponse
import com.hyunolike.foundation.web.support.ErrorMessageResolver
import com.hyunolike.foundation.web.support.HttpStatusMapper
import com.hyunolike.foundation.web.support.ResponseMetaFactory
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `/error` 로 포워딩된 요청까지 봉투로 덮는다. (설계 문서 §5.2)
 *
 * 이 컨트롤러가 없으면 매핑 없는 URL 의 404, 405, 그리고 필터 안에서 터진 예외가
 * Boot 의 `BasicErrorController` 로 가서 `{timestamp, status, error, path}` 라는
 * 전혀 다른 모양으로 나간다. Advice 도 예외 핸들러도 그 경로를 타지 않는다.
 *
 * [ErrorController] 빈이 있으면 Boot 는 자기 것을 등록하지 않는다.
 */
@RestController
class FoundationErrorController(
    private val metaFactory: ResponseMetaFactory,
    private val messages: ErrorMessageResolver,
) : ErrorController {
    private val log = LoggerFactory.getLogger(FoundationErrorController::class.java)

    @RequestMapping("\${server.error.path:/error}")
    fun handleError(request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int ?: HttpStatus.INTERNAL_SERVER_ERROR.value()
        val path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String ?: request.requestURI
        val exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) as? Throwable

        if (exception != null) {
            log.error("error-dispatch status={} path={}", status, path, exception)
        }

        val errorCode = HttpStatusMapper.toErrorCode(status)
        return ResponseEntity
            .status(status)
            .body(
                ApiResponse.failure(
                    error = ApiError(errorCode.code, messages.resolve(errorCode), null),
                    meta = metaFactory.create(path),
                ),
            )
    }
}
