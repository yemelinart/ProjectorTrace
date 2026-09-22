const root=document.documentElement;
const language=document.getElementById('language');
const descriptions={
 interface:['Your reference and a menu designed for remote control.','Референс и меню для управления пультом.'],
 'transform-menu':['Move, scale, rotate, fit and adjust perspective.','Перемещение, масштаб, поворот, вписывание и перспектива.'],
 guides:['Grid, corner guides and a center cross help you judge placement.','Сетка, угловые направляющие и перекрестие помогают выстроить изображение.'],
 outline:['Magic Outline brings out the contours in your reference.','Magic Outline выделяет контуры референса.'],
 palette:['A color palette extracted from the reference, displayed in the workspace.','Палитра цветов референса прямо в рабочей области.'],
 'filter-menu':['The image-processing tools, directly inside the projector app.','Инструменты обработки прямо в приложении на проекторе.']
};
let current='interface';
function updateCaption(){const label=descriptions[current][root.lang==='ru'?1:0];document.getElementById('screen-description').textContent=label;document.getElementById('workspace-screen').alt=label;}
function setLanguage(lang){root.lang=lang;language.textContent=lang==='en'?'RU':'EN';language.setAttribute('aria-label',lang==='en'?'Switch to Russian':'Switch to English');document.title=lang==='en'?'Projector Trace — Your image. On your canvas.':'Projector Trace — Ваш референс. На вашем холсте.';updateCaption();try{localStorage.setItem('pt-language',lang)}catch{}}
language.addEventListener('click',()=>setLanguage(root.lang==='en'?'ru':'en'));
try{if(localStorage.getItem('pt-language')==='ru')setLanguage('ru')}catch{}
document.querySelectorAll('[data-screen]').forEach(button=>{button.setAttribute('aria-pressed',button.classList.contains('active'));button.addEventListener('click',()=>{current=button.dataset.screen;document.getElementById('workspace-screen').src=`assets/${current}.webp`;document.querySelectorAll('[data-screen]').forEach(b=>{b.classList.toggle('active',b===button);b.setAttribute('aria-pressed',String(b===button))});updateCaption()})});
const slider=document.getElementById('compare-range');slider.addEventListener('input',()=>document.getElementById('comparison').style.setProperty('--split',slider.value+'%'));
const dialog=document.getElementById('lightbox');document.querySelector('.screen-expand').addEventListener('click',()=>{const img=dialog.querySelector('img');img.src=`assets/${current}.webp`;img.alt=document.getElementById('workspace-screen').alt;dialog.showModal()});document.getElementById('close-lightbox').addEventListener('click',()=>dialog.close());dialog.addEventListener('click',event=>{if(event.target===dialog)dialog.close()});
