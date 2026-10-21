import type { CapacitorConfig } from '@capacitor/cli';

// Local testing uses an HTTP Spring Boot server. The scripts set
// NABTA_ANDROID_DEV_HTTP=true only while generating/syncing the dev app.
// Leave this false for a production build that uses an HTTPS API.
const allowLocalHttp = process.env.NABTA_ANDROID_DEV_HTTP === 'true';

const config: CapacitorConfig = {
  appId: 'com.nabta.app',
  appName: 'Nabta',
  webDir: 'www',
  backgroundColor: '#356b4c',
  android: {
    allowMixedContent: allowLocalHttp,
    webContentsDebuggingEnabled: allowLocalHttp,
  },
  server: {
    androidScheme: 'https',
    cleartext: allowLocalHttp,
  },
};

export default config;
