import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import ts from 'typescript';
const temp=new URL('./node_modules/.king-tests/',import.meta.url);await fs.mkdir(temp,{recursive:true});
for(const name of ['native','model','summary']){
 const source=await fs.readFile(new URL(`./src/${name}.${name==='summary'?'tsx':'ts'}`,import.meta.url),'utf8');
 const result=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ES2022,jsx:ts.JsxEmit.ReactJSX}}).outputText.replaceAll("'./native'","'./native.mjs'").replaceAll("'./model'","'./model.mjs'");
 await fs.writeFile(new URL(name+'.mjs',temp),result);
}
let stored=null,exported=null;globalThis.window={KingProgress:{getItem:()=>stored,setItem:value=>stored=value,exportFile:(...args)=>exported=args}};globalThis.confirm=()=>true;
const {useStore,metrics,due,today,scores,streak}=await import(new URL('model.mjs',temp));
const {backupData,restoreBackup}=await import(new URL('native.mjs',temp));
const {cashBalance}=await import(new URL('summary.mjs',temp));
assert.equal(metrics.length,38);assert.equal(useStore.getState().entries.length,0);assert.equal(useStore.getState().demo,false);
const h={id:'test',name:'Read',group:'Mind',days:[0,1,2,3,4,5,6],created:today};
useStore.getState().patch({habits:[h],start:today});useStore.getState().toggle(today,h.id);
assert.equal(streak([h],useStore.getState().completed,today),1);assert.equal(scores([h],useStore.getState().completed,today).at(-1).you,1);
assert.equal(JSON.parse(stored).state.completed[today][0],'test');
useStore.getState().toggle(today,h.id);assert.equal(useStore.getState().completed[today].length,0);
assert.equal(due({...h,archived:today},today),false);
useStore.getState().save({id:'weight',metric:'weight',date:today,value:72.4,note:''});
useStore.getState().save({id:'weight',metric:'weight',date:today,value:73,note:'updated'});assert.equal(useStore.getState().entries.length,1);
backupData();assert.equal(exported[0],'king-progress-backup.json');const backup=exported[2];
useStore.getState().remove('weight');await restoreBackup(new File([backup],'backup.json'));assert.equal(useStore.getState().entries[0].value,73);
const before=stored;await assert.rejects(()=>restoreBackup(new File(['{"format":"king-progress-v1","state":{}}'],'bad.json')));assert.equal(stored,before);
const entry=(metric,date,value)=>({id:metric+date,metric,date,value,note:''});
assert.equal(cashBalance([entry('income','2026-10-01',100),entry('balance','2026-10-02',500),entry('spending','2026-10-03',40)]),460);
assert.equal(cashBalance([entry('income','2026-10-01',100),entry('spending','2026-10-02',20)]),80);
console.log('Progress checks passed: native persistence, score/streak, archive, edit, full backup round-trip, invalid backup preservation, cash snapshot accounting.');
