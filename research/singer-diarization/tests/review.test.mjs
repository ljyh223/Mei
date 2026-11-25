import test from 'node:test';
import assert from 'node:assert/strict';
import {makeExport} from '../tools/review.mjs';

const song={song_id:'123',revision:'r1',lines:[
  {line_index:0,start_ms:0,end_ms:1000,label:'A',reviewed:true},
  {line_index:1,start_ms:1000,end_ms:2000,label:'B',reviewed:true},
]};
test('legacy reviewed flags never auto-confirm manual decisions',()=>{
  assert.deepEqual(makeExport(song,{rows:{},offset:0,version:true,decision:'two_singers'}).lines,[]);
});
test('both a chosen identity and explicit timing check are required',()=>{
  const s={rows:{0:{label:'A',timing:false},1:{timing:true}},offset:0,version:true,decision:'two_singers'};
  assert.equal(makeExport(song,s).lines.length,0);
  s.rows[1].label='A+B';
  assert.deepEqual(makeExport(song,s).lines.map(x=>x.label),['A+B']);
});
test('manual edits preserve intervals and revision for import validation',()=>{
  const r=makeExport(song,{rows:{0:{label:'B',timing:true,note:'correction'}},offset:50,version:true,decision:'two_singers'});
  assert.equal(r.revision,'r1'); assert.equal(r.offset_ms,50);
  assert.equal(r.lines[0].start_ms,0); assert.equal(r.lines[0].note,'correction');
});
