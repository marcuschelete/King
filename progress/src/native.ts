import {useStore,metrics} from './model';
declare global {interface Window {KingProgress?:{getItem:()=>string|null;setItem:(value:string)=>void;exportFile:(name:string,mime:string,data:string)=>void}}}
export const nativeStorage={
 getItem:(_key:string)=>window.KingProgress?window.KingProgress.getItem():localStorage.getItem('rival-v1'),
 setItem:(_key:string,value:string)=>{if(window.KingProgress)window.KingProgress.setItem(value);else localStorage.setItem('rival-v1',value)},
 removeItem:(_key:string)=>{if(window.KingProgress)window.KingProgress.setItem('');else localStorage.removeItem('rival-v1')}
};
export function saveFile(name:string,mime:string,data:string){
 if(window.KingProgress){window.KingProgress.exportFile(name,mime,data);return}
 const u=URL.createObjectURL(new Blob([data],{type:mime}));const a=document.createElement('a');a.href=u;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(u),1000)
}
export function backupData(){const {habits,entries,completed,start,review,goal,theme,demo}=useStore.getState();saveFile('king-progress-backup.json','application/json',JSON.stringify({format:'king-progress-v1',state:{habits,entries,completed,start,review,goal,theme,demo}},null,2))}
export async function restoreBackup(file:File){
 if(file.size>2*1024*1024)throw Error('Too large');const b=JSON.parse(await file.text());const s=b.state;
 const validDate=(v:unknown)=>typeof v==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(v)&&!isNaN(Date.parse(v))&&new Date(v).toISOString().slice(0,10)===v;
 if(b.format!=='king-progress-v1'||!s||!Array.isArray(s.habits)||!Array.isArray(s.entries)||!s.completed||typeof s.completed!=='object'||Array.isArray(s.completed)||!validDate(s.start)||!Number.isFinite(s.goal)||s.goal<=0||typeof s.review!=='string'||!['light','dark'].includes(s.theme)||typeof s.demo!=='boolean')throw Error('Invalid');
 const ids=new Set<string>();for(const h of s.habits){if(!h||typeof h.id!=='string'||ids.has(h.id)||typeof h.name!=='string'||!['Body','Mind','Craft','Money','Recovery','Spirit'].includes(h.group)||!validDate(h.created)||(h.archived&&!validDate(h.archived))||!Array.isArray(h.days)||!h.days.every((d:unknown)=>Number.isInteger(d)&&Number(d)>=0&&Number(d)<=6))throw Error('Invalid habit');ids.add(h.id)}
 const entryIds=new Set<string>();for(const e of s.entries){if(!e||typeof e.id!=='string'||entryIds.has(e.id)||!validDate(e.date)||!Number.isFinite(e.value)||e.value<0||typeof e.note!=='string'||!metrics.some(m=>m.id===e.metric)&&!['income','spending'].includes(e.metric))throw Error('Invalid entry');entryIds.add(e.id)}
 for(const [d,values]of Object.entries(s.completed)){if(!validDate(d)||!Array.isArray(values)||!values.every(id=>ids.has(id)))throw Error('Invalid history')}
 if(!confirm('Replace your progress, habits and history with this backup?'))return;
 useStore.getState().patch({habits:s.habits,entries:s.entries,completed:s.completed,start:s.start,review:s.review,goal:s.goal,theme:s.theme,demo:s.demo});
}
