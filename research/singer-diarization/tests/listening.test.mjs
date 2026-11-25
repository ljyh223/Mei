import test from 'node:test';
import assert from 'node:assert/strict';
import {blankDocument,newSegment,splitSegment,progress} from '../tools/listening_assets/app.mjs';
const task={song_id:'123',revision:'r',lyric_sha256:'lyric',audio:{sha256:'audio'},
 lines:[{line_index:0,start_ms:1000,end_ms:3000,agent_ids:['v1'],reviewed:true,label:'A'}]};
test('source roles and old reviewed flags never populate manual answers',()=>{
 const doc=blankDocument(task);
 assert.equal(doc.version_confirmed,false);assert.deepEqual(doc.speakers,[]);
 assert.deepEqual(doc.lines[0].segments,[]);assert.equal(doc.lines[0].status,'pending');
 assert.equal(progress(doc).reviewed,0);
});
test('new segment cannot auto-become a certain reviewed identity',()=>{
 const s=newSegment(1000,3000);assert.equal(s.reviewed,false);
 assert.equal(s.activity,'uncertain');assert.equal(s.certainty,'uncertain');assert.deepEqual(s.foreground_ids,[]);
});
test('split requires an internal boundary and both parts need re-confirmation',()=>{
 const s={...newSegment(1000,3000),reviewed:true,foreground_ids:['A']};
 const parts=splitSegment(s,2000);assert.equal(parts[0].end_ms,parts[1].start_ms);
 assert.ok(parts.every(p=>p.reviewed===false));assert.equal(s.reviewed,true);
 parts[0].foreground_ids.push('B');assert.deepEqual(s.foreground_ids,['A']);
 for(const t of [1000,3000,0,2000.5])assert.throws(()=>splitSegment(s,t));
});
test('unknown backing voice is retained in uncertainty progress',()=>{
 const d=blankDocument(task);d.lines[0].segments.push({...newSegment(1000,3000),reviewed:true,certainty:'certain',unknown_background:true});
 assert.equal(progress(d).uncertain,1);assert.equal(progress(d).finished,0);
});
