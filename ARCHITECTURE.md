# No Snooze Alarm — Architecture

Offline-first Android alarm with mandatory wake-up missions.
Kotlin, Jetpack Compose, minSdk 26, targetSdk 34. No backend, no account, no network permission.

## Key Android decisions

|Concern|Decision|Why|
|-|-|-|
|Scheduling|`AlarmManager.setAlarmClock()`|Exempt from Doze, shown as a system alarm, and does not need the `SCHEDULE\\\_EXACT\\\_ALARM` permission.|
|Ringing|Foreground service (`mediaPlayback` type) owns audio and vibration|Audio survives the UI being backgrounded, swiped away, or the app being killed.|
|Screen takeover|Full-screen intent notification + `setShowWhenLocked` / `setTurnScreenOn`|The legitimate way to wake the screen and show over the lock screen.|
|Reboot|`BOOT\\\_COMPLETED` receiver reschedules every enabled alarm|AlarmManager alarms are lost on reboot.|
|Storage|One JSON file in app-private storage (kotlinx.serialization)|Simple, no Room/KSP build complexity, trivially inspectable.|
|Locking|Enforced in `AlarmRepository.update/delete`, not in the UI|The UI only reflects the rule.|
|Audio stop|Service stops sound on a `MISSION\\\_STARTED` action|The first interaction in the first mission stops the sound.|
|Steps|`TYPE\\\_STEP\\\_COUNTER` + `ACTIVITY\\\_RECOGNITION`|Hardware step counting. A fallback message appears when unavailable.|
|Shake|`TYPE\\\_ACCELEROMETER` magnitude threshold|No permission needed.|

## Permissions (and why)

* `POST\\\_NOTIFICATIONS` — alarm notification and bedtime reminders (Android 13+)
* `USE\\\_FULL\\\_SCREEN\\\_INTENT` — show the alarm over the lock screen
* `FOREGROUND\\\_SERVICE`, `FOREGROUND\\\_SERVICE\\\_MEDIA\\\_PLAYBACK` — keep ringing
* `WAKE\\\_LOCK` — keep the CPU on while ringing
* `VIBRATE` — optional vibration
* `RECEIVE\\\_BOOT\\\_COMPLETED` — reschedule after reboot
* `ACTIVITY\\\_RECOGNITION` — step mission only, requested only when needed
* Camera: handled through the system camera app via `ACTION\\\_IMAGE\\\_CAPTURE`, so no `CAMERA` permission is declared.

## Packages

```
com.wake.alarm
├── data/       Alarm model, settings, JSON repository, history
├── schedule/   AlarmScheduler, BootReceiver, AlarmReceiver, BedtimeScheduler
├── ring/       RingService, RingActivity, AudioPlayer
├── mission/    Mission interface + one file per mission, generators
├── ui/         Compose theme and screens
└── util/       Time helpers
```

## Adding a mission later

Implement `Mission` in `mission/`, add a `MissionType` entry, add its config fields to `MissionConfig`, and register it in `MissionFactory`.

## Assets

```
app/src/main/assets/
  audio/{classic,funny,dramatic,weird,calm}/\\\*.mp3
  backgrounds/\\\*.jpg
  logic\\\_questions.json
```

Drop files in and they are discovered at runtime by listing the folders. No code change is needed.

