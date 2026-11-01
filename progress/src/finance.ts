export type Account={id:string;name:string;currency:string;openingMinor:number;opened:string;archived?:boolean};
export type Transaction={id:string;kind:'income'|'expense'|'transfer';accountId:string;toAccountId?:string;amountMinor:number;receivedMinor?:number;feeMinor:number;date:string;note:string};
export type Finance={accounts:Account[];transactions:Transaction[]};
export const emptyFinance=():Finance=>({accounts:[],transactions:[]});
const validDay=(d:unknown):d is string=>typeof d==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(d)&&!isNaN(Date.parse(d))&&new Date(d).toISOString().slice(0,10)===d;
const minor=(v:unknown)=>Number.isSafeInteger(v)&&Math.abs(Number(v))<=1e12;
export function parseMoney(value:string):number{if(!/^-?\d+(\.\d{1,2})?$/.test(value.trim()))throw Error('Use an amount with no more than two decimal places.');const sign=value.trim().startsWith('-')?-1:1;const [a,b='']=value.trim().replace('-','').split('.');const n=sign*(Number(a)*100+Number((b+'00').slice(0,2)));if(!minor(n))throw Error('Amount is too large.');return n}
export function validateFinance(value:unknown):Finance{
 const f=value as Finance;if(!f||!Array.isArray(f.accounts)||!Array.isArray(f.transactions))throw Error('Invalid accounts data.');
 const ids=new Set<string>();for(const a of f.accounts){if(!a||typeof a.id!=='string'||!a.id||ids.has(a.id)||typeof a.name!=='string'||!a.name.trim()||!['BWP','USD','ZAR','EUR','GBP'].includes(a.currency)||!minor(a.openingMinor)||!validDay(a.opened)||(a.archived!==undefined&&typeof a.archived!=='boolean'))throw Error('Invalid account.');ids.add(a.id)}
 const txids=new Set<string>();for(const t of f.transactions){if(!t||typeof t.id!=='string'||!t.id||txids.has(t.id)||!['income','expense','transfer'].includes(t.kind)||!ids.has(t.accountId)||!minor(t.amountMinor)||t.amountMinor<=0||!minor(t.feeMinor)||t.feeMinor<0||!validDay(t.date)||typeof t.note!=='string')throw Error('Invalid transaction.');
  const source=f.accounts.find(a=>a.id===t.accountId)!;if(t.date<source.opened)throw Error('Transaction date must be on or after the account opening date.');
  if(t.kind==='income'&&t.feeMinor>t.amountMinor)throw Error('Income fee cannot exceed income.');
  if(t.kind==='transfer'){const target=f.accounts.find(a=>a.id===t.toAccountId);if(!target||target.id===source.id||!minor(t.receivedMinor)||Number(t.receivedMinor)<=0||t.date<target.opened)throw Error('Choose a different destination account and a valid received amount.');if(target.currency===source.currency&&t.receivedMinor!==t.amountMinor)throw Error('Same-currency transfer amounts must match; record charges as fees.');}
  txids.add(t.id);
 }
 for(const balance of Object.values(balances(f)))if(!Number.isSafeInteger(balance))throw Error('Balance exceeds supported precision.');return f;
}
export function balances(f:Finance,through='9999-12-31'):Record<string,number>{const values:Record<string,number>={};for(const a of f.accounts)values[a.id]=a.opened<=through?a.openingMinor:0;for(const t of f.transactions){if(t.date>through)continue;values[t.accountId]+=(t.kind==='income'?t.amountMinor:-t.amountMinor)-t.feeMinor;if(t.kind==='transfer')values[t.toAccountId!]+=t.receivedMinor!;}return values}
export function totals(f:Finance,through='9999-12-31'){const values=balances(f,through),result:Record<string,number>={};for(const a of f.accounts)result[a.currency]=(result[a.currency]||0)+values[a.id];return result}
export function cashflow(f:Finance,from:string,through:string){const result:Record<string,{income:number;expense:number;fees:number}>={};for(const t of f.transactions){if(t.date<from||t.date>through)continue;const currency=f.accounts.find(a=>a.id===t.accountId)!.currency;const v=result[currency]||(result[currency]={income:0,expense:0,fees:0});if(t.kind==='income')v.income+=t.amountMinor;if(t.kind==='expense')v.expense+=t.amountMinor;v.fees+=t.feeMinor;}return result}
export function saveTransaction(f:Finance,t:Transaction){return validateFinance({...f,transactions:[...f.transactions.filter(x=>x.id!==t.id),t]})}
export const money=(n:number,currency:string)=>`${currency} ${(n/100).toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2})}`;
