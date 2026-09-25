# 0001 In-app review prompt at the end of a working remote session

Status: accepted   Date: 2026-09-25   Issue: POR-118

## Context

The TelePort Android app (`com.carfry369.teleport`) is live on Google Play at v1.4.0 with **50+
downloads and 0 ratings** (`metrics/latest.md`, 2026-09-24), and has been live well over 30 days —
stage 5 ("Live: adoption") of `sops/product-lifecycle.md`. The product goal is 50+ → 500+ downloads
by 2026-12-31, and a listing with no star rating converts worse than one with any rating at all.
Nobody has ever been asked to rate the app: there is **no rating or review code in the repo today**
(checked before designing — no `com.google.android.play:review` in `app/build.gradle.kts`, no
`ReviewManager`/`ReviewInfo`/"rate" reference in `app/src`, no rating string in
`app/src/main/res/values/`).

This is the same gap already decided for InvTrack (POR-91, `docs/adr/0001-in-app-review-prompt.md`
in that repo) and EchoKit (POR-90). That ADR is an accepted precedent for the *mechanism*; what
needs deciding here is the **trigger**, because TelePort's shape is different: one APK that runs both
as an Android TV browser and as a phone remote, with no data model and no account.

Constraints that shape the design:

- Play policy (`developer.android.com/guide/playcore/in-app-review`): the app must not ask the user
  any question before or while the card is shown, must not add custom rating UI or overlays, must
  not remove the card programmatically, and must not put the request behind a call-to-action button.
  The request is quota-limited and returns **no signal** about whether the card was actually shown.
- Device support, quoted from the same page: *"Android devices (phones, tablets, and TVs with Google
  TV) running Android 5.0 (API level 21) or higher that have the Google Play Store installed"* plus
  ChromeOS. So plain Android TV OS (non–Google TV) is **not** covered.
- The phone remote is used while the user is looking at the television, often mid-video. An
  interruption during a session is worse here than in a normal touch app.
- `minSdk = 26`, so the API level floor is already satisfied everywhere.

What the repo already provides and this design reuses:

- Play Core precedent: `com.google.android.play:app-update` + `app-update-ktx` 2.1.0, driven from
  `MainActivity.checkPlayStoreUpdates()` / `onResume()` (non-blocking, failures only logged).
  `com.google.android.play:review` is a sibling artifact under the same Play Core SDK terms.
- Local persistence: the `teleport_prefs` `SharedPreferences` file, already used for
  `has_seen_onboarding` (`MobileRemoteScreen.kt:121`). No database, no Firebase read/write.
- A real session lifecycle to hang the trigger on: `TvConnectionManager.connectionState`
  (`Disconnected`/`Connecting`/`Connected`/`Error`), `TvConnectionManager.tvState` fed by the TV's
  `TabManager.syncState()` broadcast, an explicit **Disconnect** button
  (`MobileRemoteScreen.kt:543`), and cast commands `Command.OpenUrl` / `Command.PlayStreamNatively`.
- `MainActivity` already owns the mobile-vs-TV branch (`checkIsTvDevice()`), so a phone-only feature
  has an obvious home and cannot leak onto the TV build path.
- Unit test setup: JUnit 4 + Robolectric 4.11.1 in `app/src/test`.

## Decision

Add the **Play in-app review API** (`com.google.android.play:review` + `review-ktx`), requested **at
most once per install**, **on the phone remote only**, fired **when a session that demonstrably
worked ends**. All gating state is local `SharedPreferences`. No custom prompt UI, no new string, no
new permission, no analytics event, no protocol change.

A "session that demonstrably worked" means, within one `Connected` period:

1. the user sent at least one cast command (`Command.OpenUrl` or `Command.PlayStreamNatively`), and
2. at least one `TvState` was received from the TV *after* that first cast — the TV echoing its tab
   state is proof the link is actually working end to end, not just a socket that opened, and
3. the connection stayed up for at least 30 seconds.

Shape:

1. `app/src/main/java/com/teleport/app/review/ReviewPromptManager.kt` — holds `SharedPreferences`,
   a clock lambda, and a narrow `ReviewLauncher` interface (`suspend fun launch(activity: Activity):
   Boolean`) so tests inject a fake instead of the Play library. Real implementation
   `PlayReviewLauncher` wraps `ReviewManagerFactory.create(...)` + `requestReview()` +
   `launchReviewFlow(...)`. Public surface: `onSessionStarted()`, `onCastSent()`,
   `onTvStateReceived()`, `suspend fun onSessionEnded(activity: Activity)`.
2. Keys in `teleport_prefs`, versioned so a later policy change starts clean:
   `review_prompt_v1_requested_at` (epoch ms; absent = never asked).
3. Gate order, cheapest first, all local: phone (not `checkIsTvDevice()`) → not already requested →
   session had a cast → a `TvState` arrived after that cast → connected ≥ 30 s → no in-app update
   flow in progress → activity lifecycle is `RESUMED` → `requestReview()` returned a `ReviewInfo` →
   **persist `requested_at`** → `launchReviewFlow`.
4. `requested_at` is written only immediately before `launchReviewFlow`, and never cleared: one shot
   per install. The API is quota-limited and reports nothing, so retrying on "failure" risks
   interrupting the user twice for nothing. If a gate suppresses the prompt (not resumed, update
   flow pending, `requestReview()` failed), the one shot is **not** spent.
5. Call sites, in the mobile branch of `MainActivity.setContent` only:
   - a `LaunchedEffect(connState)` that calls `onSessionStarted()` on entering `Connected` and
     `onSessionEnded(this@MainActivity)` on leaving it;
   - a `LaunchedEffect` collecting `connectionManager.tvState` → `onTvStateReceived()`;
   - cast counting inside `TvConnectionManager.sendCommand` via a small `onCastSent` callback (or an
     exposed per-session counter), reset in `connect()`, so every cast path is covered — the
     Disconnect button, the share-intent auto-cast (`MainActivity:265`), the URL bar and the native
     stream button — without touching 30 call sites.
   Everything is fire-and-forget and wrapped so no failure can reach the remote-control path.

Session end, not mid-session, is the moment: the user has stopped driving the television and the app
is showing the pairing screen again, so the card lands on an idle screen instead of over a live
remote. It is also when the user knows whether the thing worked.

## Rejected

- **Prompt on the TV build.** Google only supports in-app review on "TVs with Google TV", so on a
  large share of Android TV OS devices the call is a no-op that silently burns the one shot; and the
  TV is a shared-room, D-pad surface where a rating card over a video is hostile. Phone-only. Left
  open for a later ADR if Play widens support.
- **Prompt on first successful cast, while still connected (mid-session).** Highest reach, worst
  timing: the card would cover the remote while the user is aiming at a playing video, and the TV
  would keep playing behind it. Directly invites the 1-star review we are trying to avoid.
- **Prompt on app launch, or after N launches.** Cheap to build and what most apps do; it is also
  what produces "pushy app" ratings, and it carries no evidence the app ever worked for that user.
  A TelePort install that never paired should never be asked.
- **Prompt on successful pairing (`Connected`).** Tempting because it is the one unambiguous event,
  but pairing is the *start* of the job, not the payoff — plus when advertising is re-enabled
  (`FeatureFlags.ENABLE_ADVERTISEMENTS`, currently `false`) an interstitial already fires on
  `Connected` (`MainActivity:191`), so the card would queue behind an ad.
- **Custom "Enjoying TelePort?" dialog first, Play card only on yes.** The common pre-prompt pattern
  and explicitly against Play's in-app review policy. Also adds strings and a second interruption.
- **`Intent` / deep link to the Play listing's review page.** No new dependency and works on plain
  Android TV OS, but it throws the user out of the app and converts far worse. Kept only as the
  fallback if Play ever withdraws the in-app API.
- **Requiring 2+ successful sessions before asking.** Safer against asking a lukewarm user, but at
  50+ installs it would keep the rating count at or near 0 — the exact problem being fixed. One
  session with a confirmed, ≥30 s cast is enough evidence.
- **A `review_prompt_requested` analytics event.** InvTrack added one, but TelePort has the Firebase
  Analytics dependency and **zero** `logEvent` call sites today; this change is not the place to open
  the app's first analytics write and the privacy-disclosure question that comes with it. The Play
  Console rating count is the metric that matters.
- **Remote-config gating or a kill switch.** Blast radius without one is a single card per install.
  Not worth a new remote dependency; `FeatureFlags` already provides a compile-time off switch
  pattern if we ever need it.
- **A "Rate TelePort" button in the remote's settings/help sheet.** Play explicitly says not to put
  the API behind a call-to-action, because a quota-exhausted user gets a dead button.

## Consequences

- Risk: a user whose cast worked still rates 1 star. Accepted — 0 ratings is worse than a real
  distribution, and Play's own card is the lowest-friction ask available.
- Risk: collision with the in-app update flow (`AppUpdateType.IMMEDIATE`/`FLEXIBLE` can start in
  `onCreate` or `onResume`). Mitigated by the update-in-progress gate; when it suppresses, the one
  shot is not spent.
- Risk: a new Play Core dependency (`com.google.android.play:review` 2.0.2 + `review-ktx`). Same
  publisher and same SDK terms as the `app-update` artifacts the app already ships, so no new
  licence category and no board gate for the dependency itself. The dev issue must prove Gradle
  still resolves and `:app:assembleRelease` still builds.
- We cannot measure whether the card was shown. The success signal is the Play listing rating count
  moving off 0 in `metrics/latest.md` within a few weeks of the next release.
- The flow is untestable end-to-end on a debug build (the Play API no-ops outside a Play-installed
  build), so QA verifies the gating logic plus that pairing, casting, air-mouse and mirroring are
  unchanged; the card itself is verified on an internal-track install or accepted as Play behaviour.
- The prompt only ships to users once a release goes out. Version bump and publishing are board-only
  (`company.md`), so this ADR's PRs must **not** bump `versionCode`/`versionName`.

## Build plan

One PR, ~250 lines including tests.

1. Add `com.google.android.play:review:2.0.2` and `review-ktx:2.0.2` to `app/build.gradle.kts`, next
   to the existing `app-update` lines.
2. `ReviewPromptManager` + `ReviewLauncher` + `PlayReviewLauncher` in
   `app/src/main/java/com/teleport/app/review/`, error handling mirroring `checkPlayStoreUpdates()`.
3. Per-session cast counting in `TvConnectionManager` (increment on `OpenUrl` /
   `PlayStreamNatively`, reset in `connect()`), and an in-app-update-in-progress flag in
   `MainActivity` that the gate can read.
4. Wire the call sites in the mobile branch of `MainActivity.setContent`; the TV branch is untouched.
5. Unit tests (JUnit 4 + Robolectric, fake `ReviewLauncher`, in-memory prefs): a qualifying session
   asks exactly once; a second qualifying session does not ask again; a session with no cast never
   asks; a cast with no `TvState` afterwards never asks; a < 30 s session never asks; a suppressed
   gate does not spend the one shot; a throwing launcher cannot break disconnect.
6. Verify with `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleRelease`.

Out of scope: version bump, any CI/CD or `release.yaml` change, the TV surface, the Chrome extension
(POR-94), new strings, and any settings-screen entry point.
