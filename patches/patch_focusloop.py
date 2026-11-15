from pathlib import Path

ROOT = Path("FocusLoop")

def replace_once(path: str, old: str, new: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one patch target in {path}; found {count}")
    p.write_text(text.replace(old, new), encoding="utf-8")

replace_once("app/src/main/java/com/focusloop/app/MainActivity.kt",
'''    private fun onResetClicked() {
        if (store.status == Status.IDLE) {
            store.saveIdle(Phase.FOCUS)
            refreshTimer()
        } else {
            TimerService.send(this, TimerService.ACTION_RESET)
        }
    }

    private fun onSkipClicked() {
        if (store.status == Status.IDLE) {
            val other = if (store.phase == Phase.FOCUS) Phase.REST else Phase.FOCUS
            store.saveIdle(other)
            refreshTimer()
        } else {
            TimerService.send(this, TimerService.ACTION_SKIP)
        }
    }
''',
'''    private fun onResetClicked() {
        if (store.status == Status.IDLE) {
            store.saveIdle(Phase.FOCUS)
            refreshTimer()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_title)
            .setMessage(R.string.reset_message)
            .setPositiveButton(R.string.btn_reset) { _, _ ->
                TimerService.send(this, TimerService.ACTION_RESET)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onSkipClicked() {
        if (store.status == Status.IDLE) {
            val other = if (store.phase == Phase.FOCUS) Phase.REST else Phase.FOCUS
            store.saveIdle(other)
            refreshTimer()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.skip_title)
            .setMessage(if (store.phase == Phase.FOCUS) R.string.skip_focus_message else R.string.skip_rest_message)
            .setPositiveButton(R.string.btn_skip) { _, _ ->
                TimerService.send(this, TimerService.ACTION_SKIP)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
''')

replace_once("app/src/main/java/com/focusloop/app/MainActivity.kt",
'        sessionInfo.text = getString(R.string.today_summary, todaySessions, todayMinutes)\n',
'''        sessionInfo.text = resources.getQuantityString(
            R.plurals.today_summary, todaySessions, todaySessions, todayMinutes
        )
''')
replace_once("app/src/main/java/com/focusloop/app/MainActivity.kt",
'            lengthText.text = getString(R.string.history_row_minutes, row.seconds / 60)\n',
'''            lengthText.text = getString(R.string.history_row_minutes, row.seconds / 60)
            lengthText.contentDescription = getString(R.string.history_row_accessibility, row.seconds / 60)
''')
replace_once("app/src/main/java/com/focusloop/app/MainActivity.kt",
'        store.reconcileAfterProcessDeath()\n',
'        store.reconcileAfterProcessDeath(this)\n')
replace_once("app/src/main/java/com/focusloop/app/Store.kt",
'import android.os.SystemClock\n',
'import android.os.SystemClock\nimport android.provider.Settings\n')
replace_once("app/src/main/java/com/focusloop/app/Store.kt",
'''    fun reconcileAfterProcessDeath() {
        if (status == Status.RUNNING && !TimerService.alive) {
            val remaining = endAt - SystemClock.elapsedRealtime()
            if (remaining > 0L && remaining <= totalMs + 1_000L) {
                saveState(phase, Status.PAUSED, 0L, remaining, totalMs)
            } else {
                saveIdle(Phase.FOCUS)
            }
        }
    }
''',
'''    fun reconcileAfterProcessDeath(context: Context) {
        val currentBootCount = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.getOrDefault(-1)
        val previousBootCount = sp.getInt(KEY_BOOT_COUNT, currentBootCount)
        val rebooted = currentBootCount >= 0 && previousBootCount >= 0 &&
            currentBootCount != previousBootCount
        if (currentBootCount >= 0) {
            sp.edit().putInt(KEY_BOOT_COUNT, currentBootCount).apply()
        }

        // elapsedRealtime restarts at boot. Never reuse a deadline from the previous boot.
        if (rebooted) {
            if (status != Status.IDLE) saveIdle(Phase.FOCUS)
            return
        }
        if (status == Status.RUNNING && !TimerService.alive) {
            val remaining = endAt - SystemClock.elapsedRealtime()
            if (remaining > 0L && remaining <= totalMs + 1_000L) {
                saveState(phase, Status.PAUSED, 0L, remaining, totalMs)
            } else {
                saveIdle(Phase.FOCUS)
            }
        }
    }
''')
replace_once("app/src/main/java/com/focusloop/app/Store.kt",
'        private const val KEY_TOTAL = "total_ms"\n',
'        private const val KEY_TOTAL = "total_ms"\n        private const val KEY_BOOT_COUNT = "boot_count"\n')
replace_once("app/src/main/res/values/strings.xml",
'    <string name="today_summary">Today: %1$d sessions · %2$d min</string>\n',
'''    <plurals name="today_summary">
        <item quantity="one">Today: %1$d session · %2$d min</item>
        <item quantity="other">Today: %1$d sessions · %2$d min</item>
    </plurals>
''')
replace_once("app/src/main/res/values/strings.xml",
'    <string name="history_row_minutes">%1$d min</string>\n',
'    <string name="history_row_minutes">%1$d min</string>\n    <string name="history_row_accessibility">Completed focus session, %1$d minutes</string>\n')
replace_once("app/src/main/res/values/strings.xml",
'    <string name="clear_title">Clear history?</string>\n',
'''    <string name="reset_title">Reset timer?</string>
    <string name="reset_message">The current phase will end and the timer will return to Focus ready state.</string>
    <string name="skip_title">Skip this phase?</string>
    <string name="skip_focus_message">The current focus phase will be skipped and the rest timer will start. This focus session will not be counted.</string>
    <string name="skip_rest_message">The current rest phase will end and a new focus phase will start.</string>
    <string name="clear_title">Clear history?</string>
''')
replace_once("app/src/main/res/values/strings.xml",
'    <string name="duration_hint">Tap − or + for 1 minute, press and hold for 5. Changes apply from the next session.</string>\n',
'    <string name="duration_hint">Tap − or + for 1 minute, press and hold for 5. Focus: 1–180 min; Rest: 1–60 min. Changes apply from the next session.</string>\n')

print("FocusLoop source patches applied successfully")
