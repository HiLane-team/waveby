package com.drivepark.app.domain

/** 주행 중 새로 만난 캐릭터를 공급한다. 2단계에서 서버 구현체로 교체한다. */
interface EncounterSource {
    suspend fun fetchNew(): List<Character>
}
