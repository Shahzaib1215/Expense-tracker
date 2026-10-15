# Kharcha Tracker

A simple personal expense tracker for Pakistan. It automatically records money
sent and received by reading **bank SMS alerts** and **banking app
notifications**, and it also lets you add cash/manual entries by hand.

Everything is stored in a local Room database on the phone. **No account, no
server, no internet needed — your data never leaves your device.**

## What it does

- **Dashboard** — Total Received (green), Total Sent (red), and Balance cards,
  plus a list of recent transactions with the bank name and date.
- **Auto-detect from SMS** — a background receiver reads incoming texts,
  recognises Pakistani bank alert formats (`Rs. 5,000`, `PKR 12,500.00`, …),
  works out whether money was sent or received from words like
  *debited / credited / transferred / received*, detects the bank
  (Meezan, UBL, HBL, MCB, Allied, Alfalah, Faysal, Mashreq, Easypaisa,
  JazzCash, NayaPay, SadaPay), and saves the transaction. Duplicate
  protection included.
- **Auto-detect from app notifications** — a notification-listener service
  watches alerts from the banking apps above and records them the same way
  (skips repeats within 60 seconds).
- **Manual entries** — floating + button, amount + Sent/Received toggle + title.
- **Clear history** — trash icon in the top bar (with confirmation).

## Open it in Android Studio

1. Open Android Studio → **Open** → select the `expense-tracker-app` folder.
2. Let Gradle sync (needs internet once, to download dependencies).
3. Run on a physical phone (SMS + notifications don't work well on emulators).

Minimum: Android 8.0 (API 26). Built with Kotlin + Jetpack Compose (Material3).

## Paste it into Google AI Studio ("Build an Android app")

Copy the files keeping the folder structure, or zip the whole
`expense-tracker-app` folder and attach it, then prompt something like:

> "This is a complete Android Studio project (Kotlin + Jetpack Compose).
> Build it as an Android app I can preview and install privately."

AI Studio understands standard Gradle projects, so it should pick up the
structure directly.

## Permissions — what to grant and why

| Permission | Why |
|---|---|
| **Receive / read SMS** | To detect bank transaction texts automatically. Only messages containing an amount + sent/received keywords are saved; everything else is ignored. |
| **Notifications (Android 13+)** | System requirement for showing any notification; the app barely uses it. |
| **Notification access** (special access, not a normal permission) | To read transaction alerts posted by banking apps. You enable it once in **Settings → Notifications → Notification access** — the app's dashboard shows a button that opens that screen. |

If you deny SMS permission, the app still works fully for manual entries.

## Honest technical note

Android sandboxes every app: **no app can open your banking app and read its
balance directly** — not this one, not any tracker. That is why Kharcha
Tracker uses the two channels banks actually push to you:

1. **Transaction SMS alerts** — Pakistani banks send one for almost every
   debit/credit, and the parser is tuned for their formats.
2. **Banking app notifications** — for banks whose SMS is delayed or missing.

Between the two, nearly all day-to-day transactions get captured. Anything
missed (cash, etc.) takes five seconds to add manually.

## Privacy

- All parsing happens on-device, in `TransactionParser.kt` — you can read the
  code, there is no network call anywhere in the app.
- The database (`kharcha_tracker.db`) lives in the app's private storage;
  uninstalling the app deletes it.
- Grant the SMS / notification permissions knowing they are sensitive: this
  app only uses them for the transaction detection described above.

## Project structure

```
expense-tracker-app/
├── settings.gradle / build.gradle      # project-level Gradle config
├── README.md
└── app/
    ├── build.gradle                    # Room, Compose BOM, Navigation, KSP
    └── src/main/
        ├── AndroidManifest.xml         # permissions, SmsReceiver, listener service
        ├── java/com/shahzaib/kharchatracker/
        │   ├── MainActivity.kt         # permissions + navigation
        │   ├── data/                  # Transaction, DAO, Room database
        │   ├── parser/                # SMS/notification text parser
        │   ├── sms/                   # SMS broadcast receiver
        │   ├── notifications/         # Notification listener service
        │   ├── viewmodel/             # UI state (totals, list, actions)
        │   └── ui/                    # Dashboard, Add form, Material3 theme
        └── res/values/                # app name, activity theme
```

## One more note

SMS-related permissions (`RECEIVE_SMS`, `READ_SMS`) are restricted by Google
Play policy: publishing this exact app on the Play Store would require a
core-functionality declaration and review. It is designed for **personal use**
— sideload the APK or install it privately through Google Play's personal-app
flow instead.
