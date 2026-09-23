# Play Console: Accessibility API disclosure for TelePort

Google rejected an update on 23 Jun 2026 for two Accessibility API policy issues:

1. **Missing prominent disclosure (in-app).** Fixed in code. The TV pairing screen now shows
   `AccessibilityDisclosureDialog` before it opens Accessibility settings. Settings open only after
   the user taps **Agree**. **Decline**, Back, or dismissing the dialog does nothing.
2. **Missing description in the Play listing.** Fix this in Play Console by hand, using the text below.

---

## 1. Paragraph to append to the Full description

Play Console > **Grow users > Store presence > Main store listing > Full description**. Paste this
at the end of the existing description (about 900 characters, within the 4000 limit):

```
ACCESSIBILITY SERVICE DISCLOSURE
TelePort includes an optional Accessibility Service called "TelePort Air Mouse" on Android TV. It uses the Android AccessibilityService API so the phone you pair with TelePort can act as a remote mouse and keyboard for your TV, including inside other apps. When you turn it on, it draws a mouse cursor on screen, performs taps, scrolls and the Back action where you point, and finds the focused text field so it can type the text you send from your phone. It only acts on commands from your paired phone. TelePort does not collect, store, log or share any on-screen content, typed text or activity in other apps, and none of it leaves your TV. The service is off by default. TelePort shows an in-app disclosure and asks for your consent before you enable it, and you can turn it off at any time in Settings > Accessibility. TelePort is not an accessibility tool for people with disabilities.
```

If you localise the listing, add the same paragraph (translated) to each localised full description.

---

## 2. Accessibility API declaration form answers

Play Console > **Policy and programs > App content > Accessibility API** (or the "Sensitive
permissions and APIs" declaration shown with the rejection).

| Question | Answer |
|---|---|
| Does your app use the AccessibilityService API? | **Yes** |
| Is your app an accessibility tool (`isAccessibilityTool="true"`), mainly built to help people with disabilities? | **No.** The manifest config sets `android:isAccessibilityTool="false"`. |
| Core functionality / feature that uses the API | Remote control of Android TV from a paired phone ("TelePort Air Mouse"). The phone sends cursor movement, tap, scroll, Back and text-input commands over the local network, and the service performs them on the TV so the user can control apps other than TelePort. |
| Why is the API needed? What is the alternative? | Android offers no other public API that lets an app inject taps and gestures, perform the global Back action, or enter text into other apps' fields. Without it, TelePort can only control content inside its own built-in browser. |
| Which accessibility capabilities are used? | `canPerformGestures` (tap and scroll gestures at the cursor), `performGlobalAction(GLOBAL_ACTION_BACK)`, `TYPE_ACCESSIBILITY_OVERLAY` window (draws the cursor), and finding the focused input node plus `ACTION_SET_TEXT` (types the text the user sent from their phone). `onAccessibilityEvent` is a no-op, so no events are captured. |
| Personal or sensitive user data accessed | Only the currently focused text-input field, and only when the user sends text from their paired phone. The service does not read, record or send screen content. |
| Is data collected (sent off device)? | **No** |
| Is data shared with third parties? | **No** |
| Is data stored or logged? | **No** |
| Prominent in-app disclosure | Yes. On the TV pairing screen, "Enable Global Air Mouse" opens a full dialog that lists what the service does and what data it touches, and states that nothing is collected or shared. Accessibility settings open only after the user taps **Agree**. **Decline** or Back closes the dialog without enabling anything. The service stays off until the user turns it on in system settings. |
| Is the use described in the Play listing? | Yes (the paragraph in section 1 above). |
| Video demonstration | Record on an Android TV or emulator: open TelePort TV > press **Enable Global Air Mouse** > show the disclosure dialog in full > press **Agree** > Settings > Accessibility > TelePort Air Mouse > turn it on > go back and move the cursor, tap and type from the paired phone in another app. Upload it as an unlisted YouTube link. |

### Privacy policy

The hosted privacy policy (`docs/privacy.html`, GitHub Pages) now has a
"5. Accessibility Service" section with the same statements: what is accessed, that it is used only
to perform the user's remote-control commands, and that nothing is collected or shared. Google
checks that the listing, the in-app disclosure and the privacy policy agree with each other.

---

## Source of truth in code

- Disclosure dialog: `app/src/main/java/com/teleport/app/tv/AccessibilityDisclosureDialog.kt`
- Disclosure strings: `app/src/main/res/values/strings.xml` (`accessibility_disclosure_*`)
- Service config: `app/src/main/res/xml/accessibility_service_config.xml`
  (`android:description`, `android:isAccessibilityTool="false"`)
- Service: `app/src/main/java/com/teleport/app/tv/server/TelePortAccessibilityService.kt`
