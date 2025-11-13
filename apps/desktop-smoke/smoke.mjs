import * as shared from '@forwardapp/shared-kmp';

const keys = Object.keys(shared);
if (typeof shared !== 'object') {
  throw new Error('Shared package did not load as an object');
}
console.log(`[smoke] Loaded @forwardapp/shared-kmp with ${keys.length} export(s).`);
console.log('[smoke] Sample exports:', keys.slice(0, 5));
