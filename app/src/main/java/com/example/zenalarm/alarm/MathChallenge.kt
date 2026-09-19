package com.example.zenalarm.alarm

import com.example.zenalarm.data.AlarmGroup
import kotlin.random.Random

data class MathChallenge(
    val question: String,
    val answer: Int,
)

object MathChallengeGenerator {
    fun generate(difficulty: Int = AlarmGroup.DEFAULT_CHALLENGE_DIFFICULTY): MathChallenge {
        val level = difficulty.coerceIn(
            AlarmGroup.MIN_CHALLENGE_DIFFICULTY,
            AlarmGroup.MAX_CHALLENGE_DIFFICULTY,
        )

        return when (level) {
            1 -> {
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(2, 10)
                MathChallenge("$a + $b", a + b)
            }
            2 -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(2, 10)
                MathChallenge("$a + $b", a + b)
            }
            3 -> {
                val a = Random.nextInt(12, 99)
                val b = Random.nextInt(12, 99)
                MathChallenge("$a + $b", a + b)
            }
            4 -> {
                val a = Random.nextInt(40, 99)
                val b = Random.nextInt(12, 39)
                MathChallenge("$a - $b", a - b)
            }
            else -> {
                val a = Random.nextInt(6, 16)
                val b = Random.nextInt(3, 10)
                MathChallenge("$a x $b", a * b)
            }
        }
    }

    fun difficultyLabel(difficulty: Int): String {
        return when (difficulty.coerceIn(1, 5)) {
            1 -> "Easy"
            2 -> "Light"
            3 -> "Normal"
            4 -> "Hard"
            else -> "Expert"
        }
    }
}
