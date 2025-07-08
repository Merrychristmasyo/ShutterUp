package com.example.shutterup.model

import java.util.UUID

data class PhotoUploadData(
    val spotName: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val tags: List<String>,
    val fNumber: String,
    val focalLength: String,
    val iso: String,
    val shutterSpeed: String,
    val lensName: String,
    val cameraName: String,
    val shootingMethod: String,
    val userId: String = "user_001" // 기본 사용자 ID, 추후 실제 사용자 시스템과 연동
) {
    /**
     * 파일명을 자동 생성합니다 (userId + randomString)
     * @return 생성된 파일명
     */
    fun generateFilename(): String {
        val randomString = UUID.randomUUID().toString().take(8)
        return "${userId}_${randomString}"
    }
    
    /**
     * 입력 데이터의 유효성을 검사합니다.
     * @return ValidationResult 검사 결과와 에러 메시지
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // 필수 필드 검사
        if (spotName.isBlank()) {
            errors.add("사진 스팟 이름을 입력해주세요")
        }
        
        // filename은 자동 생성되므로 검사 불필요

        // 위도/경도 범위 검사
        if (latitude < -90.0 || latitude > 90.0) {
            errors.add("위도는 -90도에서 90도 사이여야 합니다")
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            errors.add("경도는 -180도에서 180도 사이여야 합니다")
        }

        // 카메라 설정값 검사
        if (fNumber.isNotBlank() && !isValidFNumber(fNumber)) {
            errors.add("조리개 값(F-number)이 올바르지 않습니다")
        }
        
        if (iso.isNotBlank() && !isValidISO(iso)) {
            errors.add("ISO 값이 올바르지 않습니다")
        }

        if (shutterSpeed.isNotBlank() && !isValidShutterSpeed(shutterSpeed)) {
            errors.add("셔터 속도 값이 올바르지 않습니다")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    private fun isValidFNumber(fNumber: String): Boolean {
        return try {
            val value = fNumber.replace("f/", "").replace("F", "").toDoubleOrNull()
            value != null && value > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidISO(iso: String): Boolean {
        return try {
            val value = iso.toIntOrNull()
            value != null && value in 50..102400
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidShutterSpeed(shutterSpeed: String): Boolean {
        return try {
            // 1/60, 1/125, 2s 등의 형태 검사
            shutterSpeed.isNotBlank() && (
                shutterSpeed.contains("/") || 
                shutterSpeed.contains("s") || 
                shutterSpeed.toDoubleOrNull() != null
            )
        } catch (e: Exception) {
            false
        }
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val messages: List<String>) : ValidationResult()
    }
} 