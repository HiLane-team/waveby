package com.drivepark.app.domain

data class LatLng(val lat: Double, val lng: Double)

data class Region(
    val id: String,
    val name: String,
    val mascotName: String,
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
)

/** 대략적인 행정구역을 덮는 사각형. 지역 판별은 기기 안에서만 한다. */
object Regions {
    val ALL: List<Region> = listOf(
        Region("hoengseong", "횡성", "한우 음메롱", 37.35, 37.65, 127.85, 128.25),
        Region("boseong", "보성", "보성 녹차", 34.60, 34.95, 127.00, 127.40),
        Region("jeju", "제주", "제주 감귤", 33.10, 33.60, 126.10, 127.00),
        Region("gangneung", "강릉", "강릉 커피", 37.55, 37.95, 128.70, 129.10),
        Region("yeongdeok", "영덕", "영덕 대게", 36.25, 36.65, 129.20, 129.50),
    )
}
