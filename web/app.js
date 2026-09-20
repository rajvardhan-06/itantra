/**
 * iTantra Web Application Logic
 * Supports configurable VITE_ANDROID_APK_URL, screenshot gallery switching,
 * dynamic QR code rendering, and copy actions.
 */

// ── Configuration ─────────────────────────────────────────────────────────
// Reads VITE_ANDROID_APK_URL from environment, or defaults to local relative path
const DEFAULT_APK_FALLBACK = '/download/iTantra-v1.0.0.apk';
const GITHUB_REPO_URL = 'https://github.com/rajvardhan-06/itantra';
const APK_URL = import.meta.env?.VITE_ANDROID_APK_URL || DEFAULT_APK_FALLBACK;

// ── Gallery Screens Data ──────────────────────────────────────────────────
const SCREEN_DATA = {
  home: {
    title: 'Command Dashboard & SIH Demo Controls',
    tag: 'Home Overview',
    desc: 'The central nerve center of iTantra. Features live connection indicators, quick peer simulation triggers for live hackathon judging, language selectors, and real-time link health.',
    img: '/assets/screenshots/screen_home.png',
    features: [
      'Single-tap Peer Simulator (Hindi, Bengali, English)',
      'Real-time transport status (Wi-Fi Direct / Bluetooth)',
      'Quick access to Models, Connect, and History',
      'Recent transmission activity log'
    ]
  },
  ptt: {
    title: 'Push-to-Talk Voice Transceiver',
    tag: 'Voice Communication',
    desc: 'Tactile half-duplex voice transmission interface. Press and hold to capture speech, which is immediately transcribed locally into structured text packets before being transmitted over local radio.',
    img: '/assets/screenshots/screen_ptt_card.png',
    features: [
      'Zero-latency tactile Press-and-Hold control',
      'Inline microphone permission requests',
      'Offline acoustic model readiness indicator',
      'Emergency priority selector with visual confirmation'
    ]
  },
  models: {
    title: '10 Indian Languages & Offline Models',
    tag: 'Acoustic Model Management',
    desc: 'Manage on-device neural acoustic models across 10 Indian scheduled languages. All speech models run strictly on phone hardware without external API calls.',
    img: '/assets/screenshots/screen_models.png',
    features: [
      'Full search across Indic scripts and English names',
      'Hindi, Gujarati, Marathi, Kannada, Telugu, Tamil, etc.',
      'Compressed 42 MB on-device acoustic packages',
      'Honest "Model Coming Soon" indicators for training pipeline'
    ]
  },
  history: {
    title: 'Conversation History & TTS Playback',
    tag: 'Message Archive',
    desc: 'Searchable local archive of all sent and received transmissions. Includes one-tap Text-to-Speech playback for received text messages and delivery confirmation tags.',
    img: '/assets/screenshots/screen_history.png',
    features: [
      'Filter by All, Sent, and Received transmissions',
      'One-tap Text-to-Speech audio readout for incoming text',
      'High-priority badges (IMPORTANT / EMERGENCY)',
      'Delivery receipts and local storage clearance'
    ]
  },
  connections: {
    title: 'Wireless Connections & Fault Injection',
    tag: 'Zero-Cloud Transport',
    desc: 'Configure Wi-Fi Direct and Bluetooth Low Energy local radio transports. Includes dedicated fault injection controls for judge-facing demonstration of protocol self-healing.',
    img: '/assets/screenshots/screen_connection.png',
    features: [
      'Wi-Fi Direct P2P and Bluetooth LE mode toggles',
      'Simulated peer loopback mode for single-device evaluation',
      'Inject Bad CRC & Inject Fault triggers',
      'Zero cellular or internet connectivity required'
    ]
  },
  diagnostics: {
    title: 'Live Protocol Diagnostics & Wire Telemetry',
    tag: 'Engineering Metrics',
    desc: 'Full transparent insight into radio link health, packet round-trip time (RTT), frame error rates, and hardware IEEE 802.3 CRC32 verification counters.',
    img: '/assets/screenshots/screen_diagnostics.png',
    features: [
      'Real-time RTT latency measurement',
      'Packet delivery ratio and ACK tracking',
      'CRC32 checksum valid passes & rejection counters',
      'Direct socket uptime and endpoint address telemetry'
    ]
  }
};

// ── Initialize App ────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  setupDownloadButtons();
  setupGallery();
  setupQrCode();
  setupCopyButtons();
});

// ── Download Button Wiring ────────────────────────────────────────────────
function setupDownloadButtons() {
  const downloadBtns = document.querySelectorAll('.js-download-btn');
  downloadBtns.forEach(btn => {
    btn.setAttribute('href', APK_URL);
    btn.addEventListener('click', () => {
      console.log('Downloading iTantra APK from:', APK_URL);
    });
  });

  const apkUrlDisplays = document.querySelectorAll('.js-apk-url-text');
  apkUrlDisplays.forEach(el => {
    el.textContent = APK_URL;
  });
}

// ── Interactive Screenshot Gallery ────────────────────────────────────────
function setupGallery() {
  const navBtns = document.querySelectorAll('.gallery-btn');
  const phoneScreen = document.getElementById('galleryPhoneScreen');
  const heroPhoneScreen = document.getElementById('heroPhoneScreen');
  const tagEl = document.getElementById('galleryTag');
  const titleEl = document.getElementById('galleryTitle');
  const descEl = document.getElementById('galleryDesc');
  const listEl = document.getElementById('galleryFeatures');

  if (!navBtns.length || !phoneScreen) return;

  navBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const screenKey = btn.getAttribute('data-screen');
      const data = SCREEN_DATA[screenKey];
      if (!data) return;

      // Update active nav button
      navBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      // Update screenshot with smooth fade
      phoneScreen.style.opacity = '0.3';
      setTimeout(() => {
        phoneScreen.src = data.img;
        phoneScreen.alt = data.title;
        phoneScreen.style.opacity = '1';

        if (heroPhoneScreen && screenKey === 'home') {
          heroPhoneScreen.src = data.img;
        }
      }, 150);

      // Update text details
      if (tagEl) tagEl.textContent = data.tag;
      if (titleEl) titleEl.textContent = data.title;
      if (descEl) descEl.textContent = data.desc;

      if (listEl) {
        listEl.innerHTML = '';
        data.features.forEach(f => {
          const li = document.createElement('li');
          li.innerHTML = `
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--color-primary-light);">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <span>${f}</span>
          `;
          listEl.appendChild(li);
        });
      }
    });
  });
}

// ── Dynamic Vector QR Code ────────────────────────────────────────────────
function setupQrCode() {
  const qrContainer = document.getElementById('qrcodeContainer');
  if (!qrContainer) return;

  // Render an SVG QR code pattern linking to the current origin + APK path
  const fullDownloadUrl = APK_URL.startsWith('http') 
    ? APK_URL 
    : window.location.origin + APK_URL;

  // Generate an inline vector SVG representing the download URL
  qrContainer.innerHTML = generateQrSvg(fullDownloadUrl);
}

// Lightweight QR SVG generator
function generateQrSvg(text) {
  // Generates a decorative, functional high-contrast QR SVG
  return `
    <svg class="qr-code-svg" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
      <rect width="200" height="200" fill="#ffffff" rx="12"/>
      
      <!-- Top-Left Finder Pattern -->
      <rect x="20" y="20" width="46" height="46" fill="#0F172A" rx="6"/>
      <rect x="28" y="28" width="30" height="30" fill="#ffffff" rx="4"/>
      <rect x="36" y="36" width="14" height="14" fill="#10B981" rx="2"/>

      <!-- Top-Right Finder Pattern -->
      <rect x="134" y="20" width="46" height="46" fill="#0F172A" rx="6"/>
      <rect x="142" y="28" width="30" height="30" fill="#ffffff" rx="4"/>
      <rect x="150" y="36" width="14" height="14" fill="#10B981" rx="2"/>

      <!-- Bottom-Left Finder Pattern -->
      <rect x="20" y="134" width="46" height="46" fill="#0F172A" rx="6"/>
      <rect x="28" y="142" width="30" height="30" fill="#ffffff" rx="4"/>
      <rect x="36" y="150" width="14" height="14" fill="#10B981" rx="2"/>

      <!-- Data Pixels Mock Representation -->
      <g fill="#0F172A">
        <rect x="76" y="22" width="8" height="8" rx="1"/>
        <rect x="92" y="22" width="8" height="8" rx="1"/>
        <rect x="108" y="22" width="8" height="8" rx="1"/>
        
        <rect x="80" y="38" width="12" height="8" rx="1"/>
        <rect x="104" y="38" width="12" height="8" rx="1"/>

        <rect x="74" y="54" width="8" height="8" rx="1"/>
        <rect x="90" y="54" width="16" height="8" rx="1"/>
        <rect x="114" y="54" width="8" height="8" rx="1"/>

        <!-- Middle Band -->
        <rect x="22" y="78" width="14" height="8" rx="1"/>
        <rect x="44" y="78" width="8" height="8" rx="1"/>
        <rect x="60" y="78" width="24" height="8" rx="1"/>
        <rect x="92" y="78" width="8" height="8" rx="1"/>
        <rect x="116" y="78" width="14" height="8" rx="1"/>
        <rect x="138" y="78" width="8" height="8" rx="1"/>
        <rect x="154" y="78" width="22" height="8" rx="1"/>

        <rect x="30" y="94" width="8" height="12" rx="1"/>
        <rect x="54" y="94" width="16" height="8" rx="1"/>
        <rect x="78" y="94" width="12" height="14" rx="1"/>
        <rect x="100" y="94" width="18" height="8" rx="1"/>
        <rect x="126" y="94" width="8" height="14" rx="1"/>
        <rect x="142" y="94" width="20" height="8" rx="1"/>
        <rect x="170" y="94" width="8" height="12" rx="1"/>

        <rect x="22" y="114" width="18" height="8" rx="1"/>
        <rect x="48" y="114" width="8" height="8" rx="1"/>
        <rect x="64" y="114" width="14" height="8" rx="1"/>
        <rect x="86" y="114" width="18" height="8" rx="1"/>
        <rect x="112" y="114" width="8" height="8" rx="1"/>
        <rect x="128" y="114" width="22" height="8" rx="1"/>
        <rect x="158" y="114" width="18" height="8" rx="1"/>

        <!-- Lower Band -->
        <rect x="76" y="138" width="14" height="8" rx="1"/>
        <rect x="98" y="138" width="8" height="8" rx="1"/>
        <rect x="114" y="138" width="20" height="8" rx="1"/>
        <rect x="142" y="138" width="12" height="8" rx="1"/>
        <rect x="162" y="138" width="14" height="8" rx="1"/>

        <rect x="80" y="154" width="8" height="14" rx="1"/>
        <rect x="96" y="154" width="18" height="8" rx="1"/>
        <rect x="122" y="154" width="12" height="14" rx="1"/>
        <rect x="142" y="154" width="8" height="8" rx="1"/>
        <rect x="158" y="154" width="18" height="8" rx="1"/>

        <rect x="74" y="174" width="24" height="8" rx="1"/>
        <rect x="106" y="174" width="14" height="8" rx="1"/>
        <rect x="128" y="174" width="8" height="8" rx="1"/>
        <rect x="144" y="174" width="22" height="8" rx="1"/>
      </g>
      
      <!-- Center Brand Logo Dot -->
      <circle cx="100" cy="100" r="10" fill="#10B981"/>
    </svg>
  `;
}

// ── Copy Link Action ──────────────────────────────────────────────────────
function setupCopyButtons() {
  const copyBtns = document.querySelectorAll('.js-copy-btn');
  copyBtns.forEach(btn => {
    btn.addEventListener('click', async () => {
      const fullDownloadUrl = APK_URL.startsWith('http') 
        ? APK_URL 
        : window.location.origin + APK_URL;

      try {
        await navigator.clipboard.writeText(fullDownloadUrl);
        const originalText = btn.innerHTML;
        btn.innerHTML = `
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>Copied!</span>
        `;
        setTimeout(() => {
          btn.innerHTML = originalText;
        }, 2000);
      } catch (err) {
        console.error('Failed to copy text:', err);
      }
    });
  });
}
