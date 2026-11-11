package com.drivepark.app.ui.character

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.InMemoryParkRepository
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.SkinTone
import kotlinx.coroutines.flow.first
import com.drivepark.app.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateCharacterViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = InMemoryParkRepository()

    @Test
    fun save_rejectsInvalidInput_keepsError() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("   ")
        vm.setGreeting("안녕!")
        vm.save()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.saved)
        assertNull(repo.getMyCharacter())
    }

    @Test
    fun typing_stopsAtTheLimit() {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("Waveby3Hello") // 12자
        vm.setGreeting("가".repeat(40))
        assertEquals("Waveby3Hel", vm.state.value.nickname) // 닉네임은 10자에서 멈춘다
        assertEquals(30, vm.state.value.greeting.length)   // 인사말은 30자에서 멈춘다
    }

    @Test
    fun newUser_defaultsToPersonShape() {
        val vm = CreateCharacterViewModel(repo)
        assertEquals(CharacterKind.PERSON, vm.state.value.kind)
        assertEquals("person", vm.state.value.shape)
        assertEquals(listOf("person", "wolf"), vm.state.value.shapeChoices)
    }

    @Test
    fun pet_hidesPersonShape() {
        val vm = CreateCharacterViewModel(repo)
        vm.setKind(CharacterKind.PET)
        assertFalse("person" in vm.state.value.shapeChoices)
        assertEquals("wolf", vm.state.value.shape)
    }

    @Test
    fun switchingBackToPerson_restoresPersonShapeUnlessTouched() {
        val vm = CreateCharacterViewModel(repo)
        vm.setKind(CharacterKind.PET)
        vm.setKind(CharacterKind.PERSON)
        assertEquals("person", vm.state.value.shape)
        // 사람이 직접 늑대 모습을 골랐다면 "누구"를 바꿔도 그대로 둔다.
        vm.setShape("wolf")
        vm.setKind(CharacterKind.PET)
        vm.setKind(CharacterKind.PERSON)
        assertEquals("wolf", vm.state.value.shape)
    }

    @Test
    fun nameLabel_followsKind() {
        val vm = CreateCharacterViewModel(repo)
        assertEquals("닉네임", vm.state.value.nameLabel)
        vm.setKind(CharacterKind.PET)
        assertEquals("반려동물 이름", vm.state.value.nameLabel)
    }

    @Test
    fun newUser_eyeDefaults_personBlack_wolfGold() {
        val vm = CreateCharacterViewModel(repo)
        assertEquals(EyeColor.BLACK, vm.state.value.parts.eye)
        vm.setShape("wolf")
        assertEquals(EyeColor.GOLD, vm.state.value.parts.eye)
        vm.setShape("person")
        assertEquals(EyeColor.BLACK, vm.state.value.parts.eye)
        // 반려동물로 바꾸면 늑대 모습이 되면서 눈도 늑대 기본(금색).
        vm.setKind(CharacterKind.PET)
        assertEquals(EyeColor.GOLD, vm.state.value.parts.eye)
    }

    @Test
    fun newAnimals_eyeDefaults_dogBrown_catBlue() {
        // 모습마다 어울리는 눈 기본색(퍼스널 컬러): 강아지 갈색, 고양이 파랑.
        val vm = CreateCharacterViewModel(repo, animalShapes = listOf("wolf", "dog", "cat"))
        vm.setShape("dog")
        assertEquals(EyeColor.BROWN, vm.state.value.parts.eye)
        vm.setShape("cat")
        assertEquals(EyeColor.BLUE, vm.state.value.parts.eye)
        vm.setShape("wolf")
        assertEquals(EyeColor.GOLD, vm.state.value.parts.eye)
    }

    @Test
    fun pickedEye_survivesShapeChange() {
        val vm = CreateCharacterViewModel(repo)
        vm.setParts(vm.state.value.parts.copy(eye = EyeColor.BLUE))
        vm.setShape("wolf")
        assertEquals(EyeColor.BLUE, vm.state.value.parts.eye)
    }

    @Test
    fun existingCharacter_keepsItsEye() = runTest {
        // 저장된 캐릭터(예: 이호랑)의 눈 색은 기본값으로 바꾸지 않는다.
        repo.saveMyCharacter(
            Character(CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.SCARF, EyeColor.GREEN), "이호랑", "안녕", CharacterKind.PERSON, "wolf", PersonLook()),
        )
        val vm = CreateCharacterViewModel(repo)
        val s = vm.state.first { it.nickname == "이호랑" }
        assertEquals(EyeColor.GREEN, s.parts.eye)
    }

    @Test
    fun save_storesKindShapeLook() = runTest {
        val vm = CreateCharacterViewModel(repo)
        val look = PersonLook(SkinTone.MEDIUM, HairColor.GRAY, BottomColor.BEIGE)
        vm.setLook(look)
        vm.setNickname("나")
        vm.setGreeting("안녕")
        vm.save()
        val saved = repo.getMyCharacter()!!
        assertEquals(CharacterKind.PERSON, saved.kind)
        assertEquals("person", saved.shape)
        assertEquals(look, saved.look)
    }

    @Test
    fun existingCharacter_loadsKindShapeLook() = runTest {
        val look = PersonLook(SkinTone.DEEP, HairColor.BROWN, BottomColor.DENIM)
        repo.saveMyCharacter(
            Character(CharacterParts(BodyColor.RED, Face.SMILE, Hat.NONE, Accessory.NONE), "콩이", "멍", CharacterKind.PET, "wolf", look),
        )
        val vm = CreateCharacterViewModel(repo)
        val s = vm.state.first { it.nickname == "콩이" }
        assertEquals(CharacterKind.PET, s.kind)
        assertEquals("wolf", s.shape)
        assertEquals(look, s.look)
    }

    @Test
    fun save_validInput_persistsTrimmed() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname(" 뚜벅이 ")
        vm.setGreeting("안녕!")
        vm.save()
        assertTrue(vm.state.value.saved)
        assertEquals("뚜벅이", repo.getMyCharacter()?.nickname)
    }

    @Test
    fun existingCharacter_unknownShape_fallsBackByKind() = runTest {
        // 다른 버전에서 만든 모양 id(이 앱이 모르는 것)는 불러올 때 누구에 맞는 기본 모양으로 바꾼다.
        val parts = CharacterParts(BodyColor.RED, Face.SMILE, Hat.NONE, Accessory.NONE)
        repo.saveMyCharacter(Character(parts, "콩이", "멍", CharacterKind.PET, "dragon", PersonLook()))
        val pet = CreateCharacterViewModel(repo).state.first { it.nickname == "콩이" }
        assertEquals("wolf", pet.shape)

        repo.saveMyCharacter(Character(parts, "나", "안녕", CharacterKind.PERSON, "dragon", PersonLook()))
        val me = CreateCharacterViewModel(repo).state.first { it.nickname == "나" }
        assertEquals("person", me.shape)
    }

    @Test
    fun editsMadeWhileLoading_survive() = runTest {
        // 수정 화면이 저장된 캐릭터를 다 읽기 전에 누른 선택이 불러온 값에 덮이지 않아야 한다.
        val parts = CharacterParts(BodyColor.RED, Face.SMILE, Hat.NONE, Accessory.NONE)
        repo.saveMyCharacter(Character(parts, "나", "안녕", CharacterKind.PERSON, "person", PersonLook()))
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow = object : com.drivepark.app.domain.ParkRepository by repo {
            override suspend fun getMyCharacter(): Character? { gate.await(); return repo.getMyCharacter() }
        }
        val vm = CreateCharacterViewModel(slow)
        val picked = PersonLook(SkinTone.DEEP, HairColor.BROWN, BottomColor.DENIM)
        vm.setLook(picked)
        gate.complete(Unit)
        val s = vm.state.first { it.nickname == "나" }
        assertEquals(picked, s.look)      // 내가 고른 것은 남고
        assertEquals("안녕", s.greeting)  // 나머지는 불러온 값
    }

    @Test
    fun consumeSaved_resetsSoReturningToTheScreenDoesNotSkipIt() = runTest {
        // 초기 설정에서 저장 → 다음 단계 → "이전"으로 돌아왔을 때 저장 신호가 남아 있으면 곧바로 다시 넘어가 버린다.
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("나")
        vm.setGreeting("안녕")
        vm.save()
        assertTrue(vm.state.value.saved)
        vm.consumeSaved()
        assertFalse(vm.state.value.saved)
    }

    // ── 직접 그리기 ──

    private val art = List(23) { y -> if (y == 22) "...KK......KK...." else if (y == 10) "....KBBBBBK......" else ".".repeat(17) }
        .joinToString("\n")

    @Test
    fun finishDrawing_selectsDrawn() {
        val vm = CreateCharacterViewModel(repo)
        vm.startDrawing()
        vm.finishDrawing(art)
        val s = vm.state.value
        assertEquals(DRAWN_SHAPE, s.shape)
        assertEquals(art, s.drawing)
        assertTrue(s.shapeTouched)
        assertNull(s.board)
    }

    @Test
    fun drawnWithoutDrawing_saveBlockedWithError() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("나")
        vm.setGreeting("안녕")
        vm.setShape(DRAWN_SHAPE)
        vm.save()
        assertTrue(vm.state.value.error!!.contains("그림"))
        assertFalse(vm.state.value.saved)
        assertNull(repo.getMyCharacter())
    }

    @Test
    fun drawnWithDrawing_saves() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("나")
        vm.setGreeting("안녕")
        vm.finishDrawing(art)
        vm.save()
        assertEquals(DRAWN_SHAPE, repo.getMyCharacter()?.shape)
        assertEquals(art, repo.getMyCharacter()?.drawing)
    }

    @Test
    fun existingDrawing_loads() = runTest {
        repo.saveMyCharacter(Character(parts(), "나", "안녕", shape = DRAWN_SHAPE, drawing = art))
        val vm = CreateCharacterViewModel(repo)
        val s = vm.state.first { it.nickname == "나" }
        assertEquals(DRAWN_SHAPE, s.shape)
        assertEquals(art, s.drawing)
        assertEquals(art, vm.startDrawing().toText())
    }

    @Test
    fun drawnHidesPersonLookRows() {
        val vm = CreateCharacterViewModel(repo)
        vm.finishDrawing(art)
        assertFalse(vm.state.value.isPersonShape)
    }

    @Test
    fun drawn_inChoicesOnlyWhenDrawingExists() {
        val vm = CreateCharacterViewModel(repo)
        assertFalse(DRAWN_SHAPE in vm.state.value.shapeChoices)
        vm.finishDrawing(art)
        assertEquals(listOf("person", "wolf", DRAWN_SHAPE), vm.state.value.shapeChoices)
        vm.setKind(CharacterKind.PET)
        assertEquals(listOf("wolf", DRAWN_SHAPE), vm.state.value.shapeChoices)
        assertEquals(DRAWN_SHAPE, vm.state.value.shape) // 반려동물로 바꿔도 내 그림은 그대로
    }

    @Test
    fun startDrawing_fromWolf_convertsLetters() {
        val vm = CreateCharacterViewModel(repo)
        vm.setShape("wolf")
        val board = vm.startDrawing()
        assertEquals(com.drivepark.app.ui.pixel.Shapes.WOLF.rows.joinToString("\n"), board.toText())
        assertEquals(board, vm.state.value.board)
    }

    @Test
    fun startDrawing_fromPerson_keepsHairAndBottom() {
        // 이제 머리·하의 붓이 있으므로 사람 그림을 불러오면 머리(H)·하의(N)를 그대로 가져온다.
        val vm = CreateCharacterViewModel(repo)
        val text = vm.startDrawing().toText()
        assertTrue(text.contains('H'))
        assertTrue(text.contains('N'))
        assertNotNull(com.drivepark.app.domain.Drawing.parse(text))
    }

    @Test
    fun cancelDrawing_keepsOldDrawing() {
        val vm = CreateCharacterViewModel(repo)
        vm.finishDrawing(art)
        vm.updateBoard(vm.startDrawing().clear())
        vm.cancelDrawing()
        assertEquals(art, vm.state.value.drawing)
        assertNull(vm.state.value.board)
    }

    private fun parts() = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun finishDrawing_blankIsRejected() {
        // 빈 그림은 공원·주행 팝업에서 보이지 않는 캐릭터가 되므로 완성할 수 없다.
        val vm = CreateCharacterViewModel(repo)
        vm.startDrawing()
        vm.finishDrawing(List(23) { ".".repeat(17) }.joinToString("\n"))
        val s = vm.state.value
        assertEquals("person", s.shape)
        assertNull(s.drawing)
        assertNotNull(s.board) // 그리기 화면에 그대로 남는다
    }

    /** 0줄 첫 칸들에 [letters]를 넣은 그림(나머지는 테두리 한 칸). */
    private fun drawingWith(letters: String): String {
        val rows = List(23) { ".".repeat(17) }.toMutableList()
        rows[0] = letters + rows[0].drop(letters.length)
        rows[22] = "K" + rows[22].drop(1)
        return rows.joinToString("\n")
    }

    @Test
    fun personShape_showsHairAndBottom() {
        val vm = CreateCharacterViewModel(repo)
        vm.setShape("person")
        assertTrue(vm.state.value.showHairRow)
        assertTrue(vm.state.value.showBottomRow)
    }

    @Test
    fun drawnWithHair_showsHairOnly() {
        val vm = CreateCharacterViewModel(repo)
        vm.finishDrawing(drawingWith("H"))
        assertTrue(vm.state.value.showHairRow)
        assertFalse(vm.state.value.showBottomRow)
    }

    @Test
    fun drawnWithoutHN_hidesBoth() {
        val vm = CreateCharacterViewModel(repo)
        vm.finishDrawing(drawingWith("KB"))
        assertFalse(vm.state.value.showHairRow)
        assertFalse(vm.state.value.showBottomRow)
    }

    @Test
    fun wolf_hidesBoth() {
        val vm = CreateCharacterViewModel(repo)
        vm.setShape("wolf")
        assertFalse(vm.state.value.showHairRow)
        assertFalse(vm.state.value.showBottomRow)
    }

    @Test
    fun eyeColor_savedWithCharacter() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("나")
        vm.setGreeting("안녕")
        vm.setParts(vm.state.value.parts.copy(eye = com.drivepark.app.domain.EyeColor.PURPLE))
        vm.save()
        assertEquals(com.drivepark.app.domain.EyeColor.PURPLE, repo.getMyCharacter()!!.parts.eye)
    }

    @Test
    fun bodyChoices_wolfGrayThenPalette() {
        val vm = CreateCharacterViewModel(repo)
        assertEquals(com.drivepark.app.domain.BodyColor.CHOICES, vm.state.value.bodyChoices)
    }

    @Test
    fun bodyChoices_keepCurrentLegacyColor() {
        // 예전 5색(노을 등)을 쓰던 캐릭터는 그 색을 목록 끝에 남겨 그대로 고를 수 있게 한다.
        val vm = CreateCharacterViewModel(repo)
        vm.setParts(vm.state.value.parts.copy(body = com.drivepark.app.domain.BodyColor.RED))
        assertEquals(com.drivepark.app.domain.BodyColor.CHOICES + com.drivepark.app.domain.BodyColor.RED, vm.state.value.bodyChoices)
    }
}
