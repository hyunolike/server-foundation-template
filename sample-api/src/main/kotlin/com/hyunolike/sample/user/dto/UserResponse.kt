package com.hyunolike.sample.user.dto

/** 컨트롤러가 그대로 반환하는 도메인 DTO. 봉투는 파운데이션이 씌운다. */
data class UserResponse(
    val userId: Long,
    val email: String,
    val createdAt: String,
)
