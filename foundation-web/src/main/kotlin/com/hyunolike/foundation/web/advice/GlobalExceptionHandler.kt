package com.hyunolike.foundation.web.advice

import com.hyunolike.foundation.core.error.BusinessException
import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.error.ErrorCode
import com.hyunolike.foundation.core.response.ApiError
import com.hyunolike.foundation.core.response.ApiErrorDetail
import com.hyunolike.foundation.core.response.ApiResponse
import com.hyunolike.foundation.web.support.ErrorMessageResolver
import com.hyunolike.foundation.web.support.HttpStatusMapper
import com.hyunolike.foundation.web.support.ResponseMetaFactory
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * DispatcherServlet 안에서 던져진 예외를 봉투로 바꾼다. (설계 문서 §5.2, §5.3)
 *
 * [ResponseEntityExceptionHandler] 를 상속하는 이유는 Spring 6 가 프레임워크 예외를
 * RFC 7807 `ProblemDetail` 로 내보내기 때문이다. [handleExceptionInternal] 을 덮어
 * 그 본문을 우리 봉투로 교체하지 않으면, 같은 API 에서 두 가지 실패 모양이 나간다.
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val metaFactory: ResponseMetaFactory,
    private val messages: ErrorMessageResolver,
) : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        exception: BusinessException,
        request: WebRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = exception.errorCode
        log.warn("business-exception code={} status={}", errorCode.code, errorCode.status)
        return envelope(errorCode, pathOf(request), exception.details, exception.messageArguments)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: WebRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 내부 메시지는 절대 밖으로 내보내지 않는다. 로그에만 전문을 남긴다.
        log.error("unexpected-exception", exception)
        return envelope(CommonErrorCode.COMMON_INTERNAL_ERROR, pathOf(request))
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val details =
            ex.bindingResult.fieldErrors.map { fieldError ->
                ApiErrorDetail(
                    field = fieldError.field,
                    reason = fieldError.defaultMessage ?: "올바르지 않은 값입니다.",
                )
            } +
                ex.bindingResult.globalErrors.map { globalError ->
                    ApiErrorDetail(
                        field = globalError.objectName,
                        reason = globalError.defaultMessage ?: "올바르지 않은 값입니다.",
                    )
                }

        val body = envelope(CommonErrorCode.COMMON_INVALID_PARAMETER, pathOf(request), details)
        return ResponseEntity.status(body.statusCode).headers(headers).body(body.body)
    }

    /** 프레임워크가 만든 모든 실패 응답의 본문을 봉투로 교체한다. */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorCode = HttpStatusMapper.toErrorCode(statusCode.value())
        val envelope =
            ApiResponse.failure(
                error = ApiError(errorCode.code, messages.resolve(errorCode), null),
                meta = metaFactory.create(pathOf(request)),
            )
        return ResponseEntity.status(statusCode).headers(headers).body(envelope)
    }

    private fun envelope(
        errorCode: ErrorCode,
        path: String,
        details: List<ApiErrorDetail> = emptyList(),
        messageArguments: List<Any> = emptyList(),
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.valueOf(errorCode.status))
            .body(
                ApiResponse.failure(
                    error =
                        ApiError(
                            code = errorCode.code,
                            message = messages.resolve(errorCode, messageArguments),
                            details = details.takeIf { it.isNotEmpty() },
                        ),
                    meta = metaFactory.create(path),
                ),
            )

    private fun pathOf(request: WebRequest): String = (request as? ServletWebRequest)?.request?.requestURI ?: request.getDescription(false)
}
