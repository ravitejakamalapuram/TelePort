# 🌐 Chrome Web Store Publishing Setup

## 📋 Current Status

**Chrome Extension:** ✅ Built and uploaded to GitHub Releases  
**Chrome Web Store:** ❌ Not auto-published (manual upload only)

---

## 🎯 What You Need

To enable automatic Chrome Web Store publishing in your CD workflow, you need **4 secrets**:

1. `CHROME_EXTENSION_ID` - Your extension's ID from Chrome Web Store
2. `CHROME_CLIENT_ID` - OAuth client ID for Chrome Web Store API
3. `CHROME_CLIENT_SECRET` - OAuth client secret
4. `CHROME_REFRESH_TOKEN` - OAuth refresh token

---

## 📝 Step-by-Step Setup

### **Step 1: Find Your Extension ID**

1. Go to [Chrome Web Store Developer Dashboard](https://chrome.google.com/webstore/devconsole)
2. Click on your **TelePort Cast & Remote** extension
3. Your extension ID is in the URL: `https://chrome.google.com/webstore/detail/YOUR_EXTENSION_ID`
4. Or find it in the dashboard under "Package" → "Item ID"
5. **Save this as:** `CHROME_EXTENSION_ID`

---

### **Step 2: Enable Chrome Web Store API**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project (or create a new one)
3. Go to: https://console.cloud.google.com/apis/library/chromewebstore.googleapis.com
4. Click **"Enable"**

---

### **Step 3: Create OAuth Credentials**

1. Go to [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials)
2. Click **"Create Credentials"** → **"OAuth client ID"**
3. If prompted, configure the OAuth consent screen:
   - User Type: **External**
   - App name: **TelePort Chrome Extension Publisher**
   - User support email: Your email
   - Developer contact: Your email
   - Save and continue
4. For Application type, choose **"Desktop app"**
5. Name: **Chrome Webstore Upload**
6. Click **"Create"**
7. **Save these values:**
   - Client ID → `CHROME_CLIENT_ID`
   - Client Secret → `CHROME_CLIENT_SECRET`

---

### **Step 4: Generate Refresh Token**

**Option A: Using CLI Tool (Recommended)**

```bash
# Install the tool
npm install -g chrome-webstore-upload-keys
# OR
npx chrome-webstore-upload-keys
```

The tool will:
1. Prompt you for `CLIENT_ID` and `CLIENT_SECRET`
2. Open a browser for Google OAuth
3. Return your `REFRESH_TOKEN`

**Option B: Using OAuth Playground**

1. Go to https://developers.google.com/oauthplayground
2. Click the ⚙️ settings icon (top right)
3. Check **"Use your own OAuth credentials"**
4. Enter your `CLIENT_ID` and `CLIENT_SECRET`
5. In the left panel, find **"Chrome Web Store API v1.1"**
6. Select: `https://www.googleapis.com/auth/chromewebstore`
7. Click **"Authorize APIs"**
8. Sign in with your Google Account (the one that owns the extension)
9. Click **"Exchange authorization code for tokens"**
10. **Save the** `refresh_token` → `CHROME_REFRESH_TOKEN`

---

## 🔑 Add Secrets to GitHub

1. Go to your repository: https://github.com/ravitejakamalapuram/TelePort
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"** for each:

| Secret Name | Value |
|-------------|-------|
| `CHROME_EXTENSION_ID` | Your extension ID |
| `CHROME_CLIENT_ID` | OAuth client ID |
| `CHROME_CLIENT_SECRET` | OAuth client secret |
| `CHROME_REFRESH_TOKEN` | OAuth refresh token |

---

## ✅ Enable Auto-Publishing

Once you've added all 4 secrets:

1. Open `.github/workflows/cd.yml`
2. **Uncomment** the `publish-chrome` job (lines 29-70)
3. Remove the comment markers (`#`) from the entire job
4. Commit and push:

```bash
git add .github/workflows/cd.yml
git commit -m "feat: enable Chrome Web Store auto-publishing"
git push
```

---

## 🚀 How It Works

After setup, every push to `main`:

1. ✅ Builds Android APK/AAB → uploads to Play Store
2. ✅ Packages Chrome Extension → uploads to GitHub Releases
3. ✅ **Publishes Chrome Extension → Chrome Web Store automatically**

---

## 🔍 Verify It's Working

1. Push a commit to `main`
2. Check GitHub Actions: https://github.com/ravitejakamalapuram/TelePort/actions
3. Look for the **"Publish Chrome Extension"** job
4. If successful, your extension will be live on Chrome Web Store within minutes

---

## 📚 Resources

- [Chrome Web Store API Docs](https://developer.chrome.com/docs/webstore/using-api)
- [How to Generate Google API Keys](https://github.com/DrewML/chrome-webstore-upload/blob/master/How%20to%20generate%20Google%20API%20keys.md)
- [chrome-webstore-upload-keys Tool](https://github.com/fregante/chrome-webstore-upload-keys)

---

## ❓ Troubleshooting

### "Invalid client ID"
- Double-check your `CHROME_CLIENT_ID` matches exactly from Google Cloud Console

### "Invalid refresh token"
- Refresh tokens can expire if unused for 6 months
- Regenerate using OAuth Playground (Step 4)

### "Extension not found"
- Verify `CHROME_EXTENSION_ID` is correct
- Make sure you're signed in with the correct Google Account

### "Insufficient permissions"
- Your Google Account must be an **owner** or **developer** of the extension in Chrome Web Store Dashboard

---

## 📝 Current Workaround

Until you set up auto-publishing:

1. Download `extension.zip` from [GitHub Releases](https://github.com/ravitejakamalapuram/TelePort/releases/latest)
2. Go to [Chrome Web Store Developer Dashboard](https://chrome.google.com/webstore/devconsole)
3. Upload manually

---

**Once setup is complete, Chrome Web Store publishing will be fully automated!** 🎉
