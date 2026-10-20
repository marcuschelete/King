import {defineConfig} from 'vite';
import {copyFileSync} from 'node:fs';
export default defineConfig({base:'./',build:{outDir:'../android/app/src/main/assets/progress',emptyOutDir:true,target:'es2020'},plugins:[{name:'dependency-notices',closeBundle(){copyFileSync('THIRD_PARTY_NOTICES.txt','../android/app/src/main/assets/progress/THIRD_PARTY_NOTICES.txt')}}]});
