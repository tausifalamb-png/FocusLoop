from pathlib import Path

ROOT = Path("FocusLoop")

def replace_once(path: str, old: str, new: str) -> None:
    p = ROOT / path
    source = p.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one patch target in {path}; found {count}")
    p.write_text(source.replace(old, new), encoding="utf-8")

replace_once("app/build.gradle.kts",
'''    implementation("androidx.appcompat:appcompat:1.7.0")
''',
'''    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
''')

replace_once("app/src/main/java/com/focusloop/app/SessionDb.kt",
'''    SQLiteOpenHelper(context.applicationContext, "focusloop.db", null, 1) {
''',
'''    SQLiteOpenHelper(context.applicationContext, "focusloop.db", null, 2) {
''')
replace_once("app/src/main/java/com/focusloop/app/SessionDb.kt",
'''                "end_ms INTEGER NOT NULL, " +
                "seconds INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_sessions_end ON sessions(end_ms)")
''',
'''                "end_ms INTEGER NOT NULL, " +
                "seconds INTEGER NOT NULL, " +
                "session_key TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_sessions_end ON sessions(end_ms)")
        db.execSQL("CREATE UNIQUE INDEX idx_sessions_key ON sessions(session_key)")
''')
replace_once("app/src/main/java/com/focusloop/app/SessionDb.kt",
'''    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 only. Future migrations go here.
    }

    @Synchronized
    fun addSession(endMs: Long, seconds: Int) {
        val values = ContentValues()
        values.put("end_ms", endMs)
        values.put("seconds", seconds)
        writableDatabase.insert("sessions", null, values)
    }
''',
'''    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Preserve every v1 row and assign it a stable legacy key before enforcing uniqueness.
            db.execSQL("ALTER TABLE sessions ADD COLUMN session_key TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE sessions SET session_key = 'legacy-' || id WHERE session_key = ''")
            db.execSQL("CREATE UNIQUE INDEX idx_sessions_key ON sessions(session_key)")
        }
    }

    /** Returns true only for the first insert of a completed timer phase. */
    @Synchronized
    fun addSession(sessionKey: String, endMs: Long, seconds: Int): Boolean {
        val values = ContentValues().apply {
            put("end_ms", endMs)
            put("seconds", seconds)
            put("session_key", sessionKey)
        }
        return writableDatabase.insertWithOnConflict(
            "sessions", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }
''')

replace_once("app/src/main/java/com/focusloop/app/TimerService.kt",
'''            SessionDb.get(this).addSession(wallEnd, (endedTotal / 1000L).toInt())
''',
'''            SessionDb.get(this).addSession(endedPhase.name + ":" + endedAt, wallEnd, (endedTotal / 1000L).toInt())
''')
replace_once("app/src/main/java/com/focusloop/app/TimerService.kt",
'''        val remaining = (store.endAt - now).coerceAtLeast(0L)
''',
'''        val remaining = TimerMath.remainingMs(store.endAt, now)
''')
replace_once("app/src/main/java/com/focusloop/app/TimerService.kt",
'''        var nextEnd = endedAt + nextTotal // chain from the true end time so nothing drifts
        if (nextEnd <= now) nextEnd = now + nextTotal // badly overdue: start the next phase fresh
''',
'''        val nextEnd = TimerMath.nextPhaseEndMs(endedAt, nextTotal, now)
''')

replace_once("app/src/main/java/com/focusloop/app/Store.kt",
'''        Status.RUNNING -> (endAt - now).coerceAtLeast(0L)
''',
'''        Status.RUNNING -> TimerMath.remainingMs(endAt, now)
''')
replace_once("app/src/main/java/com/focusloop/app/Store.kt",
'''            val remaining = endAt - SystemClock.elapsedRealtime()
            if (remaining > 0L && remaining <= totalMs + 1_000L) {
                saveState(phase, Status.PAUSED, 0L, remaining, totalMs)
            } else {
                saveIdle(Phase.FOCUS)
            }
''',
'''            val remaining = TimerMath.recoverableRemainingMs(
                endAt, SystemClock.elapsedRealtime(), totalMs
            )
            if (remaining != null) saveState(phase, Status.PAUSED, 0L, remaining, totalMs)
            else saveIdle(Phase.FOCUS)
''')

timer_math = """package com.focusloop.app

/** Pure timer arithmetic kept separate so deadline and recovery rules can be unit-tested. */
internal object TimerMath {
    fun remainingMs(endAtElapsedMs: Long, nowElapsedMs: Long): Long =
        (endAtElapsedMs - nowElapsedMs).coerceAtLeast(0L)

    fun recoverableRemainingMs(
        endAtElapsedMs: Long,
        nowElapsedMs: Long,
        totalDurationMs: Long
    ): Long? {
        if (totalDurationMs <= 0L) return null
        val remaining = endAtElapsedMs - nowElapsedMs
        return remaining.takeIf { it > 0L && it <= totalDurationMs + 1_000L }
    }

    /** Carry a phase deadline forward to avoid drift; never create an already-expired next phase. */
    fun nextPhaseEndMs(previousEndElapsedMs: Long, nextDurationMs: Long, nowElapsedMs: Long): Long {
        require(nextDurationMs > 0L) { "nextDurationMs must be positive" }
        val chained = previousEndElapsedMs + nextDurationMs
        return if (chained <= nowElapsedMs) nowElapsedMs + nextDurationMs else chained
    }
}
"""
math_path = ROOT / "app/src/main/java/com/focusloop/app/TimerMath.kt"
math_path.parent.mkdir(parents=True, exist_ok=True)
math_path.write_text(timer_math, encoding="utf-8")

test_source = """package com.focusloop.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerMathTest {
    @Test fun remainingTimeUsesDeadlineNotTickCount() {
        assertEquals(12_345L, TimerMath.remainingMs(112_345L, 100_000L))
    }

    @Test fun remainingTimeNeverBecomesNegative() {
        assertEquals(0L, TimerMath.remainingMs(99L, 100L))
    }

    @Test fun processRecoveryAcceptsValidRemainingTime() {
        assertEquals(30_000L, TimerMath.recoverableRemainingMs(130_000L, 100_000L, 1_500_000L))
    }

    @Test fun processRecoveryRejectsExpiredOrImpossibleDeadline() {
        assertNull(TimerMath.recoverableRemainingMs(90_000L, 100_000L, 1_500_000L))
        assertNull(TimerMath.recoverableRemainingMs(2_000_000L, 100_000L, 1_500_000L))
        assertNull(TimerMath.recoverableRemainingMs(130_000L, 100_000L, 0L))
    }

    @Test fun chainedPhaseDoesNotDriftWhenCallbackIsOnTime() {
        assertEquals(160_000L, TimerMath.nextPhaseEndMs(100_000L, 60_000L, 110_000L))
    }

    @Test fun overduePhaseStartsNextPhaseFromNow() {
        assertEquals(230_000L, TimerMath.nextPhaseEndMs(100_000L, 60_000L, 170_000L))
    }
}
"""
test_path = ROOT / "app/src/test/java/com/focusloop/app/TimerMathTest.kt"
test_path.parent.mkdir(parents=True, exist_ok=True)
test_path.write_text(test_source, encoding="utf-8")

print("P0 timer hardening and unit tests applied")
