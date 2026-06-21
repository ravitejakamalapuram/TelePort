# 📝 Google Play Console Manual Setup Required

## ⚠️ Required Manual Actions

The CD (Continuous Deployment) workflow uploads the app bundle to Google Play successfully, but **Google Play requires a manual accessibility declaration** in the Play Console before the release can be completed.

---

## 🔧 Accessibility Service Declaration

### **Why It's Required:**

TelePort uses Android's `AccessibilityService` API to enable air mouse functionality (gesture injection and window detection). Google Play requires all apps using this API to complete a declaration form in the Play Console.

### **How to Complete the Declaration:**

1. **Go to Play Console:**
   - Navigate to: https://play.google.com/console
   - Select the TelePort app

2. **Navigate to App Content:**
   - In the left sidebar: **Policy** → **App content**
   - Scroll to: **Accessibility** section

3. **Complete the Accessibility Declaration Form:**
   
   **Question 1:** *Does your app use AccessibilityService APIs?*
   - ✅ **Answer:** Yes
   
   **Question 2:** *What is the primary use case for AccessibilityService in your app?*
   - ✅ **Answer:** Select "**Remote control**" or "**Other**" (not accessibility tool)
   
   **Question 3:** *Describe how your app uses accessibility features:*
   - ✅ **Answer:**
     ```
     TelePort uses AccessibilityService API to enable remote control functionality:
     
     1. **Gesture Injection (canPerformGestures):** 
        Allows the app to inject touch, swipe, and scroll gestures on Android TV 
        and other devices when controlled remotely from a mobile device. This 
        enables users to control their TV using their phone as an air mouse.
     
     2. **Window Content Retrieval (flagRetrieveInteractiveWindows):**
        Detects the active window context to provide accurate cursor overlay 
        positioning and input routing during remote control sessions.
     
     The service operates entirely locally within the device. No user data is 
     collected, transmitted, or shared. All accessibility features are used 
     exclusively for remote control input handling.
     ```

4. **Provide Screenshots/Video** (if requested):
   - Show the app's remote control UI
   - Demonstrate the air mouse cursor on TV
   - Show the permission prompt for accessibility service

5. **Save and Submit:**
   - Click **Save** at the bottom
   - The declaration will be reviewed (usually within 24-48 hours)

---

## 🚀 After Declaration is Approved

Once the accessibility declaration is approved:

1. ✅ **Automatic CD workflow will work** - Future releases will deploy automatically
2. ✅ **No more manual intervention** - Just push to `main` branch
3. ✅ **Google Play releases** - The workflow will publish to the alpha track

---

## 🔍 Current CD Workflow Status

The CD workflow successfully:
- ✅ Builds the release APK/AAB
- ✅ Signs the app bundle
- ✅ Uploads to Google Play
- ❌ **Blocked by:** Accessibility declaration requirement

**Error Message:**
```
Your accessibility permission declaration needs to be updated.
```

---

## 📋 Key Service Details

For reference when completing the declaration:

**Service Name:** `TelePort Air Mouse`  
**Package:** `com.carfry369.***` (check actual package name)  
**API Used:** `android.accessibilityservice.AccessibilityService`  

**Features Used:**
- `android:canPerformGestures="true"` - For remote gesture injection
- `android:accessibilityFlags="flagRetrieveInteractiveWindows"` - For window context

**Data Collection:** None  
**Data Sharing:** None  
**Purpose:** Remote device control (air mouse functionality)

---

## 🔗 References

- [Google Play: Use of AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Play Console Policy Documentation](https://support.google.com/googleplay/android-developer)

---

## ✅ One-Time Setup

This is a **one-time manual step**. Once completed:
- The declaration persists for all future releases
- CD workflow will deploy automatically
- No code changes needed
