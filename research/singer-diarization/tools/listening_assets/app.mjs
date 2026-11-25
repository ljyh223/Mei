export function newSegment(start, end) {
  return {start_ms:Math.max(0,start),end_ms:Math.max(start+1,end),activity:'uncertain',
    foreground_ids:[],background_ids:[],unknown_foreground:false,unknown_background:false,
    arrangement:'uncertain',self_overdub:'uncertain',certainty:'uncertain',boundary_quality:'uncertain',reviewed:false,note:''};
}
export function blankDocument(task) {
  const points=[0,Math.floor(task.lines.length/2),task.lines.length-1];
  return {schema_version:2,song_id:task.song_id,revision:task.revision,audio_sha256:task.audio.sha256,
    lyric_sha256:task.lyric_sha256,reviewer:'',version_description:'',version_confirmed:false,
    offset_ms:0,song_decision:'uncertain',completion:'draft',note:'',speakers:[],
    task_checks:{whole_song_listened:false,unlyric_vocals_checked:false,final_consistency_checked:false},
    anchors:['beginning','middle','ending'].map((position,i)=>({position,lyric_ms:task.lines[points[i]]?.start_ms||0,audio_ms:task.lines[points[i]]?.start_ms||0,evidence:'',checked:false})),
    lines:task.lines.map(l=>({line_index:l.line_index,status:'pending',window_start_ms:l.start_ms,window_end_ms:l.end_ms,segments:[],note:''})),extra_segments:[]};
}
export function splitSegment(segment, at) {
  if(!Number.isInteger(at)||at<=segment.start_ms||at>=segment.end_ms) throw new Error('拆分位置必须在片段内部');
  return [{...structuredClone(segment),end_ms:at,reviewed:false},{...structuredClone(segment),start_ms:at,reviewed:false}];
}
export function progress(document) {
  const segments=[...document.lines.flatMap(l=>l.segments),...document.extra_segments];
  return {finished:document.lines.filter(l=>['complete','not_vocal','excluded_text'].includes(l.status)).length,
    total:document.lines.length,reviewed:segments.filter(s=>s.reviewed).length,
    uncertain:segments.filter(s=>s.reviewed&&(s.certainty==='uncertain'||s.unknown_foreground||s.unknown_background)).length};
}

if(typeof document!=='undefined') {
  const el=id=>document.getElementById(id);
  let catalog,task,state,stopAt=null,loadSerial=0;
  const key=()=>`singer-detail:v2:${task.song_id}:${task.revision}`;
  const names={missing:'未取得音频',ready_version_pending:'本地音频够长，版本待核对',partial_or_mismatch:'仅片段或版本可疑',multiple_recordings_need_selection:'存在多个本地录音，需先选定',probe_failed:'音频读取失败'};
  const riskNames={source_background_voice:'原歌词带背景声标记，须听音核对',invalid_source_interval:'原时间区间为零或倒置，必须修正／确认是否制作信息',short_source_line:'原歌词短于或等于 2 秒',source_timeline_overlap:'原歌词时间与其他行重叠',missing_source_role:'原歌词缺失角色'};
  function node(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
  function save(){if(!state)return;try{localStorage.setItem(key(),JSON.stringify(state));}catch{el('notice').textContent='浏览器暂存失败，请导出草稿备份。';}const p=progress(state);el('progress').textContent=`行状态标为完成 ${p.finished}/${p.total} · 片段已听 ${p.reviewed} · 身份待定片段 ${p.uncertain}（最终完整性由导入校验）`;}
  function input(label,value,change,type='text') {const wrap=node('label',label+' ');const field=node('input');field.type=type;if(type==='checkbox')field.checked=value===true;else field.value=value??'';field.onchange=()=>{change(type==='checkbox'?field.checked:field.value);save();};wrap.append(field);return wrap;}
  function textArea(label,value,change){const wrap=node('label',label);wrap.style.display='block';const f=node('textarea');f.value=value||'';f.oninput=()=>{change(f.value);save();};wrap.append(f);return wrap;}
  function select(label,value,values,change){const wrap=node('label',label+' '),f=node('select');for(const [id,text]of values){const o=node('option',text);o.value=id;f.append(o);}f.value=value;f.onchange=()=>{change(f.value);save();};wrap.append(f);return wrap;}
  function button(text,action){const n=node('button',text);n.onclick=action;return n;}
  function seconds(label,ms,change){return input(label,ms/1000,v=>change(Math.round(Number(v)*1000)),'number');}
  async function play(start,end){if(!task.audio.url){el('notice').textContent='尚无本地音频，请先固定版本。';return;}el('audio').currentTime=Math.max(0,start/1000-.3);stopAt=end/1000+.3;try{await el('audio').play();}catch{el('notice').textContent='播放失败，请检查本地音频文件。';}}
  function download(){state.exported_at=new Date().toISOString();const url=URL.createObjectURL(new Blob([JSON.stringify(state,null,2)],{type:'application/json'}));const a=node('a');a.href=url;a.download=`${task.song_id}-detailed-${state.completion}.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);save();}
  function renderSong(){
    const box=el('songFields');box.replaceChildren();
    box.append(input('核对人／稳定别名',state.reviewer,v=>state.reviewer=v),input('录音版本说明',state.version_description,v=>state.version_description=v),input('已核对为同一版本',state.version_confirmed,v=>state.version_confirmed=v,'checkbox'));
    box.append(input('全曲偏移 ms',state.offset_ms,v=>{state.offset_ms=Number(v);state.version_confirmed=false;state.anchors.forEach(a=>a.checked=false);},'number'));
    box.append(select('歌曲结论',state.song_decision,[['uncertain','待查'],['single','独唱'],['two','两人'],['multiple','三人及以上'],['exclude','排除']],v=>state.song_decision=v));
    box.append(select('本曲进度',state.completion,[['draft','草稿'],['partial','部分核对'],['complete','申请整首完成']],v=>state.completion=v));
    box.append(textArea('歌曲备注：录音来源、困难、疑点、排除原因',state.note,v=>state.note=v));
    el('anchors').replaceChildren();
    for(const a of state.anchors){const row=node('div');row.append(node('strong',({beginning:'前段',middle:'中段',ending:'后段'})[a.position]),seconds('原歌词时间',a.lyric_ms,v=>{a.lyric_ms=v;a.checked=false;}),seconds('实际音频时间',a.audio_ms,v=>{a.audio_ms=v;a.checked=false;}),input('定位词／听到的证据',a.evidence,v=>a.evidence=v),input('已核对',a.checked,v=>a.checked=v,'checkbox'));el('anchors').append(row);}
    el('checks').replaceChildren();for(const [id,text]of Object.entries({whole_song_listened:'已完整听过前中后及所有间奏',unlyric_vocals_checked:'已检查并记录歌词以外的人声',final_consistency_checked:'已回查身份编号全曲一致'})){el('checks').append(input(text,state.task_checks[id],v=>state.task_checks[id]=v,'checkbox'));}
  }
  function renderSpeakers(){el('speakers').replaceChildren();for(const [i,s]of state.speakers.entries()){
    const card=node('div',undefined,'segment');card.append(node('strong',`身份 ${s.id}`),input('称呼／听感描述',s.description,v=>s.description=v));
    card.append(select('声音来源',s.production,[['uncertain','不清楚'],['natural','真人'],['synthetic','合成']],v=>s.production=v));
    for(const [j,r]of s.references.entries()){const row=node('div');row.append(seconds('清晰参照片段开始',r.start_ms,v=>r.start_ms=v),seconds('结束',r.end_ms,v=>r.end_ms=v),button('试听参照',()=>play(r.start_ms,r.end_ms)),button('删除参照',()=>{s.references.splice(j,1);save();renderSpeakers();}));card.append(row);}
    card.append(button('新增独唱参照',()=>{const t=Math.round(el('audio').currentTime*1000);s.references.push({start_ms:t,end_ms:t+3000});save();renderSpeakers();}),input('无清晰参照时说明原因',s.reference_unavailable_reason,v=>s.reference_unavailable_reason=v));
    card.append(button('移除此身份',()=>{if(confirm(`移除 ${s.id} 后，片段中的引用也需要修正。继续？`)){state.speakers.splice(i,1);save();renderSpeakers();renderLines();}}));el('speakers').append(card);
  }}
  function segmentCard(segment,list,index,refresh){const card=node('div',undefined,'segment');
    const edited=()=>{segment.reviewed=false;const c=card.querySelector('input[data-review-confirm]');if(c)c.checked=false;const owner=state.lines.find(l=>l.segments===list);if(owner&&['complete','not_vocal','excluded_text'].includes(owner.status)){owner.status='partial';const article=card.closest('article');article?.classList.remove('line-done');const f=article?.querySelector('select[data-line-state]');if(f)f.value='partial';}};
    card.append(seconds('音频开始',segment.start_ms,v=>{segment.start_ms=v;edited();}),seconds('结束',segment.end_ms,v=>{segment.end_ms=v;edited();}),button('试听片段',()=>play(segment.start_ms,segment.end_ms)));
    card.append(button('在当前播放位置拆分',()=>{try{list.splice(index,1,...splitSegment(segment,Math.round(el('audio').currentTime*1000)));save();refresh();}catch(e){el('notice').textContent=e.message;}}));
    card.append(select('活动',segment.activity,[['uncertain','无法判断'],['singing','演唱'],['speech','念白／说话'],['no_vocal','无人声']],v=>{segment.activity=v;if(v==='no_vocal'){segment.foreground_ids=[];segment.background_ids=[];segment.unknown_foreground=false;segment.unknown_background=false;segment.arrangement='not_applicable';segment.self_overdub='not_applicable';}edited();refresh();}));
    for(const [field,title]of [['foreground_ids','主唱身份'],['background_ids','背景／和声身份']]){const row=node('div',title+'：');for(const s of state.speakers){row.append(input(s.id,segment[field].includes(s.id),on=>{segment[field]=on?[...segment[field],s.id]:segment[field].filter(x=>x!==s.id);edited();},'checkbox'));}row.append(input('还有身份不清的声音',segment[field==='foreground_ids'?'unknown_foreground':'unknown_background'],v=>{segment[field==='foreground_ids'?'unknown_foreground':'unknown_background']=v;edited();},'checkbox'));card.append(row);}
    card.append(select('演唱关系',segment.arrangement,[['uncertain','听不清／未判断'],['solo','独唱'],['unison','不同歌手同旋律齐唱'],['harmony','不同声部／和声'],['adlib','哼唱／衬词／即兴'],['not_applicable','无人声／不适用']],v=>{segment.arrangement=v;edited();}));
    card.append(select('同一人叠唱',segment.self_overdub,[['uncertain','不清楚'],['absent','未听出'],['present','有'],['not_applicable','不适用']],v=>{segment.self_overdub=v;edited();}));
    card.append(select('身份判断',segment.certainty,[['uncertain','仍有疑问'],['certain','确定']],v=>{segment.certainty=v;edited();}));
    card.append(select('时间边界',segment.boundary_quality,[['uncertain','仍有疑问'],['approximate','近似定位'],['precise','已细听定位']],v=>{segment.boundary_quality=v;edited();}));
    card.append(textArea('片段备注：疑点原因、额外歌手、背景人声、听不清的具体位置',segment.note,v=>segment.note=v));
    const confirmation=input('这段已按以上内容听音核对',segment.reviewed,v=>segment.reviewed=v,'checkbox');confirmation.querySelector('input').dataset.reviewConfirm='true';
    card.append(confirmation,button('删除片段',()=>{list.splice(index,1);save();refresh();}));return card;
  }
  function renderLines(){el('lines').replaceChildren();for(const row of state.lines){const source=task.lines.find(l=>l.line_index===row.line_index);if(!source)continue;const card=node('article',undefined,['complete','not_vocal','excluded_text'].includes(row.status)?'line-done':'');card.append(node('h3',`第 ${row.line_index+1} 行 · ${(source.start_ms/1000).toFixed(3)}–${(source.end_ms/1000).toFixed(3)} 秒`),node('div',source.text));
    if(source.source_risks.length)card.append(node('p','自动疑点（不是答案）：'+source.source_risks.map(r=>riskNames[r]||r).join('；'),'notice'));
    const candidate=node('details');candidate.append(node('summary','查看原歌词角色候选（非人工答案）'),node('p',`${source.agent_ids.join(',')||'缺失'}；类型 ${source.agent_type}；与第 ${source.overlapping_line_indices.map(i=>i+1).join(',')||'无'} 行存在源时间重叠`));if(source.source_background_spans?.length)candidate.append(node('p','原背景词候选：'+source.source_background_spans.map(b=>b.text).join(' / ')));card.append(candidate);
    card.append(seconds('本行实际音频窗口开始',row.window_start_ms,v=>{row.window_start_ms=v;row.status='partial';}),seconds('结束',row.window_end_ms,v=>{row.window_end_ms=v;row.status='partial';}),button('试听整行',()=>play(row.window_start_ms,row.window_end_ms)));
    const lineState=select('本行进度',row.status,[['pending','未开始'],['partial','部分记录'],['complete','所有片段已检查'],['uncertain','保留疑点'],['not_vocal','本窗口无人声'],['excluded_text','制作信息／非独立歌词（须备注）']],v=>row.status=v);lineState.querySelector('select').dataset.lineState='true';card.append(lineState);
    for(const [i,s]of row.segments.entries())card.append(segmentCard(s,row.segments,i,renderLines));
    card.append(button('添加本行片段',()=>{row.segments.push(newSegment(row.window_start_ms,row.window_end_ms));row.status='partial';save();renderLines();}),textArea('本行备注：句中换人、时间漂移或其它问题',row.note,v=>row.note=v));el('lines').append(card);
  }renderExtras();}
  function renderExtras(){el('extras').replaceChildren();for(const [i,s]of state.extra_segments.entries())el('extras').append(segmentCard(s,state.extra_segments,i,renderExtras));}
  function renderAll(){renderSong();renderSpeakers();renderLines();save();}
  async function load(sid){const serial=++loadSerial;el('audio').pause();stopAt=null;el('notice').textContent='';try{const res=await fetch(`tasks/${sid}.json`);if(!res.ok)throw new Error('任务文件不可读');const incoming=await res.json();if(serial!==loadSerial)return;task=incoming;state=task.previous_manual||blankDocument(task);try{const saved=JSON.parse(localStorage.getItem(key())||'null');if(saved?.schema_version===2&&saved.revision===task.revision)state=saved;}catch{el('notice').textContent='本浏览器暂存不可读，已载入文件中的记录。';}el('title').textContent=`${task.title} · ${task.song_id}`;el('meta').textContent=`元数据：${task.artists.join(' / ')} · ${task.lines.length} 行 · 本地录音：${names[task.audio.status]||task.audio.status}`;
    if(task.audio.url)el('audio').src=task.audio.url;else{el('audio').removeAttribute('src');el('audio').load();}
    el('audioBlock').hidden=!task.audio.url;el('blocker').hidden=task.audio.ready_for_detail;
    el('blocker').textContent=task.audio.ready_for_detail?'':'此录音未准备好：可保留粗筛记录和草稿，先不要投入正式逐句细标。缺失、试听片段或多个录音都不能当作固定完整版本。';
    if(task.manual_blocker)el('notice').textContent='旧人工记录属于另一个版本，未自动覆盖；请保留旧文件核对录音。';
    el('saveFinal').disabled=!task.audio.ready_for_detail;location.hash=sid;renderAll();renderCatalog();
  }catch(e){el('notice').textContent=e.message;}}
  function renderCatalog(){const q=el('search').value.toLowerCase().trim(),b=el('bucket').value;const rows=catalog.songs.filter(s=>(b==='all'||s.bucket===b)&&(!el('representatives').checked||s.is_representative)&&(!q||[s.song_id,s.title,...s.artists].join(' ').toLowerCase().includes(q)));el('listcount').textContent=`匹配 ${rows.length} 个 ID，先显示 ${Math.min(rows.length,150)} 个；可搜索定位。`;el('songs').replaceChildren();for(const s of rows.slice(0,150)){const bt=button(`${s.title} · ${s.song_id}\n${s.bucket_name} · ${names[s.audio_status]||s.audio_status}`,()=>load(s.song_id));bt.className='song'+(task?.song_id===s.song_id?' current':'');el('songs').append(bt);}}
  el('search').oninput=renderCatalog;el('bucket').onchange=renderCatalog;el('representatives').onchange=renderCatalog;
  el('back').onclick=()=>{el('audio').currentTime=Math.max(0,el('audio').currentTime-3);};
  el('speed').onchange=()=>{el('audio').playbackRate=Number(el('speed').value);};
  el('audio').ontimeupdate=()=>{el('clock').textContent=el('audio').currentTime.toFixed(3)+'s';if(stopAt!==null&&el('audio').currentTime>=stopAt){el('audio').pause();stopAt=null;}};
  el('audio').onerror=()=>{if(task?.audio.url)el('notice').textContent='音频加载失败，请检查文件或浏览器格式支持。';};
  el('addSpeaker').onclick=()=>{let n=0,id;do{id=String.fromCharCode(65+n++);}while(state.speakers.some(s=>s.id===id));state.speakers.push({id,description:'',production:'uncertain',references:[],reference_unavailable_reason:''});save();renderSpeakers();renderLines();};
  el('addExtra').onclick=()=>{const t=Math.round(el('audio').currentTime*1000);state.extra_segments.push(newSegment(t,t+3000));save();renderExtras();};
  el('saveDraft').onclick=()=>{state.completion='draft';download();renderSong();el('notice').textContent='已导出草稿。保留文件，不依赖浏览器暂存作为唯一备份。';};
  el('saveFinal').onclick=()=>{if(!state.version_confirmed||!state.reviewer.trim()){el('notice').textContent='请填写核对人并确认固定录音版本；未完成时先导出草稿。';return;}if(state.completion==='draft')state.completion='partial';download();el('notice').textContent='已导出待校验结果。需运行导入命令校验边界、身份、覆盖和版本后才写入正式记录。';renderSong();};
  el('restore').onchange=async()=>{try{const file=el('restore').files[0];if(!file)return;const incoming=JSON.parse(await file.text());if(incoming.schema_version!==2||incoming.song_id!==task.song_id||incoming.revision!==task.revision)throw new Error('歌曲或版本不匹配，不能覆盖当前任务。');if(!Array.isArray(incoming.lines)||!Array.isArray(incoming.speakers))throw new Error('导出文件不完整');if(confirm('用此文件替换当前浏览器进度？请先导出当前草稿。')){state=incoming;save();renderAll();}}catch(e){el('notice').textContent=e.message;}};
  try{const r=await fetch('catalog.json');if(!r.ok)throw new Error('清单加载失败');catalog=await r.json();const s=catalog.summary;el('totals').textContent=`完整池：${s.recording_ids} 个歌曲 ID，${s.lyric_groups} 组歌词内容。角色标记均未被自动当作人工答案。`;renderCatalog();const sid=location.hash.slice(1);await load(catalog.songs.some(s=>s.song_id===sid)?sid:catalog.songs[0].song_id);}catch(e){el('notice').textContent=`${e.message}。请通过本地 HTTP 服务打开任务台。`;}
}
