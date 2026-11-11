package com.drivepark.app.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FakeEncounterSourceTest {
    @Test
    fun generate_returnsRequestedCount() {
        val src = FakeEncounterSource()
        assertTrue(src.generate(0).isEmpty())
        assertEquals(5, src.generate(5).size)
    }

    @Test
    fun generate_producesValidCharacters() {
        FakeEncounterSource().generate(100).forEach {
            assertEquals(it, Character.create(it.parts, it.nickname, it.greeting, it.kind, it.shape, it.look))
        }
    }

    @Test
    fun mix_isAboutThirtyPercentPets() {
        val all = FakeEncounterSource(Random(42), animalShapes = listOf("wolf")).generate(1000)
        val pets = all.filter { it.kind == CharacterKind.PET }
        assertTrue("반려동물 ${pets.size}명", pets.size in 250..350)
        pets.forEach {
            assertTrue(it.nickname in FakeEncounterSource.PET_NAMES)
            assertEquals("wolf", it.shape)
        }
        all.filter { it.kind == CharacterKind.PERSON }.forEach {
            assertEquals("person", it.shape)
            assertTrue(it.nickname in FakeEncounterSource.NICKNAMES)
        }
        // 사람 외모도 섞여 나온다.
        assertTrue(all.filter { it.kind == CharacterKind.PERSON }.map { it.look }.toSet().size > 5)
    }

    @Test
    fun fetchNew_returnsZeroOrOne() = runTest {
        val src = FakeEncounterSource(Random(42))
        repeat(50) { assertTrue(src.fetchNew().size in 0..1) }
    }

    @Test
    fun pools_areBigEnoughForADemo() {
        // 시연 중 같은 이름이 자꾸 보이면 가짜 티가 난다.
        assertTrue(FakeEncounterSource.NICKNAMES.distinct().size >= 30)
        assertTrue(FakeEncounterSource.PET_NAMES.distinct().size >= 30)
        assertTrue(FakeEncounterSource.GREETINGS.distinct().size >= 20)
    }

    @Test
    fun generate_noRepeatedNamesInOneBatch() {
        val names = FakeEncounterSource(Random(3)).generate(20).map { it.nickname }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun fetchNew_noRepeatedNamesAcrossRecentTicks() = runTest {
        val source = FakeEncounterSource(Random(5))
        val names = mutableListOf<String>()
        repeat(60) { names += source.fetchNew().map { it.nickname } }
        // 최근 15명 안에서는 이름이 겹치지 않는다.
        names.windowed(15, partialWindows = true).forEach { w -> assertEquals(w.size, w.distinct().size) }
    }

    @Test
    fun eyeColor_addedWithoutChangingOtherValues() {
        // 눈 색을 더하기 전 코드로 뽑은 값(시드 42). 눈 색·몸 색은 다른 값에서 정하므로 나머지는 그대로여야 한다.
        // 몸 색은 색판 16색으로 바뀌었으므로 비교에서 뺀다(아래 bodies_comeFromPalette 테스트).
        fun noBody(s: String) = s.replace(Regex("""\|[A-Z_]+/([A-Z]+/[A-Z]+/[A-Z]+)$"""), "|$1")
        val before = listOf(
            "PERSON|person|느긋한거북|주말 잘 보내세요|MEDIUM/GRAY/GRAY|PURPLE/WINK/CROWN/BALLOON",
            "PERSON|person|콧노래운전|오늘 날씨 좋죠?|LIGHT/BLACK/DENIM|BLUE/SURPRISED/CAP/SCARF",
            "PERSON|person|느림보달팽이|어디 가는 길이에요?|DEEP/BLOND/DENIM|GREEN/WINK/CROWN/GLASSES",
            "PERSON|person|오늘도무사히|집에 잘 들어가세요|LIGHT/BLACK/BLACK|RED/WINK/CROWN/NONE",
            "PET|wolf|하루|다음에 또 스쳐요|LIGHT/BLACK/NAVY|RED/SLEEPY/BEANIE/NONE",
            "PERSON|person|졸음쉼터|주말 잘 보내세요|LIGHT/BROWN/NAVY|GREEN/SLEEPY/CROWN/BALLOON",
            "PET|dog|까미|반가워요!|LIGHT/BLACK/NAVY|YELLOW/SLEEPY/CROWN/GLASSES",
            "PERSON|person|출근길요정|오늘도 화이팅!|LIGHT/GRAY/BLACK|YELLOW/SURPRISED/CROWN/BALLOON",
            "PET|dog|솜이|행복한 여행 되세요|LIGHT/BLACK/NAVY|GREEN/SMILE/BEANIE/SCARF",
            "PERSON|person|휴게소미식가|어디 가는 길이에요?|DEEP/BLOND/BLACK|GREEN/SURPRISED/CAP/NONE",
        )
        val all = FakeEncounterSource(Random(42), animalShapes = listOf("wolf", "dog")).generate(10)
        val now = all.map { c ->
            "${c.kind}|${c.shape}|${c.nickname}|${c.greeting}|${c.look.skin}/${c.look.hair}/${c.look.bottom}|${c.parts.body}/${c.parts.face}/${c.parts.hat}/${c.parts.accessory}"
        }
        assertEquals(before.map(::noBody), now.map(::noBody))
        val eyes = FakeEncounterSource(Random(7)).generate(50).map { it.parts.eye }.toSet()
        assertTrue("눈 색 $eyes", eyes.size >= 2)
    }

    @Test
    fun bodies_comeFromPalette() {
        val bodies = FakeEncounterSource(Random(7)).generate(100).map { it.parts.body }
        assertTrue(bodies.all { it.palette != null })
        assertTrue("몸 색 ${bodies.toSet().size}가지", bodies.toSet().size >= 10)
    }
}
