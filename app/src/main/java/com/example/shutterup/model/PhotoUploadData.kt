package com.example.shutterup.model

import java.util.UUID

data class PhotoUploadData(
    val spotName: String, //포토스팟 이름 (필수)
    val latitude: Double, //위도 (필수)
    val longitude: Double, //경도 (필수)
    val userId: String = "user_001", //사용자 ID (필수, 기본값)
    val description: String? = null, //설명 (선택)
    val tags: List<String>? = null, //태그 (선택)
    val fNumber: String? = null, //F값 (선택)
    val focalLength: String? = null, //초점거리 (선택)
    val iso: String? = null, //ISO (선택)
    val shutterSpeed: String? = null, //셔터속도 (선택)
    val lensName: String? = null, //렌즈명 (선택)
    val cameraName: String? = null, //카메라명 (선택)
    val shootingMethod: String? = null //촬영방법 (선택)
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

        // 선택적 카메라 설정값 검사 (값이 있을 때만)
        fNumber?.let { value ->
            if (value.isNotBlank() && !isValidFNumber(value)) {
                errors.add("조리개 값(F-number)이 올바르지 않습니다")
            }
        }
        
        iso?.let { value ->
            if (value.isNotBlank() && !isValidISO(value)) {
                errors.add("ISO 값이 올바르지 않습니다")
            }
        }

        shutterSpeed?.let { value ->
            if (value.isNotBlank() && !isValidShutterSpeed(value)) {
                errors.add("셔터 속도 값이 올바르지 않습니다")
            }
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