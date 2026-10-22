import { defineCustomElements } from '@ionic/core/loader';
import { Capacitor, registerPlugin } from '@capacitor/core';
import '@ionic/core/css/ionic.bundle.css';
import './styles.css';
import { api, getApiBase, getToken, getUser, setSession, clearSession, loadPage } from './api.js';
import { registerAdminPushNotifications, unregisterAdminPushNotifications, getAdminPushNotificationStatus } from './push-notifications.js';
import { t, getLanguage, setLanguage, applyLanguage } from './i18n.js';

defineCustomElements(window);
function startNabtaWelcomeScreen() {
  const screen = document.querySelector('#nabtaWelcomeScreen');
  if (!screen) return;
  const ar = getLanguage() === 'ar';
  const title = screen.querySelector('#nabtaWelcomeTitle');
  const message = screen.querySelector('#nabtaWelcomeMessage');
  if (title) title.textContent = ar ? 'مرحباً بكم في شركة نبته الغذائية' : 'Welcome to Nabta Food Company';
  if (message) message.textContent = ar ? 'جودة تثق بها، ونكهات تجمعنا.' : 'Quality you trust. Flavours that bring us together.';
  window.setTimeout(() => screen.classList.add('is-hidden'), 1650);
  window.setTimeout(() => screen.remove(), 2150);
}
startNabtaWelcomeScreen();
const NabtaApkShare = registerPlugin('NabtaApkShare');
const APP_DOWNLOAD_URL = String(import.meta.env.VITE_NABTA_APK_URL || '').trim();
const LOGIN_DOOR_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M14 4h6v16h-6"/><path d="M3 12h12m-4-4 4 4-4 4"/></svg>';

const view = document.querySelector('#view');
const modalHost = document.querySelector('#modalHost');
const toastHost = document.querySelector('#toastHost');
// Portal dialogs outside the scrolling application shell so they remain visible at every page position.
document.body.appendChild(modalHost);
const state = { page: 'home', products: [], categories: [], cart: readCart(), company: null, loading: false, search: '', categoryId: null, adminTab: 'dashboard', settingsTab: 'profile', editingProduct: null, favorites: [], operationsTab: 'orders' };
const currency = (value) => new Intl.NumberFormat(getLanguage() === 'ar' ? 'ar-SA' : 'en-SA', { style: 'currency', currency: 'SAR', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value || 0));
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
const DEFAULT_COMPANY_PHONE = '+966 55 511 3539'; // Example only; replace in Admin > Company settings.
const DEFAULT_SOCIAL_LINKS = {
  xUrl: 'https://x.com/',
  instagramUrl: 'https://www.instagram.com/',
  snapchatUrl: 'https://www.snapchat.com/',
  mapsUrl: 'https://www.google.com/maps/search/?api=1&query=Nabta+Food+Company+Madinah'
};
function safeExternalUrl(value, fallback) {
  try {
    const url = new URL(String(value || '').trim());
    return ['https:', 'http:'].includes(url.protocol) ? url.href : fallback;
  } catch { return fallback; }
}
function internationalPhoneDigits(value) {
  const original = String(value || '').trim();
  let digits = original.replace(/\D/g, '');
  if (!digits) return '';
  if (original.startsWith('00')) digits = digits.slice(2);
  else if (digits.startsWith('9660')) digits = `966${digits.slice(4)}`;
  else if (digits.startsWith('0')) digits = `966${digits.slice(1)}`;
  else if (!digits.startsWith('966')) digits = `966${digits}`;
  return digits;
}
const initials = (name) => String(name || '?').trim().slice(0,1).toUpperCase();
function displayText(en, ar) { const preferred = getLanguage() === 'ar' ? ar : en; return String(preferred || en || ar || ''); }
// Names/categories are shown in both languages; selected language is listed first.
function displayBilingualText(en, ar) {
  const english = String(en ?? '').trim();
  const arabic = String(ar ?? '').trim();
  if (english && arabic) return getLanguage() === 'ar' ? `${arabic} / ${english}` : `${english} / ${arabic}`;
  return english || arabic;
}
function bilingualFormFields({ label, nameEn, nameAr, valueEn = '', valueAr = '', type = 'input', required = false, maxLength, rows = 3, className = '' }) {
  const make = (language, fieldName, value) => {
    const languageLabel = language === 'ar' ? 'العربية' : 'English';
    const dir = language === 'ar' ? 'rtl' : 'ltr';
    const cls = className ? ` class="${className}"` : '';
    const lengthAttr = Number.isFinite(maxLength) ? ` maxlength="${maxLength}"` : '';
    const requiredAttr = required ? ' required' : '';
    const field = type === 'textarea'
      ? `<textarea name="${fieldName}" rows="${rows}" dir="${dir}"${lengthAttr}>${escapeHtml(value)}</textarea>`
      : `<input name="${fieldName}" dir="${dir}"${requiredAttr}${lengthAttr} value="${escapeHtml(value)}">`;
    return `<label${cls}>${escapeHtml(label)} — ${languageLabel}${field}</label>`;
  };
  const englishField = make('en', nameEn, valueEn);
  const arabicField = make('ar', nameAr, valueAr);
  // Grid auto-placement follows page direction: current-language field starts first.
  return getLanguage() === 'ar' ? arabicField + englishField : englishField + arabicField;
}
function displayProductName(product) { return displayBilingualText(product?.name, product?.nameAr); }
function displayProductDescription(product) { return displayText(product?.description, product?.descriptionAr); }
function displayCategoryName(category) { return displayBilingualText(category?.name, category?.nameAr); }
function validPhone(value) { return /^[0-9]{7,20}$/.test(String(value || '').trim()); }
function absoluteMediaUrl(value) { const raw=String(value||'').trim(); if(!raw) return ''; if(/^https?:\/\//i.test(raw)) return raw; const root=getApiBase().replace(/\/api\/?$/,''); return raw.startsWith('/') ? `${root}${raw}` : `${root}/${raw}`; }
function isOperationsRole(role=getUser()?.role) { return ['ADMIN','SUPERVISOR','DELIVERY'].includes(role); }
function readCart() { try { return JSON.parse(localStorage.getItem('nabta_cart') || '[]'); } catch { return []; } }
function saveCart() { localStorage.setItem('nabta_cart', JSON.stringify(state.cart)); updateCartCount(); }
function updateCartCount() { const count = state.cart.reduce((sum, item) => sum + item.quantity, 0); const node = document.querySelector('#cartCount'); if (node) node.textContent = count; }
function toast(message, kind = 'success') { const el = document.createElement('div'); el.className = `toast toast-${kind}`; el.textContent = message; toastHost.append(el); setTimeout(() => el.remove(), 3800); }
function showError(error, target = view) { if (!target) return; target.querySelector('.inline-error')?.remove(); const el = document.createElement('div'); el.className = 'inline-error'; el.textContent = error?.message || t('networkError'); target.prepend(el); }
function setBusy(button, busy, label = '') { if (!button) return; if (busy) { button.dataset.oldText = button.textContent; button.disabled = true; button.textContent = label || t('processing'); } else { button.disabled = false; if (button.dataset.oldText) button.textContent = button.dataset.oldText; delete button.dataset.oldText; } }
function requireUser() { if (!getUser()) { openAuth('login'); return false; } return true; }
function refreshNav() {
  const user = getUser();
  const accountLabel = document.querySelector('#accountLabel');
  const accountInitial = document.querySelector('#accountInitial');
  const topLogoutButton = document.querySelector('#topLogoutButton');
  const companyName = document.querySelector('#topCompanyName');
  if (accountLabel) accountLabel.textContent = user ? `${getLanguage() === 'ar' ? 'مرحباً، ' : 'Hello, '}${user.name || user.email}` : t('signIn');
  if (accountInitial) { if (user) { accountInitial.classList.remove('login-door-mark'); accountInitial.textContent = initials(user.name || user.email); } else { accountInitial.classList.add('login-door-mark'); accountInitial.innerHTML = LOGIN_DOOR_ICON; } }
  if (topLogoutButton) topLogoutButton.classList.toggle('hidden', !user);
  if (companyName) companyName.textContent = getLanguage() === 'ar' ? 'شركة نبته الغذائية' : 'Nabta Food Company';
  document.querySelectorAll('.admin-nav').forEach(el => el.classList.toggle('hidden', user?.role !== 'ADMIN'));
  document.querySelectorAll('.operations-nav').forEach(el => el.classList.toggle('hidden', !['SUPERVISOR','DELIVERY'].includes(user?.role)));
  document.querySelectorAll('[data-nav=operations]').forEach(el => el.classList.toggle('hidden', !['SUPERVISOR','DELIVERY'].includes(user?.role)));
  document.querySelectorAll('[data-nav]').forEach(el => el.classList.toggle('active', el.dataset.nav === state.page));
  document.querySelector('#settingsButton')?.classList.toggle('active', state.page === 'settings');
  const categoryHost = document.querySelector('#primaryCategoryLinks');
  if (categoryHost) {
    categoryHost.innerHTML = state.categories.slice(0, 3).map(category => `<button class="nav-link nav-category" data-category="${escapeHtml(category.id)}" type="button">${escapeHtml(displayCategoryName(category))}</button>`).join('');
  }
  updateCartCount();
}
async function fetchCatalog() {
  try {
    const [products, categories, company] = await Promise.all([
      loadPage(`/products?q=${encodeURIComponent(state.search)}${state.categoryId ? `&categoryId=${encodeURIComponent(state.categoryId)}` : ''}&page=0&size=40`),
      api('/categories'), api('/company').catch(() => null)
    ]);
    state.products = products;
    state.categories = Array.isArray(categories) ? categories : [];
    state.company = company;
    state.favorites = getToken() ? await api('/users/me/favorites').catch(() => []) : [];
  } catch (error) { state.loadError = error.message; }
}
function loadingMarkup() { return `<div class="loading-panel"><span class="spinner"></span><p>${t('loading')}</p></div>`; }
function emptyMarkup(title, desc, button = '') { return `<div class="empty-state"><div class="empty-illustration">✿</div><h3>${escapeHtml(title)}</h3><p>${escapeHtml(desc)}</p>${button}</div>`; }
function imageMarkup(product, compact = false) {
  const url = absoluteMediaUrl(product.imageUrl);
  if (url) return `<img class="product-image ${compact?'compact':''}" src="${escapeHtml(url)}" alt="${escapeHtml(displayProductName(product))}" loading="lazy" onerror="this.replaceWith(Object.assign(document.createElement('div'),{className:'product-placeholder',textContent:'✿'}))">`;
  return `<div class="product-placeholder ${compact?'compact':''}" aria-label="No image">${categoryGlyph(product.categoryName || product.name)}</div>`;
}
function categoryGlyph(name) { const source = String(name || '').toLowerCase(); if (source.includes('pot')) return '◉'; if (source.includes('seed')) return '✺'; if (source.includes('tool')) return '⚒'; return '❀'; }
function productCartControl(product, detail = false) {
  const stock = Number(product.stockQuantity || 0);
  const item = state.cart.find(entry => String(entry.id) === String(product.id));
  if (item) return `<div class="product-cart-quantity ${detail ? 'product-cart-quantity-detail' : ''}" role="group" aria-label="${t('quantity')}"><button type="button" data-qty="${product.id}" data-delta="-1" aria-label="Decrease quantity">−</button><span aria-live="polite">${item.quantity}</span><button type="button" data-qty="${product.id}" data-delta="1" aria-label="Increase quantity" ${stock < 1 || item.quantity >= stock ? 'disabled' : ''}>＋</button></div>`;
  return `<button type="button" class="${detail ? 'btn btn-primary detail-add-btn' : 'add-btn'}" data-add="${product.id}" ${stock < 1 ? 'disabled' : ''} aria-label="${t('addToCart')}">＋${detail ? ` ${t('addToCart')}` : ''}</button>`;
}
function productCard(product) {
  const stock = Number(product.stockQuantity || 0);
  const favorite = state.favorites.map(String).includes(String(product.id));
  const name = displayProductName(product);
  const category = displayBilingualText(product.categoryName, product.categoryNameAr) || t('categories');
  return `<article class="product-card"><button class="product-image-button" data-product-details="${product.id}">${imageMarkup(product)}<span class="stock-pill ${stock > 0 ? 'in-stock' : 'out-stock'}">${stock > 0 ? t('available') : t('outOfStock')}</span></button><div class="product-copy"><div class="product-category">${escapeHtml(category)}</div><button class="product-name" data-product-details="${product.id}">${escapeHtml(name)}</button><p class="product-desc">${escapeHtml(displayProductDescription(product) || t('noDescription'))}</p><div class="product-card-bottom"><strong class="price">${currency(product.price)}</strong><div class="product-actions"><button class="favorite-btn ${favorite?'is-favorite':''}" data-favorite-toggle="${product.id}" type="button" aria-label="${favorite?t('removeFromPreferences'):t('addToPreferences')}" title="${favorite?t('removeFromPreferences'):t('addToPreferences')}">${favorite?'♥':'♡'}</button>${productCartControl(product)}</div></div></div></article>`;
}
function categoryCard(category) { return `<button class="category-card" data-category="${category.id}"><span class="category-glyph">${categoryGlyph(displayCategoryName(category))}</span><span><strong>${escapeHtml(displayCategoryName(category))}</strong><small>${escapeHtml(displayText(category.description, category.descriptionAr) || t('browseProducts'))}</small></span><b>↗</b></button>`; }
function renderHome() {
  const heroTitle = getLanguage() === 'ar' ? 'كتالوج منتجات شركة نبته الغذائية' : 'Nabta Food Company Product Catalogue';
  const heroSubtitle = getLanguage() === 'ar'
    ? 'يمكنك تصفح المنتجات أدناه أو طلب الكتالوج الكامل مباشرة عبر واتساب المبيعات.'
    : 'Browse the products below or request the full catalogue directly through WhatsApp sales.';
  const heroLocation = getLanguage() === 'ar' ? 'المدينة المنورة' : 'Madinah, Saudi Arabia';
  const whatsappLabel = getLanguage() === 'ar' ? 'الطلب المباشر عبر الواتساب' : 'Order directly on WhatsApp';
  view.innerHTML = `<section class="hero"><div class="hero-copy"><span class="eyebrow"><i></i> ${heroLocation}</span><h1>${heroTitle}</h1><p>${heroSubtitle}</p><div class="hero-actions"><button class="btn btn-primary hero-whatsapp" id="heroWhatsAppButton" type="button"><span aria-hidden="true">💬</span> ${whatsappLabel}</button><button class="btn btn-hero-outline" data-nav="categories" type="button">${getLanguage()==='ar'?'تصفح المنتجات':'Browse products'} ↙</button></div></div><div class="hero-art" aria-hidden="true"></div><div class="hero-grain" aria-hidden="true"></div></section>
  <section class="search-row"><div class="searchbox"><span>⌕</span><input id="productSearch" placeholder="${t('searchPlaceholder')}" value="${escapeHtml(state.search)}"><button id="clearSearch" aria-label="Clear search">×</button></div><button class="filter-btn" id="refreshCatalog">↻ <span>${t('refresh')}</span></button></section>
  <section class="content-section"><div class="section-heading"><div><span class="section-kicker">01 / ${t('explore') || 'EXPLORE'}</span><h2>${t('popularCategories')}</h2></div><button class="text-link" data-nav="categories">${t('viewAll')} ↗</button></div><div class="category-grid">${state.categories.length ? state.categories.slice(0,4).map(categoryCard).join('') : emptyMarkup(t('noCategories'), t('noProductsText'))}</div></section>
  <section class="content-section products-section"><div class="section-heading"><div><span class="section-kicker">02 / NABTA FOOD COMPANY</span><h2>${t('allProducts')}</h2></div><button class="text-link" data-nav="categories">${t('viewAll')} ↗</button></div>${state.loadError ? `<div class="inline-error">${escapeHtml(state.loadError)} <button class="text-link" id="retryCatalog">${t('tryAgain')}</button></div>` : ''}<div class="product-grid">${state.products.length ? state.products.map(productCard).join('') : emptyMarkup(t('noProducts'), t('noProductsText'), `<button class="btn btn-outline" id="retryCatalog">${t('tryAgain')}</button>`)}</div></section>
  <section class="green-banner"><div><span class="eyebrow">NABTA FOOD COMPANY</span><h2>${getLanguage()==='ar'?'جودة تثق بها، ونكهات تجمعنا':'Quality you trust. Flavours that bring us together.'}</h2><p>${getLanguage()==='ar'?'تصفح منتجاتنا وتواصل معنا لطلب الكتالوج الكامل.':'Browse our products and contact us for the full catalogue.'}</p></div><span class="banner-flower">🌿</span></section>`;
  document.querySelector('#heroWhatsAppButton')?.addEventListener('click', contactWhatsApp);
  const input = document.querySelector('#productSearch'); let timer;
  input?.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(async () => { state.search = input.value.trim(); await fetchCatalog(); renderCurrent(); }, 350); });
  document.querySelector('#clearSearch')?.addEventListener('click', async () => { state.search = ''; await fetchCatalog(); renderCurrent(); });
  document.querySelector('#refreshCatalog')?.addEventListener('click', async () => { await fetchCatalog(); renderCurrent(); });
  document.querySelector('#retryCatalog')?.addEventListener('click', async () => { await fetchCatalog(); renderCurrent(); });
}
function renderCategories() {
  view.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / EXPLORE</span><h1>${t('categories')}</h1><p>${t('heroText')}</p></section><div class="category-grid category-grid-large">${state.categories.length ? state.categories.map(categoryCard).join('') : emptyMarkup(t('noCategories'), t('noProductsText'))}</div><section class="content-section"><div class="section-heading"><div><span class="section-kicker">${state.categoryId ? t('category') : t('allCategories')}</span><h2>${state.categories.find(c => String(c.id) === String(state.categoryId))?displayCategoryName(state.categories.find(c => String(c.id) === String(state.categoryId))) : t('browseProducts')}</h2></div><button class="text-link" id="clearCategory">${t('allCategories')} ×</button></div><div class="product-grid">${state.products.length ? state.products.map(productCard).join('') : emptyMarkup(t('noProducts'), t('noProductsText'))}</div></section>`;
  document.querySelector('#clearCategory')?.addEventListener('click', async () => { state.categoryId = null; state.search = ''; await fetchCatalog(); renderCurrent(); });
}
function renderProductDetail(productId) {
  const product = state.products.find(p => String(p.id) === String(productId));
  if (!product) { toast(t('loadError'), 'error'); navigate('categories'); return; }
  const stock = Number(product.stockQuantity || 0);
  const favorite = state.favorites.map(String).includes(String(product.id));
  view.innerHTML = `<button class="text-link back-link" data-nav="${state.prevPage || 'home'}">← ${t('back')}</button><section class="detail-layout"><div class="detail-image">${imageMarkup(product)}</div><div class="detail-copy"><span class="product-category">${escapeHtml(displayBilingualText(product.categoryName,product.categoryNameAr) || '')}</span><h1>${escapeHtml(displayProductName(product))}</h1><div class="detail-price">${currency(product.price)}</div><p>${escapeHtml(displayProductDescription(product) || t('noDescription'))}</p><div class="detail-availability ${stock>0?'good':'bad'}">${stock>0?'● '+t('available'):'● '+t('outOfStock')} ${stock>0?`· ${stock} ${t('stock').toLowerCase()}`:''}</div><div class="detail-actions">${productCartControl(product,true)}<button type="button" class="favorite-btn detail-favorite-btn ${favorite?'is-favorite':''}" data-favorite-toggle="${product.id}" aria-label="${favorite?t('removeFromPreferences'):t('addToPreferences')}" title="${favorite?t('removeFromPreferences'):t('addToPreferences')}">${favorite?'♥':'♡'} <span>${favorite?t('removeFromPreferences'):t('addToPreferences')}</span></button><button class="btn btn-outline" data-nav="cart">${t('cart')} ♧</button></div><div class="detail-assurance"><span>✓</span> ${t('checkoutNote')}</div></div></section>`;
}
function renderCart() {
  const total = state.cart.reduce((sum,item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0);
  if (!state.cart.length) { view.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / CHECKOUT</span><h1>${t('cart')}</h1></section>${emptyMarkup(t('cartEmpty'), t('cartEmptyText'), `<button class="btn btn-primary" data-nav="categories">${t('continueShopping')} ↗</button>`)}`; return; }
  view.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / CHECKOUT</span><h1>${t('cart')}</h1><p>${state.cart.length} ${t('orderItems').toLowerCase()}</p></section><div class="cart-layout"><section class="cart-list">${state.cart.map(item => `<article class="cart-row">${imageMarkup(item,true)}<div class="cart-row-info"><strong>${escapeHtml(displayProductName(item))}</strong><small>${currency(item.price)} · ${escapeHtml(displayBilingualText(item.categoryName,item.categoryNameAr)||'')}</small><button class="remove-link" data-remove="${item.id}">${t('remove')}</button></div><div class="quantity-control"><button data-qty="${item.id}" data-delta="-1" aria-label="Decrease quantity">−</button><span>${item.quantity}</span><button data-qty="${item.id}" data-delta="1" aria-label="Increase quantity" ${item.stockQuantity && item.quantity>=item.stockQuantity?'disabled':''}>＋</button></div><strong class="line-total">${currency(item.price*item.quantity)}</strong></article>`).join('')}<button class="text-link" data-nav="categories">← ${t('continueShopping')}</button></section><aside class="summary-card"><h2>${t('orderSummary')}</h2><div class="summary-line"><span>${t('subtotal')}</span><span>${currency(total)}</span></div><div class="summary-line"><span>${t('delivery')}</span><span>${t('calculatedAtCheckout')}</span></div><div class="summary-divider"></div><div class="summary-line total"><span>${t('total')}</span><strong>${currency(total)}</strong></div><button class="btn btn-primary btn-full" id="checkoutButton">${t('checkout')} ↗</button><p class="summary-note">✓ ${t('checkoutNote')}</p></aside></div>`;
  document.querySelector('#checkoutButton')?.addEventListener('click', () => { if (!requireUser()) return; openCheckout(); });
}
function renderOrders(target = view) {
  if (!getToken()) { target.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / ACCOUNT</span><h1>${t('orderHistory')}</h1></section>${emptyMarkup(t('loginRequired'), t('noOrdersText'), `<button class="btn btn-primary" id="orderSignIn">${t('signIn')}</button>`)}`; document.querySelector('#orderSignIn')?.addEventListener('click',()=>openAuth('login')); return; }
  target.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / ACCOUNT</span><h1>${t('orderHistory')}</h1><p>${t('noOrdersText')}</p></section><div id="ordersContent">${loadingMarkup()}</div>`;
  loadPage('/orders/my?page=0&size=50').then(orders => { const target=document.querySelector('#ordersContent'); if(!target)return; target.innerHTML = orders.length ? `<div class="orders-table">${orders.map(order => `<article class="order-card"><div class="order-card-head"><strong>${t('orderNumber')} #${escapeHtml(order.orderNumber || order.id)}</strong><span class="status-badge status-${String(order.status).toLowerCase()}">${escapeHtml(order.status)}</span></div><div class="order-meta"><span>${new Date(order.createdAt).toLocaleDateString()}</span><strong>${currency(order.totalAmount)}</strong></div><p>${(order.items||[]).length} ${t('orderItems').toLowerCase()}</p><button class="text-link" data-order-details="${order.id}">${t('viewOrder')} ↗</button></article>`).join('')}</div>` : emptyMarkup(t('noOrders'),t('noOrdersText'),`<button class="btn btn-primary" data-nav="categories">${t('startShopping')}</button>`); }).catch(error => showError(error,document.querySelector('#ordersContent')));
}
function renderProfile(target = view) {
  const user = getUser();
  if (!user) { target.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / ACCOUNT</span><h1>${t('profileTitle')}</h1></section><div class="auth-promo"><span class="auth-mark">ن</span><h2>${t('welcomeBack')}</h2><p>${t('forgotNote')}</p><button class="btn btn-primary" id="profileLogin">${t('signIn')}</button><button class="btn btn-outline" id="profileRegister">${t('createAccount')}</button></div>`; document.querySelector('#profileLogin')?.addEventListener('click',()=>openAuth('login')); document.querySelector('#profileRegister')?.addEventListener('click',()=>openAuth('register')); return; }
  const roleLabels={ADMIN:(getLanguage()==='ar'?'المالك':'Owner'),SUPERVISOR:(getLanguage()==='ar'?'مشرف':'Supervisor'),DELIVERY:(getLanguage()==='ar'?'مندوب توصيل':'Delivery'),CUSTOMER:t('customer')};
  target.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / ACCOUNT</span><h1>${t('profileTitle')}</h1><p>${t('customerTools')}</p></section><form id="profileForm" class="form-card"><div class="avatar-large">${initials(user.name)}</div><label>${t('name')}<input name="name" maxlength="100" required value="${escapeHtml(user.name||'')}"></label><label>${t('email')}<input name="email" type="email" maxlength="190" required value="${escapeHtml(user.email||'')}"></label><label>${t('phone')}<input name="phone" type="tel" inputmode="numeric" pattern="[0-9]{7,20}" maxlength="20" value="${escapeHtml(user.phone||'')}" placeholder="${getLanguage()==='ar'?'أرقام فقط، 7 إلى 20 رقمًا':'Digits only, 7–20 digits'}"></label><label>${t('deliveryAddress')}<textarea name="address" rows="3" maxlength="1500">${escapeHtml(user.address||'')}</textarea></label><label>${t('accountType')}<input value="${escapeHtml(roleLabels[user.role]||user.role||t('customer'))}" disabled></label><button class="btn btn-primary" type="submit">${t('saveChanges')}</button><button class="btn btn-outline" type="button" id="logoutButton">${t('logout')}</button></form><form id="passwordChangeForm" class="form-card password-change-card"><h2>${getLanguage()==='ar'?'تغيير كلمة المرور':'Change password'}</h2><label>${getLanguage()==='ar'?'كلمة المرور الحالية':'Current password'}<input type="password" name="currentPassword" autocomplete="current-password" required></label><label>${getLanguage()==='ar'?'كلمة المرور الجديدة':'New password'}<input type="password" name="newPassword" autocomplete="new-password" minlength="8" maxlength="72" required></label><button class="btn btn-outline" type="submit">${getLanguage()==='ar'?'تحديث كلمة المرور':'Update password'}</button><div class="form-error" id="passwordChangeError"></div></form>`;
  document.querySelector('#profileForm')?.addEventListener('submit', async event => { event.preventDefault(); const form=event.currentTarget; const button=event.submitter; const data=Object.fromEntries(new FormData(form)); if(data.phone && !validPhone(data.phone)){showError(new Error(getLanguage()==='ar'?'رقم الهاتف يجب أن يحتوي على 7 إلى 20 رقمًا فقط.':'Phone must contain 7–20 digits only.'),form);return;} setBusy(button,true); try { const updated=await api('/users/me',{method:'PUT',body:{name:data.name,email:data.email,phone:data.phone,address:data.address}}); localStorage.setItem('nabta_auth_user',JSON.stringify({...user,...updated})); refreshNav(); toast(t('saved')); renderCurrent(); } catch(error){showError(error,form);} finally{setBusy(button,false);} });
  document.querySelector('#passwordChangeForm')?.addEventListener('submit',async event=>{event.preventDefault();const form=event.currentTarget;const data=Object.fromEntries(new FormData(form));const errorNode=form.querySelector('#passwordChangeError');try{await api('/users/me/password',{method:'PUT',body:data});form.reset();toast(getLanguage()==='ar'?'تم تغيير كلمة المرور بنجاح.':'Password updated successfully.');}catch(error){if(errorNode)errorNode.textContent=error.message;}});
  document.querySelector('#logoutButton')?.addEventListener('click',async()=>{await unregisterAdminPushNotifications();clearSession();state.favorites=[];toast(t('logout'));refreshNav();navigate('home');});
}
async function renderSavedAddresses(target = view) {
  const user = getUser();
  if (!user) { target.innerHTML = `<section class="settings-panel"><div class="settings-panel-header"><h2>${t('savedAddressesTab')}</h2></div><div class="auth-promo"><span class="auth-mark">ن</span><h2>${t('loginRequired')}</h2><p>${t('savedAddressNote')}</p><button class="btn btn-primary" id="addressLoginButton">${t('signIn')}</button></div></section>`; target.querySelector('#addressLoginButton')?.addEventListener('click', () => openAuth('login')); return; }
  target.innerHTML = `<section class="settings-panel"><div class="settings-panel-header"><div><h2>${t('savedAddressesTab')}</h2><p>${getLanguage()==='ar'?'تُحفظ هذه العناوين في حسابك وتظهر عند إتمام الطلب.':'Addresses are saved to your account and available at checkout.'}</p></div></div><form id="savedAddressForm" class="saved-address-form"><label>${t('addressTitle')}<input name="label" maxlength="80" required></label><label>${t('addressDetails')}<textarea name="address" rows="3" maxlength="1200" required></textarea></label><label>${t('addressPhone')}<input name="phone" type="tel" inputmode="numeric" pattern="[0-9]{7,20}" maxlength="20" required placeholder="${getLanguage()==='ar'?'أرقام فقط':'Digits only'}"></label><button class="btn btn-primary" type="submit">＋ ${t('addAddress')}</button></form><div id="savedAddressList">${loadingMarkup()}</div></section>`;
  async function refreshAddresses(){const host=target.querySelector('#savedAddressList');if(!host)return;try{const addresses=await loadPage('/users/me/addresses');host.innerHTML=addresses.length?`<div class="saved-address-list">${addresses.map(a=>`<article class="saved-address-card"><div class="saved-address-icon">⌖</div><div class="saved-address-copy"><strong>${escapeHtml(a.label)}</strong><p>${escapeHtml(a.address)}</p><small>${escapeHtml(a.phone||'')}</small></div><button class="btn btn-outline btn-small" type="button" data-delete-address="${a.id}">${t('deleteAddress')}</button></article>`).join('')}</div>`:emptyMarkup(t('noSavedAddresses'),getLanguage()==='ar'?'أضف عنوانًا ليكون متاحًا عند إتمام الطلب.':'Add an address so it is available at checkout.');host.querySelectorAll('[data-delete-address]').forEach(button=>button.addEventListener('click',async()=>{try{await api(`/users/me/addresses/${button.dataset.deleteAddress}`,{method:'DELETE'});toast(t('addressDeleted'));await refreshAddresses();}catch(e){toast(e.message,'error');}}));}catch(error){showError(error,host);}}
  target.querySelector('#savedAddressForm')?.addEventListener('submit',async event=>{event.preventDefault();const form=event.currentTarget;const data=Object.fromEntries(new FormData(form));if(!validPhone(data.phone)){showError(new Error(getLanguage()==='ar'?'يجب إدخال 7 إلى 20 رقمًا فقط.':'Phone must contain 7–20 digits only.'),form);return;}try{await api('/users/me/addresses',{method:'POST',body:{label:data.label.trim(),address:data.address.trim(),phone:data.phone.trim()}});form.reset();toast(t('addressSaved'));await refreshAddresses();}catch(error){showError(error,form);}});
  await refreshAddresses();
}
async function toggleFavorite(productId) {
  if (!requireUser()) return;
  const id=Number(productId); const active=state.favorites.map(Number).includes(id);
  try { if(active) await api(`/users/me/favorites?productId=${id}`,{method:'DELETE'}); else await api('/users/me/favorites',{method:'POST',body:{productId:id}}); state.favorites=active?state.favorites.filter(v=>Number(v)!==id):[...state.favorites,id]; toast(active?t('removedFromPreferences'):t('addedToPreferences')); renderCurrent(); }
  catch(error){toast(error.message,'error');}
}
function renderWishlist(target=view) {
  if(!getUser()){target.innerHTML=emptyMarkup(t('loginRequired'),t('noOrdersText'),`<button class="btn btn-primary" id="wishlistLogin">${t('signIn')}</button>`);target.querySelector('#wishlistLogin')?.addEventListener('click',()=>openAuth('login'));return;}
  const favorites=state.favorites.map(String);const products=state.products.filter(p=>favorites.includes(String(p.id)));
  target.innerHTML=`<section class="settings-panel"><div class="settings-panel-header"><div><h2>${t('myPreferences')}</h2><p>${t('myPreferencesNote')}</p></div><button class="btn btn-outline" data-nav="categories">${t('browseProducts')}</button></div><div class="product-grid">${products.length?products.map(productCard).join(''):emptyMarkup(t('noPreferences'),t('myPreferencesNote'))}</div></section>`;
}
function notificationStatusMessage(status) {
  const ar = getLanguage() === 'ar';
  const messages = {
    'not-native': ar ? 'لتفعيل إشعارات الطلبات، افتح تطبيق نبته المثبّت على جهاز Android.' : 'Open the installed Nabta Android app to enable order notifications.',
    'admin-only': ar ? 'سجّل الدخول بحساب المالك أو المشرف أو مندوب التوصيل لتفعيل الإشعارات.' : 'Sign in with an Owner, Supervisor, or Delivery account to enable notifications.',
    'denied': ar ? 'الإشعارات محظورة. افتح إعدادات Android ← التطبيقات ← Nabta ← الإشعارات واسمح بها.' : 'Notifications are blocked. Open Android Settings → Apps → Nabta → Notifications and allow them.',
    'permission-needed': ar ? 'اضغط تفعيل الإشعارات واسمح بها إذا طلب Android ذلك.' : 'Tap Enable notifications and allow them if Android asks.',
    'permitted-not-registered': ar ? 'السماح بالإشعارات متاح، لكن الجهاز لم يسجّل بعد. اضغط تفعيل إشعارات الطلبات.' : 'Permission is available, but this device is not registered yet. Tap Enable order notifications.',
    'token-not-saved': ar ? 'تم إنشاء رمز الإشعارات لكن لم يُحفظ في الخادم بعد. اضغط تفعيل مرة أخرى.' : 'A notification token exists but has not been saved on the server. Tap Enable again.',
    'registered': ar ? 'إشعارات الطلبات مفعّلة لهذا الجهاز.' : 'Order notifications are enabled for this device.'
  };
  return messages[status] || (ar ? 'جارٍ التحقق من حالة الإشعارات…' : 'Checking notification status…');
}
async function updateAdminPushStatus() {
  const node = document.querySelector('#adminPushStatus');
  if (!node) return;
  try {
    const status = await getAdminPushNotificationStatus();
    node.textContent = notificationStatusMessage(status.state);
  } catch {
    node.textContent = getLanguage() === 'ar' ? 'تعذّر التحقق من الإشعارات.' : 'Could not check notification status.';
  }
}
function renderPreferences(target = view) {
  const languageButtonText = getLanguage() === 'ar' ? 'English' : 'العربية';
  const isAdmin = getUser()?.role === 'ADMIN';
  const canReceiveOperationsPush = isOperationsRole();
  const ar = getLanguage() === 'ar';
  target.innerHTML = `<section class="settings-panel">
    <div class="settings-panel-header"><div><h2>${t('generalPreferencesTab')}</h2><p>${t('accountSettingsIntro')}</p></div></div>
    <div class="preference-row"><div><strong>${t('languagePreference')}</strong><small>${getLanguage() === 'ar' ? 'العربية' : 'English'}</small></div><button class="btn btn-outline" id="settingsLanguageToggle" type="button">${languageButtonText}</button></div>
    ${canReceiveOperationsPush ? `<div class="preference-row admin-push-preference" style="margin-top:14px;align-items:flex-start;gap:16px"><div style="flex:1;min-width:0"><strong>${ar ? 'إشعارات الطلبات' : 'Order notifications'}</strong><small id="adminPushStatus" style="display:block;white-space:normal;line-height:1.5;margin-top:6px">${ar ? 'جارٍ التحقق من الإشعارات…' : 'Checking notification status…'}</small></div><button class="btn btn-primary" id="enableAdminNotifications" type="button">${ar ? 'تفعيل الإشعارات' : 'Enable notifications'}</button></div>` : ''}
  </section>`;
  target.querySelector('#settingsLanguageToggle')?.addEventListener('click', async () => {
    setLanguage(getLanguage() === 'ar' ? 'en' : 'ar');
    const button = document.querySelector('#languageButton');
    if (button) button.textContent = getLanguage() === 'ar' ? 'English' : 'عربي';
    applyLanguage();
    await fetchCatalog();
    renderCurrent();
  });
  if (canReceiveOperationsPush) {
    void updateAdminPushStatus();
    target.querySelector('#enableAdminNotifications')?.addEventListener('click', async (event) => {
      const button = event.currentTarget;
      const status = document.querySelector('#adminPushStatus');
      button.disabled = true;
      button.textContent = getLanguage() === 'ar' ? 'جارٍ التفعيل…' : 'Enabling…';
      if (status) status.textContent = getLanguage() === 'ar' ? 'جارٍ طلب إذن الإشعارات وتسجيل الجهاز…' : 'Requesting notification access and registering this device…';
      try {
        const result = await registerAdminPushNotifications({ force: true });
        if (!result?.ok) {
          if (status) status.textContent = result?.reason === 'permission-denied'
            ? notificationStatusMessage('denied')
            : result?.reason === 'not-native' ? notificationStatusMessage('not-native')
            : result?.reason === 'admin-only' ? notificationStatusMessage('admin-only')
            : (getLanguage() === 'ar' ? 'تعذّر بدء تسجيل الإشعارات. تأكد من إعداد Firebase ثم أعد المحاولة.' : 'Could not start notification registration. Check Firebase setup and try again.');
        } else if (status) {
          status.textContent = getLanguage() === 'ar' ? 'تم بدء التسجيل. انتظر لحظات للتحقق من حالة الجهاز.' : 'Registration started. Wait a moment while the device is checked.';
        }
        window.setTimeout(() => { void updateAdminPushStatus(); }, 1500);
      } finally {
        button.disabled = false;
        button.textContent = getLanguage() === 'ar' ? 'تفعيل الإشعارات' : 'Enable notifications';
      }
    });
  }
}
function renderSettingsTab() {
  const tabs = ['profile', 'orders', 'addresses', 'wishlist', 'preferences', 'about'];
  if (!tabs.includes(state.settingsTab)) state.settingsTab = 'profile';
  document.querySelectorAll('[data-settings-tab]').forEach(button => {
    const active = button.dataset.settingsTab === state.settingsTab;
    button.classList.toggle('active', active);
    button.setAttribute('aria-selected', active ? 'true' : 'false');
    button.tabIndex = active ? 0 : -1;
  });
  const target = document.querySelector('#settingsTabContent');
  if (!target) return;
  if (state.settingsTab === 'profile') renderProfile(target);
  else if (state.settingsTab === 'orders') renderOrders(target);
  else if (state.settingsTab === 'addresses') void renderSavedAddresses(target);
  else if (state.settingsTab === 'wishlist') renderWishlist(target);
  else if (state.settingsTab === 'about') renderAbout(target);
  else renderPreferences(target);
}
function renderSettings() {
  const isArabic = getLanguage() === 'ar';
  view.innerHTML = `<section class="page-heading settings-page-heading"><div><span class="section-kicker">NABTA / ACCOUNT</span><h1>${t('accountSettingsTitle')}</h1><p>${t('accountSettingsIntro')}</p></div></section>
    <section class="account-settings-shell">
      <nav class="account-settings-tabs" role="tablist" aria-label="${isArabic ? 'إعدادات الحساب' : 'Account settings'}">
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="profile">♙ <span>${t('profileTab')}</span></button>
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="orders">▣ <span>${t('previousOrdersTab')}</span></button>
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="addresses">⌖ <span>${t('savedAddressesTab')}</span></button>
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="wishlist">♡ <span>${t('myPreferences')}</span></button>
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="preferences">⚙ <span>${t('generalPreferencesTab')}</span></button>
        <button class="account-settings-tab" type="button" role="tab" data-settings-tab="about">ⓘ <span>${isArabic ? 'عن التطبيق' : 'About Us'}</span></button>
      </nav>
      <div id="settingsTabContent" class="settings-tab-content" role="tabpanel"></div>
    </section>`;
  renderSettingsTab();
}

function renderAdmin() {
  if (getUser()?.role !== 'ADMIN') { view.innerHTML = `<section class="page-heading"><h1>${t('adminTitle')}</h1></section>${emptyMarkup(t('adminOnly'),t('signInTitle'),`<button class="btn btn-primary" id="adminLogin">${t('signIn')}</button>`)}`; document.querySelector('#adminLogin')?.addEventListener('click',()=>openAuth('login')); return; }
  const tabs = [['dashboard',t('adminTitle')],['products',t('productsManagement')],['categories',t('categories')],['orders',t('orderManagement')],['inventory',t('inventory')],['users',t('usersManagement')],['company',t('companySettings')]];
  view.innerHTML = `<section class="page-heading admin-heading"><div><span class="section-kicker">NABTA / ADMINISTRATION</span><h1>${t('adminTitle')}</h1><p>${t('adminTools')}</p></div><div style="display:flex;gap:8px;flex-wrap:wrap"><button class="btn btn-outline" id="openJspCatalogue">📄 JSP Catalogue</button><button class="btn btn-outline" id="adminRefresh">↻ ${t('refresh')}</button></div></section><div class="admin-tabs">${tabs.map(([id,label])=>`<button data-admin-tab="${id}" class="${state.adminTab===id?'active':''}">${escapeHtml(label)}</button>`).join('')}</div><div id="adminContent">${loadingMarkup()}</div>`;
  document.querySelector('#adminRefresh')?.addEventListener('click',renderAdminData);
    document.querySelector('#openJspCatalogue')?.addEventListener('click',()=>window.open(getApiBase().replace(/\/api$/,'')+'/academic/jsp/products','_blank'));
  renderAdminData();
}
async function renderAdminData() {
  const target = document.querySelector('#adminContent'); if (!target) return; target.innerHTML=loadingMarkup();
  try {
    if(state.adminTab==='dashboard'){
      const [stats,orders]=await Promise.all([api('/admin/dashboard'),loadPage('/admin/orders?page=0&size=5')]);
      const fields=[['totalOrders',stats.totalOrders],['pendingOrders',stats.pendingOrders],['completedOrders',stats.completedOrders],['customers',stats.registeredCustomers],['activeProducts',stats.activeProducts],['lowStock',stats.lowStockProducts]];
      target.innerHTML=`<div class="stats-grid">${fields.map(([label,value],i)=>`<div class="stat-card"><span class="stat-icon">${['▤','◷','✓','♙','✿','⚠'][i]}</span><small>${t(label)}</small><strong>${value ?? 0}</strong></div>`).join('')}</div><section class="admin-panel"><div class="panel-head"><h2>${t('currentOrders')}</h2><button class="text-link" data-admin-tab="orders">${t('viewAll')} ↗</button></div>${orders.length?orders.map(order=>`<div class="mini-row"><div><strong>#${escapeHtml(order.orderNumber||order.id)}</strong><small>${escapeHtml(order.status)} · ${new Date(order.createdAt).toLocaleDateString()}</small></div><strong>${currency(order.totalAmount)}</strong></div>`).join(''):emptyMarkup(t('noOrders'),t('noData'))}</section><section class="admin-panel"><div class="panel-head"><h2>${t('orderManagement')}</h2><strong>${currency(stats.completedOrderValue||0)}</strong></div><p class="muted">${t('completedOrders')}: ${stats.completedOrders||0}</p></section>`;
    } else if(state.adminTab==='products') await renderAdminProducts(target);
    else if(state.adminTab==='categories') await renderAdminCategories(target);
    else if(state.adminTab==='orders') await renderAdminOrders(target);
    else if(state.adminTab==='inventory') await renderInventory(target);
    else if(state.adminTab==='users') await renderUsers(target);
    else if(state.adminTab==='company') await renderCompany(target);
  } catch(error) { showError(error,target); }
}
async function uploadMedia(file) {
  if (!file || !['image/png','image/jpeg'].includes(file.type)) throw new Error(getLanguage()==='ar'?'يرجى اختيار صورة PNG أو JPEG.':'Choose a PNG or JPEG image.');
  if (file.size > 5 * 1024 * 1024) throw new Error(getLanguage()==='ar'?'يجب ألا يتجاوز حجم الصورة 5 ميغابايت.':'Image must be 5 MB or smaller.');
  const headers = new Headers(); if (getToken()) headers.set('Authorization', `Bearer ${getToken()}`);
  const response = await fetch(`${getApiBase()}/admin/media`, { method:'POST', headers, body:(()=>{const fd=new FormData();fd.append('file',file);return fd;})() });
  const result = await response.json().catch(()=>({}));
  if (!response.ok) throw new Error(result.message || result.error || `Image upload failed (${response.status}).`);
  return result;
}
async function renderAdminProducts(target) {
  const products=await loadPage('/admin/products?page=0&size=100'); const categories=await api('/admin/categories');
  target.innerHTML=`<div class="panel-head"><h2>${t('productsManagement')}</h2><button class="btn btn-primary" id="newProductButton">＋ ${t('newProduct')}</button></div><div id="productFormHost"></div><div class="admin-list">${products.map(p=>`<article class="admin-list-row">${imageMarkup(p,true)}<div class="admin-list-copy"><strong>${escapeHtml(displayProductName(p))}</strong><small>${escapeHtml(displayBilingualText(p.categoryName,p.categoryNameAr)||'')} · ${currency(p.price)} · ${t('stock')}: ${p.stockQuantity}</small></div><button class="btn btn-outline btn-small" data-edit-product="${p.id}">${t('edit')}</button><button class="btn btn-ghost btn-small" data-deactivate-product="${p.id}">${t('deactivate')}</button></article>`).join('')||emptyMarkup(t('noProducts'),t('noProductsText'))}</div>`;
  document.querySelector('#newProductButton')?.addEventListener('click',()=>renderProductForm(categories,null));
  products.forEach(p=>document.querySelector(`[data-edit-product="${p.id}"]`)?.addEventListener('click',()=>renderProductForm(categories,p)));
  products.forEach(p=>document.querySelector(`[data-deactivate-product="${p.id}"]`)?.addEventListener('click',async()=>{if(!confirm(t('confirmDeactivate')))return;try{await api(`/admin/products/${p.id}`,{method:'DELETE'});toast(t('saved'));renderAdminData();}catch(e){toast(e.message,'error');}}));
}
function renderProductForm(categories, product) {
  const host=document.querySelector('#productFormHost'); if(!host)return;
  const nameFields=bilingualFormFields({label:t('productName'),nameEn:'name',nameAr:'nameAr',valueEn:product?.name||'',valueAr:product?.nameAr||'',required:true,maxLength:150});
  const descriptionFields=bilingualFormFields({label:t('description'),nameEn:'description',nameAr:'descriptionAr',valueEn:product?.description||'',valueAr:product?.descriptionAr||'',type:'textarea',rows:3,className:'span-two'});
  host.innerHTML=`<form id="adminProductForm" class="admin-form"><h3>${product?t('edit'):t('newProduct')}</h3><div class="form-grid">${nameFields}<label>${t('category')}<select name="categoryId" required><option value="">${t('categoryOptional')}</option>${categories.map(c=>`<option value="${c.id}" ${String(c.id)===String(product?.categoryId)?'selected':''}>${escapeHtml(displayCategoryName(c))}</option>`).join('')}</select></label><label>${t('price')}<input name="price" type="number" min="0.01" step="0.01" required value="${escapeHtml(product?.price??'')}"></label><label>${t('stock')}<input name="stockQuantity" type="number" min="0" step="1" required value="${escapeHtml(product?.stockQuantity??0)}"></label><label class="span-two">${t('imageOptional')} — URL<input name="imageUrl" type="url" value="${escapeHtml(product?.imageUrl||'')}"></label><label class="span-two">${getLanguage()==='ar'?'أو ارفع صورة من الجهاز':'Or upload an image from this device'}<input name="imageFile" type="file" accept="image/png,image/jpeg"></label>${descriptionFields}</div><div class="form-actions"><button class="btn btn-primary" type="submit">${t('saveProduct')}</button><button class="btn btn-outline" type="button" id="cancelProductForm">${t('cancel')}</button></div></form>`;
  document.querySelector('#cancelProductForm')?.addEventListener('click',()=>host.replaceChildren());
  document.querySelector('#adminProductForm')?.addEventListener('submit',async e=>{e.preventDefault();const form=e.currentTarget;const data=Object.fromEntries(new FormData(form));const button=form.querySelector('button[type=submit]');setBusy(button,true);try{let imageUrl=data.imageUrl||null;const file=form.querySelector('input[name=imageFile]')?.files?.[0];if(file){const uploaded=await uploadMedia(file);imageUrl=uploaded.url;}const payload={name:data.name.trim(),nameAr:data.nameAr.trim(),description:data.description.trim()||null,descriptionAr:data.descriptionAr.trim()||null,price:Number(data.price),imageUrl,categoryId:Number(data.categoryId),stockQuantity:Number(data.stockQuantity),active:product?.active??true};await api(product?`/admin/products/${product.id}`:'/admin/products',{method:product?'PUT':'POST',body:payload});toast(t('productSaved'));await fetchCatalog();renderAdminData();}catch(error){showError(error,form);}finally{setBusy(button,false);}});
}
async function renderAdminCategories(target) {
  const cats=await api('/admin/categories');
  const createNameFields=bilingualFormFields({label:t('categoryName'),nameEn:'name',nameAr:'nameAr',required:true,maxLength:100});
  const createDescriptionFields=bilingualFormFields({label:t('descriptionOptional'),nameEn:'description',nameAr:'descriptionAr',type:'textarea',rows:2,maxLength:1000,className:'span-two'});
  target.innerHTML=`<section class="admin-panel"><h2>${t('createCategory')}</h2><form id="categoryForm" class="admin-form"><div class="form-grid">${createNameFields}${createDescriptionFields}<label class="span-two">${t('imageOptional')}<input name="imageUrl" type="url"></label></div><button class="btn btn-primary" type="submit">${t('saveCategory')}</button></form><div id="categoryEditHost"></div></section><section class="admin-panel"><h2>${t('categories')}</h2>${cats.map(c=>`<div class="mini-row"><div><strong>${escapeHtml(displayCategoryName(c))}</strong><small>${escapeHtml(displayText(c.description,c.descriptionAr)||'')}</small></div><span class="status-badge ${c.active?'status-completed':'status-cancelled'}">${c.active?t('active'):t('disabled')}</span><button class="btn btn-outline btn-small" data-edit-category="${c.id}">${t('edit')}</button><button class="btn btn-ghost btn-small" data-deactivate-category="${c.id}">${t('deactivate')}</button></div>`).join('')||emptyMarkup(t('noCategories'),t('noData'))}</section>`;
  document.querySelector('#categoryForm')?.addEventListener('submit',async e=>{e.preventDefault();const data=Object.fromEntries(new FormData(e.currentTarget));try{await api('/admin/categories',{method:'POST',body:{name:data.name.trim(),nameAr:data.nameAr.trim(),description:data.description.trim()||null,descriptionAr:data.descriptionAr.trim()||null,imageUrl:data.imageUrl||null,active:true}});toast(t('categoryCreated'));await fetchCatalog();renderAdminData();}catch(error){showError(error,e.currentTarget);}});
  cats.forEach(c=>document.querySelector(`[data-edit-category="${c.id}"]`)?.addEventListener('click',()=>{
    const host=document.querySelector('#categoryEditHost');
    const nameFields=bilingualFormFields({label:t('categoryName'),nameEn:'name',nameAr:'nameAr',valueEn:c.name||'',valueAr:c.nameAr||'',required:true,maxLength:100});
    const descriptionFields=bilingualFormFields({label:t('descriptionOptional'),nameEn:'description',nameAr:'descriptionAr',valueEn:c.description||'',valueAr:c.descriptionAr||'',type:'textarea',rows:2,maxLength:1000,className:'span-two'});
    host.innerHTML=`<form id="categoryEditForm" class="admin-form"><h3>${t('edit')} ${escapeHtml(displayCategoryName(c))}</h3><div class="form-grid">${nameFields}${descriptionFields}<label class="span-two">${t('imageOptional')}<input name="imageUrl" type="url" value="${escapeHtml(c.imageUrl||'')}"></label></div><button class="btn btn-primary" type="submit">${t('saveCategory')}</button><button class="btn btn-outline" type="button" id="cancelEditCategory">${t('cancel')}</button></form>`;
    host.querySelector('#cancelEditCategory').addEventListener('click',()=>host.replaceChildren());
    host.querySelector('form').addEventListener('submit',async e=>{e.preventDefault();const d=Object.fromEntries(new FormData(e.currentTarget));try{await api(`/admin/categories/${c.id}`,{method:'PUT',body:{name:d.name.trim(),nameAr:d.nameAr.trim(),description:d.description.trim()||null,descriptionAr:d.descriptionAr.trim()||null,imageUrl:d.imageUrl||null,active:c.active}});toast(t('saved'));await fetchCatalog();renderAdminData();}catch(error){showError(error,e.currentTarget);}});
  }));
  cats.forEach(c=>document.querySelector(`[data-deactivate-category="${c.id}"]`)?.addEventListener('click',async()=>{if(!confirm(t('confirmDeactivate')))return;try{await api(`/admin/categories/${c.id}`,{method:'DELETE'});await fetchCatalog();renderAdminData();}catch(e){toast(e.message,'error');}}));
}
async function renderAdminOrders(target) {
  const orders=await loadPage('/admin/orders?page=0&size=100');
  target.innerHTML=`<section class="admin-panel"><div class="panel-head"><h2>${t('orderManagement')}</h2><select id="statusFilter"><option value="">${t('allStatuses')}</option>${['PENDING','CONFIRMED','PROCESSING','OUT_FOR_DELIVERY','PARTIALLY_DELIVERED','COMPLETED','CANCELLED'].map(s=>`<option>${s}</option>`).join('')}</select></div><div id="adminOrdersList">${orders.map(order=>adminOrderMarkup(order,'owner')).join('')||emptyMarkup(t('noOrders'),t('noData'))}</div></section>`;
  document.querySelector('#statusFilter')?.addEventListener('change',async e=>{const filtered=e.target.value?await loadPage(`/admin/orders?status=${encodeURIComponent(e.target.value)}&page=0&size=100`):orders;document.querySelector('#adminOrdersList').innerHTML=filtered.map(order=>adminOrderMarkup(order,'owner')).join('')||emptyMarkup(t('noOrders'),t('noData'));wireStatusButtons(filtered,'owner');wireDispatchButtons(filtered,'owner');}); wireStatusButtons(orders,'owner');wireDispatchButtons(orders,'owner');
}
function statusOptions(role,current){let options=role==='supervisor'?['PROCESSING','CANCELLED']:['PENDING','CONFIRMED','PROCESSING','CANCELLED'];if(current==='COMPLETED'||current==='CANCELLED')options=[current];else if(current==='OUT_FOR_DELIVERY')options=['OUT_FOR_DELIVERY'];return [...new Set([current,...options])];}
function adminOrderMarkup(order,role='owner') {const name=order.customerName||order.customerEmail||'';const allowed=statusOptions(role,order.status);return `<article class="order-card" data-order-card="${order.id}"><div class="order-card-head"><strong>#${escapeHtml(order.orderNumber||order.id)}${name?` · ${escapeHtml(name)}`:''}</strong><span class="status-badge status-${String(order.status).toLowerCase()}">${escapeHtml(order.status)}</span></div><div class="order-meta"><span>${new Date(order.createdAt).toLocaleString()}</span><strong>${currency(order.totalAmount)}</strong></div><p>${escapeHtml(order.contactPhone||'')} · ${escapeHtml(order.deliveryAddress||'')}</p><small>${(order.items||[]).map(i=>`${escapeHtml(displayBilingualText(i.productName,i.productNameAr))} × ${i.quantity}`).join(', ')}</small><div class="order-update"><select data-next-status="${order.id}" ${['COMPLETED','CANCELLED','OUT_FOR_DELIVERY'].includes(order.status)?'disabled':''}>${allowed.map(s=>`<option value="${s}" ${s===order.status?'selected':''}>${s}</option>`).join('')}</select><button class="btn btn-outline btn-small" data-update-status="${order.id}" ${['COMPLETED','CANCELLED','OUT_FOR_DELIVERY'].includes(order.status)?'disabled':''}>${t('updateStatus')}</button>${['PROCESSING','PARTIALLY_DELIVERED'].includes(order.status)?`<button class="btn btn-primary btn-small" data-dispatch-order="${order.id}">${getLanguage()==='ar'?'تعيين توصيل':'Assign delivery'}</button>`:''}</div><div class="order-audit"><button class="btn btn-outline btn-small" data-order-history="${order.id}">${getLanguage()==='ar'?'سجل حالة الطلب':'View status history'}</button></div></article>`;}
function wireStatusButtons(orders,role='owner'){orders.forEach(o=>document.querySelector(`[data-update-status="${o.id}"]`)?.addEventListener('click',async()=>{const status=document.querySelector(`[data-next-status="${o.id}"]`)?.value;try{await api(`/${role==='supervisor'?'supervisor':'admin'}/orders/${o.id}/status`,{method:'PUT',body:{status}});toast(t('saved'));if(role==='supervisor')await renderOperationsData();else await renderAdminData();}catch(e){toast(e.message,'error');}}));}
function wireDispatchButtons(orders,role='owner'){orders.forEach(o=>document.querySelector(`[data-dispatch-order="${o.id}"]`)?.addEventListener('click',()=>openDispatchForm(o,role)));}
async function openDispatchForm(order,role='owner') {
  try { const users=role==='supervisor'?await api('/supervisor/delivery-users'):await api('/admin/users');const delivery=users.filter(u=>u.role==='DELIVERY'&&u.status==='ACTIVE');if(!delivery.length){toast(getLanguage()==='ar'?'لا يوجد مندوب توصيل نشط. قم بترقية حساب أولاً.':'No active Delivery account. Promote an account first.','error');return;}
    const assigned=await loadPage(`/${role==='supervisor'?'supervisor':'admin'}/assignments?page=0&size=100`);const used={};assigned.filter(a=>a.orderId===order.id&&a.status!=='CANCELLED').forEach(a=>(a.items||[]).forEach(i=>used[i.orderItemId]=(used[i.orderItemId]||0)+i.assignedQuantity));const pending=(order.items||[]).map(i=>({...i,remaining:Math.max(0,Number(i.quantity)-Number(used[i.orderItemId]||0))})).filter(i=>i.remaining>0);
    const ar=getLanguage()==='ar';openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / DELIVERY</span><h2>${ar?'تعيين توصيل للطلب':'Assign delivery for order'} #${escapeHtml(order.orderNumber||order.id)}</h2><p>${ar?'اختر مندوبًا والعناصر التي سترسل.':'Choose a delivery person and the items to dispatch.'}</p></div><button class="modal-close" data-close-modal>×</button></div><form id="dispatchForm" class="auth-form"><label>${ar?'مندوب التوصيل':'Delivery person'}<select name="deliveryUserId" required>${delivery.map(u=>`<option value="${u.id}">${escapeHtml(u.name)} · ${escapeHtml(u.email)}</option>`).join('')}</select></label><fieldset class="dispatch-choice"><legend>${ar?'العناصر المرسلة':'Items to dispatch'}</legend><label class="dispatch-radio"><input type="radio" name="dispatchMode" value="all" checked> ${ar?'كل العناصر المتبقية':'All remaining items'}</label><label class="dispatch-radio"><input type="radio" name="dispatchMode" value="selected"> ${ar?'اختيار عناصر محددة':'Choose selected items'}</label><div class="dispatch-items">${pending.map(i=>`<label class="dispatch-item"><span><input type="checkbox" name="item_${i.orderItemId}" value="${i.orderItemId}" disabled> ${escapeHtml(displayBilingualText(i.productName,i.productNameAr))} <small>${ar?'المتبقي':'Remaining'}: ${i.remaining}</small></span><input type="number" min="1" max="${i.remaining}" name="qty_${i.orderItemId}" value="${i.remaining}" disabled aria-label="quantity"></label>`).join('')||`<p>${ar?'لا توجد عناصر متبقية':'No remaining items'}</p>`}</div></fieldset><label>${ar?'ملاحظات':'Delivery note'}<textarea name="note" rows="2" maxlength="1000"></textarea></label><div id="dispatchError" class="form-error"></div><button type="submit" class="btn btn-primary btn-full" ${pending.length?'':'disabled'}>${ar?'تأكيد التعيين':'Assign delivery'}</button></form>`,true);
    const form=document.querySelector('#dispatchForm');form?.querySelectorAll('[name=dispatchMode]').forEach(r=>r.addEventListener('change',()=>{const partial=r.value==='selected'&&r.checked;form.querySelectorAll('.dispatch-item input').forEach(input=>input.disabled=!partial);}));form?.addEventListener('submit',async e=>{e.preventDefault();const data=new FormData(form);const selected=data.get('dispatchMode')==='selected';const items=pending.filter(i=>selected&&data.get(`item_${i.orderItemId}`)).map(i=>({orderItemId:Number(i.orderItemId),quantity:Number(data.get(`qty_${i.orderItemId}`))}));if(selected&&!items.length){form.querySelector('#dispatchError').textContent=ar?'اختر عنصرًا واحدًا على الأقل.':'Choose at least one item.';return;}const payload={deliveryUserId:Number(data.get('deliveryUserId')),sendAllPendingItems:!selected,note:String(data.get('note')||'').trim()||null,items:selected?items:[]};const button=form.querySelector('button[type=submit]');setBusy(button,true);try{await api(`/${role==='supervisor'?'supervisor':'admin'}/orders/${order.id}/dispatch`,{method:'POST',body:payload});closeModal();toast(ar?'تم تعيين التوصيل.':'Delivery assigned.');if(role==='supervisor')await renderOperationsData();else await renderAdminData();}catch(err){form.querySelector('#dispatchError').textContent=err.message;}finally{setBusy(button,false);}});
  }catch(error){toast(error.message,'error');}
}
function renderOperations(){const role=getUser()?.role;if(!['SUPERVISOR','DELIVERY'].includes(role)){view.innerHTML=`<section class="page-heading"><h1>${getLanguage()==='ar'?'العمليات':'Operations'}</h1></section>${emptyMarkup(t('adminOnly'),t('loginRequired'))}`;return;}const tabs=role==='SUPERVISOR'?[['orders',getLanguage()==='ar'?'الطلبات':'Orders'],['assignments',getLanguage()==='ar'?'التوصيلات':'Deliveries']]:[['assignments',getLanguage()==='ar'?'التوصيلات المسندة إليّ':'My assigned deliveries']];view.innerHTML=`<section class="page-heading"><span class="section-kicker">NABTA / OPERATIONS</span><h1>${role==='SUPERVISOR'?(getLanguage()==='ar'?'مساحة المشرف':'Supervisor workspace'):(getLanguage()==='ar'?'مساحة التوصيل':'Delivery workspace')}</h1><p>${role==='SUPERVISOR'?(getLanguage()==='ar'?'راجع الطلبات وأدر التوصيلات.':'Review orders and manage dispatches.'):(getLanguage()==='ar'?'اقبل التوصيلات المسندة إليك وأكد التسليم.':'Accept your assigned deliveries and confirm delivery.')}</p></section><div class="admin-tabs">${tabs.map(([id,label])=>`<button type="button" data-operations-tab="${id}" class="${state.operationsTab===id?'active':''}">${label}</button>`).join('')}<button type="button" id="operationsRefresh">↻ ${t('refresh')}</button></div><div id="operationsContent">${loadingMarkup()}</div>`;document.querySelector('#operationsRefresh')?.addEventListener('click',renderOperationsData);void renderOperationsData();}
async function renderOperationsData(){const target=document.querySelector('#operationsContent');if(!target)return;target.innerHTML=loadingMarkup();const role=getUser()?.role;try{if(role==='DELIVERY'){const assignments=await loadPage('/delivery/assignments?page=0&size=100');target.innerHTML=renderAssignmentCards(assignments,'delivery');wireDeliveryAssignmentButtons(assignments);return;}if(role!=='SUPERVISOR')return;if(state.operationsTab==='assignments'){const assignments=await loadPage('/supervisor/assignments?page=0&size=100');target.innerHTML=renderAssignmentCards(assignments,'supervisor');wireAssignmentCancelButtons(assignments);return;}const orders=await loadPage('/supervisor/orders?page=0&size=100');target.innerHTML=orders.map(order=>adminOrderMarkup(order,'supervisor')).join('')||emptyMarkup(t('noOrders'),t('noData'));wireStatusButtons(orders,'supervisor');wireDispatchButtons(orders,'supervisor');}catch(error){showError(error,target);}}
function renderAssignmentCards(assignments,role){const ar=getLanguage()==='ar';return assignments.length?`<div class="orders-table">${assignments.map(a=>`<article class="order-card"><div class="order-card-head"><strong>#${escapeHtml(a.orderNumber||a.orderId)} · ${ar?'التوصيل':'Delivery'} #${a.id}</strong><span class="status-badge status-${String(a.status).toLowerCase()}">${escapeHtml(a.status)}</span></div><p>${ar?'العناصر':'Items'}: ${(a.items||[]).map(i=>`${escapeHtml(displayBilingualText(i.productName,i.productNameAr))} × ${i.assignedQuantity}`).join(', ')}</p>${a.note?`<p>${escapeHtml(a.note)}</p>`:''}${a.status==='ASSIGNED'&&role==='delivery'?`<button class="btn btn-primary btn-small" data-accept-assignment="${a.id}">${ar?'تأكيد استلام التوصيل':'Accept delivery'}</button>`:''}${a.status==='ACCEPTED'&&role==='delivery'?`<button class="btn btn-primary btn-small" data-complete-assignment="${a.id}">${ar?'تم التسليم':'Mark delivered'}</button>`:''}${a.status==='ASSIGNED'&&role==='supervisor'?`<button class="btn btn-outline btn-small" data-cancel-assignment="${a.id}">${ar?'إلغاء التعيين':'Cancel assignment'}</button>`:''}</article>`).join('')}</div>`:emptyMarkup(ar?'لا توجد توصيلات':'No deliveries',ar?'ستظهر التوصيلات المسندة هنا.':'Assigned deliveries will appear here.');}
function wireDeliveryAssignmentButtons(assignments){assignments.forEach(a=>{document.querySelector(`[data-accept-assignment="${a.id}"]`)?.addEventListener('click',async()=>{try{await api(`/delivery/assignments/${a.id}/accept`,{method:'POST'});toast(getLanguage()==='ar'?'تم تأكيد استلام التوصيل.':'Delivery accepted.');await renderOperationsData();}catch(e){toast(e.message,'error');}});document.querySelector(`[data-complete-assignment="${a.id}"]`)?.addEventListener('click',async()=>{if(!confirm(getLanguage()==='ar'?'تأكيد تسليم العناصر المحددة؟':'Confirm that these items have been delivered?'))return;try{await api(`/delivery/assignments/${a.id}/complete`,{method:'POST'});toast(getLanguage()==='ar'?'تم تسجيل التسليم.':'Delivery marked completed.');await renderOperationsData();}catch(e){toast(e.message,'error');}});});}
function wireAssignmentCancelButtons(assignments){assignments.forEach(a=>document.querySelector(`[data-cancel-assignment="${a.id}"]`)?.addEventListener('click',async()=>{if(!confirm(getLanguage()==='ar'?'إلغاء تعيين التوصيل؟':'Cancel this delivery assignment?'))return;try{await api(`/supervisor/assignments/${a.id}/cancel`,{method:'POST'});toast(t('saved'));await renderOperationsData();}catch(e){toast(e.message,'error');}}));}
async function renderInventory(target){const items=await api('/admin/inventory');target.innerHTML=`<section class="admin-panel"><div class="panel-head"><h2>${t('inventory')}</h2><span class="muted">${items.length} ${t('activeProducts').toLowerCase()}</span></div>${items.map(item=>`<form class="inventory-row" data-stock-form="${item.productId}"><div><strong>${escapeHtml(displayBilingualText(item.productName,item.productNameAr))}</strong><small>${item.lowStock?`⚠ ${t('lowStock')}`:t('stock')+': '+item.stockQuantity}</small></div><input aria-label="${t('stock')}" name="quantity" type="number" min="0" value="${item.stockQuantity}" required><button class="btn btn-outline btn-small" type="submit">${t('save')}</button></form>`).join('')||emptyMarkup(t('noProducts'),t('noData'))}</section>`;items.forEach(item=>document.querySelector(`[data-stock-form="${item.productId}"]`)?.addEventListener('submit',async e=>{e.preventDefault();const quantity=Number(new FormData(e.currentTarget).get('quantity'));try{await api(`/admin/inventory/${item.productId}?quantity=${quantity}`,{method:'PUT'});toast(t('stockUpdated'));renderAdminData();}catch(error){showError(error,e.currentTarget);}}));}
async function renderUsers(target){const users=await api('/admin/users');target.innerHTML=`<section class="admin-panel"><h2>${t('usersManagement')}</h2><p class="form-note">${getLanguage()==='ar'?'يمكن للمالك فقط ترقية الحسابات إلى مشرف أو مندوب توصيل.':'Only the Owner can promote accounts to Supervisor or Delivery.'}</p>${users.map(u=>`<article class="mini-row user-admin-row"><div class="admin-list-copy"><strong>${escapeHtml(u.name)}</strong><small>${escapeHtml(u.email)} · ${escapeHtml(u.phone||'—')} · ${escapeHtml(u.role)}</small></div><span class="status-badge ${u.status==='ACTIVE'?'status-completed':'status-cancelled'}">${escapeHtml(u.status)}</span>${u.role==='ADMIN'?`<span class="role-owner-label">${getLanguage()==='ar'?'المالك':'Owner'}</span>`:`<label class="user-role-picker">${getLanguage()==='ar'?'الدور':'Role'}<select data-user-role="${u.id}"><option value="CUSTOMER" ${u.role==='CUSTOMER'?'selected':''}>${t('customer')}</option><option value="SUPERVISOR" ${u.role==='SUPERVISOR'?'selected':''}>${getLanguage()==='ar'?'مشرف':'Supervisor'}</option><option value="DELIVERY" ${u.role==='DELIVERY'?'selected':''}>${getLanguage()==='ar'?'مندوب توصيل':'Delivery'}</option></select></label>`}<label>${t('userStatus')}<select data-user-status="${u.id}"><option value="ACTIVE" ${u.status==='ACTIVE'?'selected':''}>${t('active')}</option><option value="DISABLED" ${u.status==='DISABLED'?'selected':''}>${t('disabled')}</option></select></label><button class="btn btn-outline btn-small" data-save-user="${u.id}" ${u.role==='ADMIN'?'disabled':''}>${t('save')}</button></article>`).join('')||emptyMarkup(t('noData'),t('noData'))}</section>`;users.forEach(u=>document.querySelector(`[data-save-user="${u.id}"]`)?.addEventListener('click',async()=>{const status=document.querySelector(`[data-user-status="${u.id}"]`)?.value;const role=document.querySelector(`[data-user-role="${u.id}"]`)?.value;try{if(status!==u.status)await api(`/admin/users/${u.id}/status`,{method:'PUT',body:{status}});if(role&&role!==u.role)await api(`/admin/users/${u.id}/role`,{method:'PUT',body:{role}});toast(t('saved'));renderAdminData();}catch(e){toast(e.message,'error');}}));}
async function renderCompany(target){
  const company=state.company||await api('/company').catch(()=>({}));
  const isArabic=getLanguage()==='ar';
  target.innerHTML=`<form id="companyForm" class="form-card admin-settings">
    <h2>${t('companySettings')}</h2>
    <p class="form-note">${isArabic?'يمكنك تغيير معلومات الشركة وروابط التواصل هنا. ضع الروابط الرسمية وأرقام الشركة قبل النشر.':'Edit company details and social links here. Replace the example links and phone number with the company’s official details before publishing.'}</p>
    <label>${t('companyName')}<input name="companyName" value="${escapeHtml(company.companyName||'')}"></label>
    <label>${t('companyPhone')}<input name="phone" type="tel" value="${escapeHtml(company.phone||DEFAULT_COMPANY_PHONE)}"></label>
    <label>${t('whatsappNumber')}<input name="whatsappNumber" type="tel" value="${escapeHtml(company.whatsappNumber||company.phone||DEFAULT_COMPANY_PHONE)}"></label>
    <label>${t('companyEmail')}<input name="email" type="email" value="${escapeHtml(company.email||'')}"></label>
    <label>${t('companyAddress')}<textarea name="address" rows="3">${escapeHtml(company.address||'')}</textarea></label>
    <label>${t('imageOptional')}<input name="logoUrl" type="url" value="${escapeHtml(company.logoUrl||'')}"></label>
    <label>${isArabic?'رابط حساب X':'X / Twitter profile URL'}<input name="xUrl" type="url" value="${escapeHtml(company.xUrl||DEFAULT_SOCIAL_LINKS.xUrl)}" required></label>
    <label>${isArabic?'رابط إنستغرام':'Instagram profile URL'}<input name="instagramUrl" type="url" value="${escapeHtml(company.instagramUrl||DEFAULT_SOCIAL_LINKS.instagramUrl)}" required></label>
    <label>${isArabic?'رابط سناب شات':'Snapchat profile URL'}<input name="snapchatUrl" type="url" value="${escapeHtml(company.snapchatUrl||DEFAULT_SOCIAL_LINKS.snapchatUrl)}" required></label>
    <label>${isArabic?'رابط الموقع على خرائط Google':'Google Maps location URL'}<input name="mapsUrl" type="url" value="${escapeHtml(company.mapsUrl||DEFAULT_SOCIAL_LINKS.mapsUrl)}" required></label>
    <button class="btn btn-primary" type="submit">${t('saveSettings')}</button>
  </form>`;
  document.querySelector('#companyForm')?.addEventListener('submit',async e=>{
    e.preventDefault(); const form=e.currentTarget; const data=Object.fromEntries(new FormData(form));
    try{state.company=await api('/admin/company',{method:'PUT',body:data});renderSiteFooter();toast(t('saved'));}
    catch(err){showError(err,form);}
  });
}
function renderOrderDetails(orderId) {
  api(`/orders/${orderId}`).then(order=>openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / ORDER</span><h2>${t('orderNumber')} #${escapeHtml(order.orderNumber||order.id)}</h2></div><button class="modal-close" data-close-modal>×</button></div><div class="order-detail-status"><span class="status-badge status-${String(order.status).toLowerCase()}">${escapeHtml(order.status)}</span><strong>${currency(order.totalAmount)}</strong></div><p>${escapeHtml(order.deliveryAddress||'')}</p><p>${escapeHtml(order.contactPhone||'')}</p><div class="detail-order-items">${(order.items||[]).map(item=>`<div class="mini-row"><span>${escapeHtml(displayBilingualText(item.productName,item.productNameAr))} × ${item.quantity}</span><strong>${currency(item.lineTotal)}</strong></div>`).join('')}</div><button class="btn btn-primary btn-full" data-reorder="${order.id}">${t('reorder')}</button>`)).catch(error=>toast(error.message,'error'));
}
function renderSiteFooter() {
  const footer = document.querySelector('#siteFooter');
  if (!footer) return;
  const isArabic = getLanguage() === 'ar';
  const year = new Date().getFullYear();
  const company = state.company || {};
  const savedCompanyName = String(company.companyName || '').trim();
  const companyName = (!savedCompanyName || savedCompanyName.toLowerCase() === 'nabta')
    ? (isArabic ? 'شركة نبته الغذائية' : 'Nabta Food Company')
    : savedCompanyName;
  const phoneDisplay = company.phone || DEFAULT_COMPANY_PHONE;
  const social = {
    xUrl: safeExternalUrl(company.xUrl, DEFAULT_SOCIAL_LINKS.xUrl),
    instagramUrl: safeExternalUrl(company.instagramUrl, DEFAULT_SOCIAL_LINKS.instagramUrl),
    snapchatUrl: safeExternalUrl(company.snapchatUrl, DEFAULT_SOCIAL_LINKS.snapchatUrl),
    mapsUrl: safeExternalUrl(company.mapsUrl, DEFAULT_SOCIAL_LINKS.mapsUrl)
  };
  footer.dir = isArabic ? 'rtl' : 'ltr';
  footer.innerHTML = `
    <div class="site-footer-main">
      <section class="site-footer-company" aria-labelledby="footerCompanyTitle">
        <h2 id="footerCompanyTitle">${escapeHtml(companyName)}</h2>
        <p>${isArabic
          ? 'المدينة المنورة، المملكة العربية السعودية. متخصصون في تجارة وتعبئة التوابل، الأعشاب، والمنتجات الغذائية.'
          : 'Madinah, Kingdom of Saudi Arabia. Specializing in trading and packaging spices, herbs, and food products.'}</p>
      </section>
      <section class="site-footer-social" aria-labelledby="footerSocialTitle">
        <h2 id="footerSocialTitle">${isArabic ? 'مواقع التواصل الاجتماعي' : 'Social Media'}</h2>
        <ul class="site-footer-social-list">
          <li><span aria-hidden="true">📞</span><span><button type="button" class="footer-contact-trigger" data-contact-choice>${isArabic ? 'رقم الهاتف:' : 'Phone / WhatsApp:'} <bdi dir="ltr">${escapeHtml(phoneDisplay)}</bdi></button></span></li>
          <li><span aria-hidden="true" class="footer-x-icon">𝕏</span><a class="footer-social-link" href="${social.xUrl}" target="_blank" rel="noopener noreferrer">${isArabic ? 'منصة إكس (تويتر)' : 'X (Twitter)'}</a></li>
          <li><span aria-hidden="true">📷</span><a class="footer-social-link" href="${social.instagramUrl}" target="_blank" rel="noopener noreferrer">${isArabic ? 'إنستغرام (Instagram)' : 'Instagram'}</a></li>
          <li><span aria-hidden="true">👻</span><a class="footer-social-link" href="${social.snapchatUrl}" target="_blank" rel="noopener noreferrer">${isArabic ? 'سناب شات (Snapchat)' : 'Snapchat'}</a></li>
          <li><span aria-hidden="true">📍</span><a class="footer-social-link" href="${social.mapsUrl}" target="_blank" rel="noopener noreferrer">${isArabic ? 'موقع شركة نبته (خرائط جوجل)' : 'Nabta Food Company location (Google Maps)'}</a></li>
        </ul>
      </section>
    </div>
    <div class="site-footer-bottom"><p>${isArabic
      ? `© ${year} ${escapeHtml(companyName)}. جميع الحقوق محفوظة.`
      : `© ${year} ${escapeHtml(companyName)}. All rights reserved.`}</p><button type="button" class="footer-about-link" data-open-about>${isArabic ? 'عن التطبيق · مشاركة نبته' : 'About Us · Share Nabta'}</button></div>`;
}

function renderAbout(target = view) {
  const ar = getLanguage() === 'ar';
  const isNativeAndroid = Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';
  const downloadUrl = /^https:\/\//i.test(APP_DOWNLOAD_URL) ? APP_DOWNLOAD_URL : '';
  target.innerHTML = `<section class="page-heading"><span class="section-kicker">NABTA / GENERAL</span><h1>${ar ? 'عن التطبيق' : 'About Nabta'}</h1><p>${ar ? 'تعرف على شركة نبته وشارك التطبيق مع الآخرين.' : 'Learn about Nabta and share the app with others.'}</p></section>
    <section class="settings-panel about-app-panel">
      <div class="about-app-brand"><img src="/assets/nabta-logo.png" alt="Nabta Food Company" /><div><h2>${ar ? 'شركة نبته الغذائية' : 'Nabta Food Company'}</h2><p>${ar ? 'جودة تثق بها، ونكهات تجمعنا.' : 'Quality you trust. Flavours that bring us together.'}</p></div></div>
      <div class="about-app-actions"><button class="btn btn-primary" id="shareNabtaApk" type="button">↗ ${ar ? 'مشاركة ملف التطبيق APK' : 'Share Nabta APK'}</button>
      ${downloadUrl ? `<a class="btn btn-outline" id="downloadNabtaApk" href="${escapeHtml(downloadUrl)}" target="_blank" rel="noopener noreferrer" download>↓ ${ar ? 'تنزيل أحدث إصدار' : 'Download latest APK'}</a>` : `<button class="btn btn-outline" id="downloadNabtaApk" type="button" disabled>↓ ${ar ? 'تنزيل التطبيق' : 'Download APK'}</button>`}</div>
      <p class="about-app-note">${isNativeAndroid ? (ar ? 'ستظهر قائمة مشاركة أندرويد، ويمكنك اختيار واتساب أو تيليجرام أو Quick Share أو البلوتوث أو الملفات أو أي تطبيق متوافق.' : 'Android will open its native share menu. Choose WhatsApp, Telegram, Quick Share, Bluetooth, Files, or another compatible app.') : (ar ? 'مشاركة ملف APK مباشرة تتطلب تطبيق أندرويد. رابط التنزيل العام سيُفعّل بعد نشر الإصدار النهائي.' : 'Direct APK-file sharing requires the Android app. The public download link will be activated after the final release is hosted online.')}</p>
      ${!downloadUrl ? `<p class="form-note">${ar ? 'رابط تنزيل الإصدار النهائي لم يُضبط بعد. بعد النشر، أضف VITE_NABTA_APK_URL إلى إعدادات بناء الواجهة.' : 'The final APK download URL is not configured yet. After hosting the release, set VITE_NABTA_APK_URL for the frontend build.'}</p>` : ''}
      <div class="about-app-version"><span>${ar ? 'المنصة' : 'Platform'}</span><strong>Android · Nabta</strong><span>${ar ? 'اللغات' : 'Languages'}</span><strong>العربية / English</strong></div>
    </section>`;
  target.querySelector('#shareNabtaApk')?.addEventListener('click', async event => {
    const button = event.currentTarget;
    setBusy(button, true, ar ? 'جارٍ فتح المشاركة…' : 'Opening share menu…');
    try {
      if (isNativeAndroid) {
        await NabtaApkShare.shareInstalledApk();
      } else {
        const url = downloadUrl || window.location.href.split('#')[0];
        const shareData = { title: 'Nabta Food Company', text: ar ? 'جرّب تطبيق نبته الغذائية.' : 'Try the Nabta Food Company app.', url };
        if (navigator.share) await navigator.share(shareData);
        else if (navigator.clipboard?.writeText) {
          await navigator.clipboard.writeText(url);
          toast(ar ? 'تم نسخ رابط التطبيق.' : 'App link copied.');
        } else {
          openModal(`<div class="modal-head"><h2>${ar ? 'مشاركة نبته' : 'Share Nabta'}</h2><button class="modal-close" data-close-modal>×</button></div><p>${ar ? 'انسخ رابط التطبيق:' : 'Copy the app link:'}</p><input readonly value="${escapeHtml(url)}" />`);
        }
      }
    } catch (error) {
      if (error?.name !== 'AbortError') toast(error?.message || (ar ? 'تعذّرت مشاركة التطبيق.' : 'Could not share the app.'), 'error');
    } finally { setBusy(button, false); }
  });
}

function renderCurrent() {
  refreshNav();
  if(state.page==='home') renderHome();
  else if(state.page==='categories') renderCategories();
  else if(state.page==='cart') renderCart();
  else if(state.page==='settings') renderSettings();
  else if(state.page==='orders') renderOrders();
  else if(state.page==='profile') renderProfile();
  else if(state.page==='admin') renderAdmin();
  else if(state.page==='operations') renderOperations();
  else if(state.page==='detail') renderProductDetail(state.productId);
  applyLanguage();
  renderSiteFooter();
}
async function navigate(page) {
  if (page === 'profile' || page === 'orders') { state.settingsTab = page === 'profile' ? 'profile' : 'orders'; page = 'settings'; }
  if (page === 'settings' && !['profile','orders','addresses','wishlist','preferences','about'].includes(state.settingsTab)) state.settingsTab = 'profile';
  if (page==='admin' && getUser()?.role!=='ADMIN') { state.page='admin'; renderCurrent(); return; }
  if (page==='operations' && !['SUPERVISOR','DELIVERY'].includes(getUser()?.role)) { state.page=getUser()?.role==='ADMIN'?'admin':'home'; renderCurrent(); return; }
  state.prevPage=state.page==='detail'?'home':state.page;
  state.page=page;
  if (page==='home'||page==='categories') { state.categoryId=page==='categories'?state.categoryId:null; await fetchCatalog(); }
  renderCurrent();
  window.scrollTo({top:0,behavior:'smooth'});
}
function addToCart(id) {
  const product=state.products.find(p=>String(p.id)===String(id)); if(!product){toast(t('loadError'),'error');return;}
  if(Number(product.stockQuantity)<1){toast(t('outOfStock'),'error');return;}
  const existing=state.cart.find(item=>String(item.id)===String(product.id));
  if(existing){if(existing.quantity>=Number(product.stockQuantity)){toast(t('outOfStock'),'error');return;}existing.quantity+=1;}
  else state.cart.push({...product,id:product.id,quantity:1});
  saveCart(); toast(`${displayProductName(product)} · ${t('addedToCart')}`); if(state.page==='cart') renderCart(); else renderCurrent();
}
function changeQty(id, delta) { const item=state.cart.find(x=>String(x.id)===String(id));if(!item)return;item.quantity+=delta;if(item.quantity<1)state.cart=state.cart.filter(x=>String(x.id)!==String(id));else if(item.stockQuantity&&item.quantity>item.stockQuantity)item.quantity=item.stockQuantity;saveCart();if(state.page==='cart')renderCart();else renderCurrent(); }
function closeModal() { modalHost.innerHTML = ''; document.body.classList.remove('nabta-modal-open'); }
function openModal(content, wide=false) { document.body.appendChild(modalHost); document.body.classList.add('nabta-modal-open'); modalHost.innerHTML=`<div class="modal-backdrop" data-close-modal><section class="modal-card ${wide?'modal-wide':''}" role="dialog" aria-modal="true" onclick="event.stopPropagation()">${content}</section></div>`; modalHost.querySelectorAll('[data-close-modal]').forEach(el=>el.addEventListener('click',e=>{if(e.target===el||el.classList.contains('modal-close'))closeModal();})); requestAnimationFrame(()=>modalHost.querySelector('input,select,textarea')?.focus({preventScroll:true})); }
function openAuth(mode='login') {
  const register=mode==='register';
  openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / ACCOUNT</span><h2>${register?t('registerTitle'):t('signInTitle')}</h2><p>${t('forgotNote')}</p></div><button class="modal-close" data-close-modal>×</button></div><form id="authForm" class="auth-form">${register?`<label>${t('name')}<input name="name" autocomplete="name" required maxlength="100"></label>`:''}<label>${t('email')}<input name="email" type="email" autocomplete="email" required></label><label>${t('password')}<input name="password" type="password" autocomplete="${register?'new-password':'current-password'}" minlength="${register?'8':'1'}" required></label><div class="form-error" id="authError"></div><button class="btn btn-primary btn-full" type="submit">${register?t('createAccount'):t('login')}</button></form><div class="switch-auth"><span>${register?t('hasAccount'):t('noAccount')}</span><button class="text-link" id="switchAuth">${register?t('login'):t('register')}</button></div>`);
  document.querySelector('#switchAuth')?.addEventListener('click',()=>openAuth(register?'login':'register'));
  document.querySelector('#authForm')?.addEventListener('submit',async event=>{event.preventDefault();const form=event.currentTarget;const button=form.querySelector('button[type=submit]');setBusy(button,true);const data=Object.fromEntries(new FormData(form));try{const result=await api(register?'/auth/register':'/auth/login',{method:'POST',body:data});setSession(result);if(isOperationsRole(result.user.role))void registerAdminPushNotifications();closeModal();toast(register?t('registerSuccess'):t('loginSuccess'));refreshNav();if(state.page==='admin'&&result.user.role!=='ADMIN')state.page='home';if(result.user.role==='ADMIN'&&state.page!=='cart')state.page='admin';else if(['SUPERVISOR','DELIVERY'].includes(result.user.role)&&state.page!=='cart')state.page='operations';await fetchCatalog();renderCurrent();}catch(error){document.querySelector('#authError').textContent=error.message;}finally{setBusy(button,false);}});
}
async function openCheckout() {
  const ar=getLanguage()==='ar';
  let addresses=[];
  try { addresses=await loadPage('/users/me/addresses'); } catch { addresses=[]; }
  const preferredPhone=getUser()?.phone||'';
  const preferredAddress=getUser()?.address||'';
  const total=state.cart.reduce((sum,item)=>sum+Number(item.price||0)*Number(item.quantity||0),0);
  openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / DELIVERY</span><h2>${t('checkoutTitle')}</h2><p>${t('checkoutNote')}</p></div><button class="modal-close" data-close-modal>×</button></div><form id="checkoutForm" class="auth-form">${addresses.length?`<label>${ar?'العناوين المحفوظة':'Saved addresses'}<select name="savedAddressId"><option value="">${ar?'إدخال عنوان جديد':'Enter a new address'}</option>${addresses.map(a=>`<option value="${a.id}">${escapeHtml(a.label)} — ${escapeHtml(a.address)}</option>`).join('')}</select></label>`:''}<label>${t('deliveryAddress')}<textarea name="deliveryAddress" rows="3" maxlength="1500" required>${escapeHtml(preferredAddress)}</textarea></label><label>${t('phone')}<input name="contactPhone" type="tel" inputmode="numeric" pattern="[0-9]{7,20}" maxlength="20" value="${escapeHtml(preferredPhone)}" required placeholder="${ar?'أرقام فقط':'Digits only'}"></label><label>${t('notes')}<textarea name="notes" rows="2" maxlength="1500"></textarea></label><fieldset class="payment-method-card"><legend>${t('paymentMethod')}</legend><label class="payment-method-option"><input type="radio" name="paymentMethod" value="CASH_ON_DELIVERY" checked><span>${t('cashOnDelivery')}</span></label><p class="form-note">${t('cashOnDeliveryNote')}</p></fieldset><div class="form-error" id="checkoutError"></div><button class="btn btn-primary btn-full" type="submit">${t('placeOrder')} · ${currency(total)}</button></form>`,true);
  const form=document.querySelector('#checkoutForm');
  form?.querySelector('[name=savedAddressId]')?.addEventListener('change',event=>{const address=addresses.find(a=>String(a.id)===String(event.target.value));if(address){form.querySelector('[name=deliveryAddress]').value=address.address;form.querySelector('[name=contactPhone]').value=address.phone||preferredPhone;}});
  form?.addEventListener('submit',async e=>{
    e.preventDefault();
    if(!getToken()){form.querySelector('#checkoutError').textContent=t('loginRequired');return;}
    const data=Object.fromEntries(new FormData(form));
    if(!validPhone(data.contactPhone)){form.querySelector('#checkoutError').textContent=ar?'يجب إدخال 7 إلى 20 رقمًا فقط.':'Phone must contain 7–20 digits only.';return;}
    const button=form.querySelector('button[type=submit]');setBusy(button,true);
    try {
      const order=await api('/orders',{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID?.()||`${Date.now()}-${Math.random()}`},body:{items:state.cart.map(item=>({productId:Number(item.id),quantity:Number(item.quantity)})),deliveryAddress:data.deliveryAddress.trim(),contactPhone:data.contactPhone.trim(),notes:data.notes.trim(),paymentMethod:'CASH_ON_DELIVERY'}});
      state.cart=[];saveCart();closeModal();toast(`${t('orderPlaced')} #${order.orderNumber||order.id}`);state.page='settings';state.settingsTab='orders';renderCurrent();
    } catch(error) { form.querySelector('#checkoutError').textContent=error.message; }
    finally { setBusy(button,false); }
  });
}
async function openOrderReorder(id) { try{const order=await api(`/orders/${id}`);state.cart=[];for(const item of order.items||[]){const live=await api(`/products/${item.productId}`).catch(()=>null);if(live&&live.available&&Number(live.stockQuantity)>0){state.cart.push({...live,id:live.id,quantity:Math.min(item.quantity,live.stockQuantity)});}}saveCart();closeModal();state.page='cart';renderCart();toast(t('reorder'));}catch(e){toast(e.message,'error');} }
async function contactWhatsApp() {
  const company=state.company||await api('/company').catch(()=>null);
  const number=internationalPhoneDigits(company?.whatsappNumber||company?.phone||DEFAULT_COMPANY_PHONE);
  if(!number){toast(t('whatsappMissing'),'error');return;}
  window.open(`https://wa.me/${number}`,'_blank','noopener,noreferrer');
}
async function openContactChoice() {
  const company=state.company||await api('/company').catch(()=>({}));
  const phoneRaw=company?.phone||DEFAULT_COMPANY_PHONE;
  const whatsappRaw=company?.whatsappNumber||company?.phone||DEFAULT_COMPANY_PHONE;
  const phone=internationalPhoneDigits(phoneRaw);
  const whatsapp=internationalPhoneDigits(whatsappRaw);
  const isArabic=getLanguage()==='ar';
  openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / CONTACT</span><h2>${isArabic?'تواصل معنا':'Contact Nabta'}</h2><p>${isArabic?'اختر طريقة التواصل المناسبة لك.':'Choose how you would like to contact us.'}</p></div><button class="modal-close" data-close-modal>×</button></div><div class="contact-choice-actions"><a class="btn btn-primary btn-full" href="tel:+${phone}">📞 ${isArabic?'اتصال هاتفي':'Call by phone'}</a><a class="btn btn-whatsapp btn-full" href="https://wa.me/${whatsapp}" target="_blank" rel="noopener noreferrer">💬 ${isArabic?'التواصل عبر واتساب':'Contact on WhatsApp'}</a></div><p class="form-note">${isArabic?'يمكن للمسؤول تحديث أرقام الاتصال من إعدادات الشركة.':'The administrator can update the contact numbers in Company Settings.'}</p>`);
}

document.addEventListener('click',async event=>{
  const aboutLink=event.target.closest('[data-open-about]'); if(aboutLink){event.preventDefault();state.settingsTab='about';await navigate('settings');return;}
  const contactChoice=event.target.closest('[data-contact-choice]'); if(contactChoice){await openContactChoice();return;}
  const nav=event.target.closest('[data-nav]'); if(nav){event.preventDefault();await navigate(nav.dataset.nav);return;}
  const add=event.target.closest('[data-add]'); if(add){addToCart(add.dataset.add);return;}
  const detail=event.target.closest('[data-product-details]'); if(detail){state.productId=detail.dataset.productDetails;state.prevPage=state.page;state.page='detail';renderCurrent();return;}
  const category=event.target.closest('[data-category]'); if(category){state.categoryId=category.dataset.category;state.search='';await fetchCatalog();state.page='categories';renderCurrent();return;}
  const qty=event.target.closest('[data-qty]');if(qty){changeQty(qty.dataset.qty,Number(qty.dataset.delta));return;}
  const remove=event.target.closest('[data-remove]');if(remove){state.cart=state.cart.filter(i=>String(i.id)!==String(remove.dataset.remove));saveCart();renderCart();return;}
  const order=event.target.closest('[data-order-details]');if(order){renderOrderDetails(order.dataset.orderDetails);return;}
  const historyBtn=event.target.closest('[data-order-history]');if(historyBtn){    try{        const history=await api(`/admin/orders/${historyBtn.dataset.orderHistory}/history`);
        openModal(`<div class="modal-head"><div><span class="section-kicker">NABTA / AUDIT TRAIL</span><h2>Order Status History</h2></div><button class="btn btn-outline" data-close-modal>Close</button></div><div class="modal-body">${Array.isArray(history)&&history.length?history.map(item=>`<article class="order-card"><strong>${escapeHtml(item.status||'Status update')}</strong><p>${escapeHtml(item.note||'')}</p><small>${escapeHtml(item.actorName||item.actorEmail||'System')} · ${escapeHtml(item.createdAt?new Date(item.createdAt).toLocaleString():'')}</small></article>`).join(''):'<p>No status history found for this order.</p>'}</div>`);
    }catch(error){
        alert(error.message||'Could not load order status history.');
    }
    return;
}
  const reorder=event.target.closest('[data-reorder]');if(reorder){openOrderReorder(reorder.dataset.reorder);return;}
  const settingsTab=event.target.closest('[data-settings-tab]');if(settingsTab){state.settingsTab=settingsTab.dataset.settingsTab;renderSettingsTab();return;}
  const tab=event.target.closest('[data-admin-tab]');if(tab){state.adminTab=tab.dataset.adminTab;renderAdmin();return;}
  const opTab=event.target.closest('[data-operations-tab]');if(opTab){state.operationsTab=opTab.dataset.operationsTab;renderOperations();return;}
  const fav=event.target.closest('[data-favorite-toggle]');if(fav){event.preventDefault();event.stopPropagation();await toggleFavorite(fav.dataset.favoriteToggle);return;}
});
document.querySelector('#languageButton').addEventListener('click',async()=>{setLanguage(getLanguage()==='en'?'ar':'en');document.querySelector('#languageButton').textContent=getLanguage()==='ar'?'English':'عربي';applyLanguage();await fetchCatalog();renderCurrent();});
document.querySelector('#settingsButton').addEventListener('click',()=>{state.settingsTab='profile';navigate('settings');});
document.querySelector('#accountButton').addEventListener('click',()=>{if(getUser()){state.settingsTab='profile';navigate('settings');}else openAuth('login');});
document.querySelector('#whatsappButton')?.addEventListener('click',contactWhatsApp);
document.querySelector('#topLogoutButton')?.addEventListener('click',async()=>{await unregisterAdminPushNotifications();clearSession();state.page='home';refreshNav();toast(t('logout'));navigate('home');});
window.addEventListener('nabta:open-admin-orders',async()=>{const role=getUser()?.role;if(role==='ADMIN'){state.adminTab='orders';await navigate('admin');}else if(['SUPERVISOR','DELIVERY'].includes(role)){state.operationsTab=role==='SUPERVISOR'?'orders':'assignments';await navigate('operations');}});
window.addEventListener('nabta:push-received',event=>{const message=event.detail?.body||(getLanguage()==='ar'?'تم استلام إشعار جديد':'New Nabta notification');toast(message);});
window.addEventListener('nabta:push-status-changed',()=>{void updateAdminPushStatus();});
window.addEventListener('nabta:session-expired',()=>{refreshNav();toast(t('loginRequired'),'error');});
window.addEventListener('hashchange',async()=>{const page=location.hash.replace('#','');if(['home','categories','cart','settings','orders','profile','admin','operations'].includes(page)&&page!==state.page)await navigate(page);});
/* Mouse-wheel safety fallback: the document is the vertical scroll container.
   Let native scrolling run first; only scroll manually if no element moved. */
function installMouseWheelFallback() {
  window.addEventListener('wheel', event => {
    if (event.ctrlKey || !Number.isFinite(event.deltaY) || Math.abs(event.deltaY) < 0.5) return;
    const target = event.target instanceof Element ? event.target : null;
    if (target?.closest('input, textarea, select, [contenteditable="true"], .modal-card, .primary-links')) return;

    const root = document.scrollingElement || document.documentElement;
    const beforeRoot = root.scrollTop;
    const beforeWindow = window.scrollY;
    const beforeAncestors = [];
    for (let node = target; node && node !== document.body; node = node.parentElement) {
      const style = window.getComputedStyle(node);
      if (/(auto|scroll|overlay)/.test(style.overflowY) && node.scrollHeight > node.clientHeight + 1) {
        beforeAncestors.push({ element: node, top: node.scrollTop });
      }
    }
    const pixels = event.deltaMode === 1 ? event.deltaY * 16 : event.deltaMode === 2 ? event.deltaY * window.innerHeight : event.deltaY;
    window.setTimeout(() => {
      const nativeMoved = Math.abs(root.scrollTop - beforeRoot) > 0.5 ||
        Math.abs(window.scrollY - beforeWindow) > 0.5 ||
        beforeAncestors.some(item => Math.abs(item.element.scrollTop - item.top) > 0.5);
      if (nativeMoved) return;
      const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
      if (maxScroll <= 1) return;
      const next = Math.max(0, Math.min(maxScroll, root.scrollTop + pixels));
      if (Math.abs(next - root.scrollTop) > 0.5) {
        root.scrollTop = next;
        window.scrollTo({ top: next, behavior: 'auto' });
      }
    }, 0);
  }, { passive: true });
}
installMouseWheelFallback();

/* Mobile bottom navigation: hide while the user scrolls down, reveal while scrolling up,
   and restore it shortly after scrolling stops so it remains easy to access. */
function installMobileNavAutoHide() {
  const nav = document.querySelector('.mobile-nav');
  if (!nav) return;
  let lastY = Math.max(0, window.scrollY || document.documentElement.scrollTop || 0);
  let showTimer = 0;
  const showNav = () => nav.classList.remove('is-hidden-on-scroll');
  window.addEventListener('scroll', () => {
    const currentY = Math.max(0, window.scrollY || document.documentElement.scrollTop || 0);
    const delta = currentY - lastY;
    if (window.innerWidth > 700 || currentY < 48 || delta < -2) {
      showNav();
      window.clearTimeout(showTimer);
    } else if (delta > 2) {
      nav.classList.add('is-hidden-on-scroll');
      window.clearTimeout(showTimer);
      showTimer = window.setTimeout(showNav, 700);
    }
    lastY = currentY;
  }, { passive: true });
  window.addEventListener('resize', () => { if (window.innerWidth > 700) showNav(); });
  nav.addEventListener('touchstart', showNav, { passive: true });
  nav.addEventListener('pointerdown', showNav, { passive: true });
}
installMobileNavAutoHide();

setLanguage(getLanguage());document.querySelector('#languageButton').textContent=getLanguage()==='ar'?'English':'عربي';
if(isOperationsRole())void registerAdminPushNotifications();
state.loading=true;renderCurrent();fetchCatalog().then(()=>{state.loading=false;renderCurrent();}).catch(()=>{state.loading=false;renderCurrent();});
