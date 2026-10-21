import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { api, getToken, getUser } from './api.js';

const FCM_TOKEN_KEY = 'nabta_fcm_token';
const FCM_TOKEN_UPLOADED_KEY = 'nabta_fcm_token_uploaded';
let listenersInstalled = false;
let registrationRequested = false;

function canRegisterAdminDevice() {
  return Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android' &&
    Boolean(getToken()) && ['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role);
}

function statusChanged() {
  window.dispatchEvent(new CustomEvent('nabta:push-status-changed'));
}

export async function getAdminPushNotificationStatus() {
  if (!Capacitor.isNativePlatform() || Capacitor.getPlatform() !== 'android') return { state: 'not-native' };
  if (!getToken() || !['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role)) return { state: 'admin-only' };
  try {
    const permission = await PushNotifications.checkPermissions();
    if (permission.receive === 'denied') return { state: 'denied' };
    if (permission.receive !== 'granted') return { state: 'permission-needed' };
    const token = localStorage.getItem(FCM_TOKEN_KEY) || '';
    if (!token) return { state: 'permitted-not-registered' };
    if (localStorage.getItem(FCM_TOKEN_UPLOADED_KEY) !== token) return { state: 'token-not-saved' };
    return { state: 'registered' };
  } catch (error) {
    console.warn('Nabta could not read notification permission status.', error);
    return { state: 'permission-needed' };
  }
}

export async function registerAdminPushNotifications(options = {}) {
  const force = options?.force === true;
  if (!Capacitor.isNativePlatform() || Capacitor.getPlatform() !== 'android') return { ok: false, reason: 'not-native' };
  if (!getToken() || !['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role)) return { ok: false, reason: 'admin-only' };
  if (registrationRequested && !force) return { ok: true, reason: 'already-requested' };
  registrationRequested = true;
  try {
    if (!listenersInstalled) {
      await PushNotifications.addListener('registration', async (registration) => {
        const token = String(registration?.value || '').trim();
        if (!token || !['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role) || !getToken()) return;
        localStorage.setItem(FCM_TOKEN_KEY, token);
        localStorage.removeItem(FCM_TOKEN_UPLOADED_KEY);
        statusChanged();
        try {
          await api('/device-tokens', { method: 'POST', body: { token, platform: 'ANDROID' } });
          localStorage.setItem(FCM_TOKEN_UPLOADED_KEY, token);
          statusChanged();
        } catch (error) {
          console.warn('Nabta: the push token was created but could not be saved on the backend.', error);
          statusChanged();
        }
      });
      await PushNotifications.addListener('registrationError', (error) => {
        registrationRequested = false;
        console.warn('Nabta push-notification registration failed. Check Firebase setup.', error);
        statusChanged();
      });
      await PushNotifications.addListener('pushNotificationReceived', (notification) => {
        window.dispatchEvent(new CustomEvent('nabta:push-received', {
          detail: { title: notification?.title || '', body: notification?.body || '' }
        }));
      });
      await PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
        const data = action?.notification?.data || {};
        if (['NEW_ORDER','DELIVERY_ASSIGNED','OUT_FOR_DELIVERY','ORDER_DELIVERED','PARTIAL_DELIVERY'].includes(data.type) && ['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role)) {
          window.dispatchEvent(new CustomEvent('nabta:open-admin-orders', {
            detail: { orderId: data.orderId || '', orderNumber: data.orderNumber || '' }
          }));
        }
      });
      listenersInstalled = true;
    }

    let permission = await PushNotifications.checkPermissions();
    if (permission.receive !== 'granted') permission = await PushNotifications.requestPermissions();
    if (permission.receive !== 'granted') {
      registrationRequested = false;
      statusChanged();
      return { ok: false, reason: 'permission-denied' };
    }
    await PushNotifications.register();
    return { ok: true, reason: 'registration-requested' };
  } catch (error) {
    registrationRequested = false;
    console.warn('Nabta push notifications are not available. Ensure Firebase Android configuration is installed.', error);
    statusChanged();
    return { ok: false, reason: 'registration-error', error };
  }
}

export async function unregisterAdminPushNotifications() {
  const token = localStorage.getItem(FCM_TOKEN_KEY) || '';
  try {
    if (token && getToken() && ['ADMIN','SUPERVISOR','DELIVERY'].includes(getUser()?.role)) {
      await api(`/device-tokens?token=${encodeURIComponent(token)}`, { method: 'DELETE' });
    }
  } catch (error) {
    console.warn('Nabta could not remove the device token from the backend before logout.', error);
  }
  localStorage.removeItem(FCM_TOKEN_KEY);
  localStorage.removeItem(FCM_TOKEN_UPLOADED_KEY);
  registrationRequested = false;
  if (Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android') {
    try { await PushNotifications.unregister(); }
    catch (error) { console.warn('Nabta could not unregister the native push token.', error); }
  }
  statusChanged();
}
