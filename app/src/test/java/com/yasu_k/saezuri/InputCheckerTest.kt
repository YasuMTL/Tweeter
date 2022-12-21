package com.yasu_k.saezuri

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

//@Config(maxSdk = 32)
//@RunWith(RobolectricTestRunner::class)
@RunWith(AndroidJUnit4::class)
class InputCheckerTest {
    lateinit var target: InputChecker

    @Before
    fun setUp() {
        target = InputChecker()
    }

    @After
    fun tearDown() {
    }

//    @Test
//    fun isValid() {
//        val actual = target.isValid("foo")
//        assertThat(actual, `is`(true))
//    }
//
//    @Test
//    fun isValid_givenLessThan3_returnsFalse() {
//        val actual = target.isValid("ab")
//        assertThat(actual, `is`(false))
//    }

//    @Test
//    fun isValid_givenAlphaNumeric_returnsTrue() {
//        val actual = target.isValid("abc123")
//        assertThat(actual, `is`(true))
//    }
//    @Test
//    fun isValid_givenAlphaNumeric_returnsTrue() {
//        val actual = target.isValid("abc123")
//        assertThat(actual).isTrue
//    }
//
//    @Test
//    fun checkTokyo(){
//        SoftAssertions().apply {
//            assertThat("TOKYO")
//                .`as`("TEXT CHECK TOKYO")
//                .isEqualTo("HOKKAIDO")
//                .isEqualToIgnoringCase("tokyo")
//                .isNotEqualTo("KYOTO")
//                .isNotBlank()
//                .startsWith("TO")
//                .endsWith("YO")
//                .contains("OKY")
//                .matches("[A-Z]{5}")
//                .isInstanceOf(String::class.java)
//        }.assertAll()
//    }
//
//    @Test
//    fun checkList(){
//        val target = listOf("Giants", "Dodgers", "Athletics")
//
//        assertThat(target)
//            .hasSize(3)
//            .contains("Dodgers")
//            .containsOnly("Athletics", "Dodgers", "Giants")
//            .containsExactly("Giants", "Dodgers", "Athletics")
//            .doesNotContain("Padres")
//    }
//
//    @Test
//    fun checkBallTeam(){
//        data class BallTeam(val name: String, val city: String, val stadium: String)
//        val target = listOf(
//            BallTeam("Giants", "San Francisco", "AT&T Park"),
//            BallTeam("Dodgers", "Los Angels", "Dodger Stadium"),
//            BallTeam("Angels", "Los Angels", "Angel Stadium"),
//            BallTeam("Athletics", "Oakland", "Oakland Coliseum"),
//            BallTeam("Padres", "San Diego", "Petco Park")
//        )
//
//        assertThat(target)
//            .filteredOn { team -> team.city.startsWith("San") }
//            .filteredOn { team -> team.city.endsWith("Francisco") }
//            .extracting("name", String::class.java)
//            .containsExactly("Giants")
//
//        assertThat(target)
//            .filteredOn { team -> team.city == "Los Angels" }
//            .extracting("name", "stadium")
//            .containsExactly(
//                tuple("Dodgers", "Dodger Stadium"),
//                tuple("Angels", "Angel Stadium")
//            )
//    }
//
//    fun functionMayThrow(){
//        throw RuntimeException()
//    }

//    @Test
//    fun checkException(){
//        assertThatExceptionOfType(RuntimeException::class.java)
//            .isThrownBy { functionMayThrow() }
//            .withMessage("Aborted!")
//            .withNoCause()
//    }
//
//    @Test
//    fun isValid_givenInvalidCharacter_returnsFalse() {
//        val actual = target.isValid("abc@123")
//        assertThat(actual, `is`(false))
//    }

    @Test
    fun isValid_givenBlank_throwsIllegalArgumentException(){
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { target.isValid("") }
            .withMessage("Cannot be blank")
    }

//    @Test(expected = IllegalArgumentException::class)
//    fun isValid_givenNull_throwsIllegalArgumentException()
//    {
//        target.isValid(null)
//    }
//
//    @Ignore("Skip temporarily because the module to be tested is implemented incompletly")
//    @Test
//    fun temporarilySkipThisTest(){
//
//    }
}