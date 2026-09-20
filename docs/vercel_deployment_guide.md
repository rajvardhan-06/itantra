# iTantra — Vercel Deployment Guide

This guide walks through deploying the iTantra landing page and APK distribution website to **Vercel** with continuous deployment from GitHub.

---

## 1. Prerequisites

- A **GitHub account** with access to `https://github.com/rajvardhan-06/itantra.git`.
- A **Vercel account** (sign up for free with GitHub at [vercel.com](https://vercel.com)).
- The project repository with the `web/` subfolder containing the Vite application.

---

## 2. 10-Step Deployment Walkthrough

### Step 1: Push Local Changes to GitHub
Ensure all website code, configurations, and documentation are committed:
```bash
git add .
git commit -m "feat: Add mobile-first Vercel website with APK download and privacy policy"
git push -u origin main
```
*(If prompted by Windows Git Credential Manager, complete your GitHub authentication in the browser).*

### Step 2: Log In to Vercel
1. Navigate to [vercel.com](https://vercel.com) and log in using your GitHub account.

### Step 3: Import Project
1. In the Vercel dashboard, click **"Add New..."** &rarr; **"Project"**.
2. Find **`itantra`** (or `rajvardhan-06/itantra`) in the Git repositories list.
3. Click **"Import"**.

### Step 4: Configure Project Root & Framework
Vercel will detect the repository. Configure the settings as follows:

| Setting | Value | Notes |
|---|---|---|
| **Project Name** | `itantra` | Or custom subdomain name |
| **Framework Preset** | **Vite** | Automatically detected |
| **Root Directory** | `web` | Click **Edit** and set to `web` |
| **Build Command** | `npm run build` | Default Vite build |
| **Output Directory** | `dist` | Default Vite output |
| **Install Command** | `npm install` | Default |

> **Note:** If you choose to keep the Root Directory as `./` (repo root), the root `vercel.json` will automatically execute `cd web && npm install && npm run build` and output from `web/dist`. Setting Root Directory to `web` is the cleanest and fastest approach.

### Step 5: Add Environment Variables (Optional)
Under the **Environment Variables** accordion:
- **Variable Name:** `VITE_ANDROID_APK_URL`
- **Variable Value:** *(Leave blank to serve the bundled APK from `/download/iTantra-v1.0.0.apk`, or enter your GitHub Release asset direct URL if hosted on GitHub Releases)*.

### Step 6: Deploy
Click **"Deploy"**. Vercel will:
- Clone your repository.
- Install dependencies (`vite`).
- Run `vite build` to bundle HTML, CSS, JavaScript, and static assets.
- Deploy to edge nodes globally.

Deployment typically completes in **under 30 seconds**.

### Step 7: Verify Production URL
Once the deployment screen displays "Congratulations!", click on the deployment preview screenshot or domain (e.g., `https://itantra.vercel.app`).

### Step 8: Test Android APK Download
1. Open the production URL on an Android smartphone or Chrome desktop.
2. Click the **"Download APK v1.0.0"** button in the hero section.
3. Verify that:
   - The browser prompts to save `iTantra-v1.0.0.apk`.
   - The download progress completes smoothly (~11.9 MB).
   - The file size matches `11,926,533 bytes`.

### Step 9: Test Mobile QR Code
1. Open the production URL on a laptop or desktop computer.
2. Scroll to the **"Download"** section.
3. Point an Android smartphone camera at the generated vector QR code on the screen.
4. Verify that the mobile phone detects the APK download URL and begins downloading immediately.

### Step 10: Configure Custom Domain (Optional)
To connect a custom domain (e.g., `itantra.org` or `getitantra.com`):
1. In Vercel, go to **Project Settings** &rarr; **Domains**.
2. Enter your domain name and follow the DNS CNAME/A record instructions.

---

## 3. Local Development & Preview Commands

You can run and test the website locally at any time:

```bash
# Navigate to the web folder
cd web

# Install dependencies (if not already installed)
npm install

# Start local dev server with hot reload
npm run dev

# Build production bundle
npm run build

# Preview the production build locally
npm run preview
```

---

## 4. Troubleshooting Common Deployment Issues

| Issue | Root Cause | Solution |
|---|---|---|
| **Vercel Build Error: `Cannot find module`** | Root directory not set | Set **Root Directory** in Vercel to `web` or ensure root `vercel.json` exists. |
| **APK downloads as `.zip` on some phones** | Missing MIME header | Handled by `vercel.json` setting `Content-Type: application/vnd.android.package-archive`. |
| **404 on `/privacy`** | Missing routing rewrite | Handled by multi-page entry in `web/vite.config.js` and `cleanUrls: true` in `vercel.json`. |
| **Git Push hangs or fails** | Windows credential manager waiting for browser auth | Run `git push origin main` in a native terminal window to trigger the browser login popup. |
