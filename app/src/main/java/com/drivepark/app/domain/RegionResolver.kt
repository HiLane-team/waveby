package com.drivepark.app.domain

/** 좌표를 지역으로 바꾼다. 경계는 포함하고, 표 순서상 첫 번째로 일치하는 지역을 돌려준다. */
class RegionResolver(private val regions: List<Region> = Regions.ALL) {
    fun resolve(at: LatLng): Region? = regions.firstOrNull {
        at.lat in it.south..it.north && at.lng in it.west..it.east
    }
}
