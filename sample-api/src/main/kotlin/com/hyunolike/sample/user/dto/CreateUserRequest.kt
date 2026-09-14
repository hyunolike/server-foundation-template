package com.hyunolike.sample.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 아닙니다.")
    val email: String,
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val password: String,
)
