const root = document.documentElement;
const language = document.getElementById('language');
function setLanguage(lang) {
  root.lang = lang;
  language.textContent = lang === 'en' ? 'RU' : 'EN';
  language.setAttribute('aria-label', lang === 'en' ? 'Switch to Russian' : 'Switch to English');
  document.title = lang === 'en' ? 'Projector Trace — Releases & roadmap' : 'Projector Trace — Обновления и планы';
  try { localStorage.setItem('pt-language', lang); } catch {}
}
language.addEventListener('click', () => setLanguage(root.lang === 'en' ? 'ru' : 'en'));
try { if (localStorage.getItem('pt-language') === 'ru') setLanguage('ru'); } catch {}
