export const LABELS = ['A', 'B', 'A+B', 'unknown', 'skip'];

export function makeExport(song, state) {
  return {
    schema_version: 1, song_id: song.song_id, revision: song.revision,
    audio_version_confirmed: state.version === true, offset_ms: Number(state.offset),
    song_decision: state.decision, exported_at: new Date().toISOString(),
    lines: song.lines.filter(line => LABELS.includes(state.rows[line.line_index]?.label) &&
      state.rows[line.line_index]?.timing === true).map(line => ({
        line_index: line.line_index, start_ms: line.start_ms, end_ms: line.end_ms,
        label: state.rows[line.line_index].label, reviewed: true, timing_checked: true,
        note: state.rows[line.line_index].note || '',
      })),
  };
}

if (typeof document !== 'undefined') {
  const songs = JSON.parse(document.querySelector('#bundle').textContent).songs;
  const flagNames = {
    unknown_roles:'存在缺失或未知角色', group_roles:'存在组唱角色',
    extra_roles:'存在第三位及以上角色', background_vocals:'存在背景人声',
    metadata_id_mismatch:'歌曲 ID 与歌词元数据不一致', imbalanced_roles:'两位角色行数不均衡',
    outside_current_candidate_filter:'不满足当前候选筛选，请重点核对角色',
  };
  const el = id => document.getElementById(id);
  let song, state, stopAt = null;
  const key = () => `singer-review:v1:${song.song_id}:${song.revision}`;
  function save() {
    try { localStorage.setItem(key(), JSON.stringify(state)); }
    catch { el('notice').textContent = '浏览器无法保存进度，请及时导出备份。'; }
    const n = makeExport(song, state).lines.length;
    el('progress').textContent = `已核对 ${n} / ${song.lines.length} 行`;
  }
  function render() {
    el('lines').replaceChildren();
    for (const line of song.lines) {
      const row = state.rows[line.line_index] ||= {};
      const card = document.createElement('article');
      card.dataset.done = String(LABELS.includes(row.label) && row.timing === true);
      const meta = document.createElement('div'); meta.className = 'meta';
      meta.textContent = `第 ${line.line_index + 1} 行 · ${(line.start_ms / 1000).toFixed(2)}–${(line.end_ms / 1000).toFixed(2)}s · 候选 ${line.label} · 原角色 ${(line.agent_ids || []).join(',')} (${line.agent_type})`;
      const text = document.createElement('div'); text.className = 'lyric'; text.textContent = line.text;
      const play = document.createElement('button'); play.textContent = '试听本句';
      play.onclick = async () => {
        const offset = Number(state.offset);
        el('audio').currentTime = Math.max(0, (line.start_ms + offset) / 1000 - .25);
        stopAt = (line.end_ms + offset) / 1000 + .25;
        try { await el('audio').play(); } catch { el('notice').textContent = '音频播放失败，请检查本地文件与浏览器音频支持。'; }
      };
      card.append(meta, text, play);
      for (const label of LABELS) {
        const button = document.createElement('button'); button.textContent = label;
        button.setAttribute('aria-pressed', String(row.label === label));
        button.onclick = () => { row.label = label; save(); render(); };
        card.append(button);
      }
      const clear = document.createElement('button'); clear.textContent = '撤销该行确认';
      clear.onclick = () => { delete state.rows[line.line_index]; save(); render(); };
      const checkLabel = document.createElement('label');
      const check = document.createElement('input'); check.type = 'checkbox'; check.checked = row.timing === true;
      check.onchange = () => { row.timing = check.checked; save(); card.dataset.done = String(LABELS.includes(row.label) && row.timing); };
      checkLabel.append(check, '该行时间已核对');
      const note = document.createElement('input'); note.type = 'text'; note.placeholder = '备注：换人、第三位、和声、错位等'; note.value = row.note || '';
      note.oninput = () => { row.note = note.value; save(); };
      card.append(checkLabel, clear, document.createElement('br'), note);
      el('lines').append(card);
    }
  }
  function load(id) {
    el('audio').pause(); stopAt = null;
    el('notice').textContent = '';
    song = songs.find(s => s.song_id === id);
    const prior = song.previous_decisions || [];
    state = {offset: prior[0]?.offset_ms ?? 0, version: prior[0]?.audio_version_confirmed === true,
      decision: prior[0]?.song_decision || 'uncertain',
      rows: Object.fromEntries(prior.map(r => [r.line_index, {label:r.label, timing:r.timing_checked, note:r.note}]))};
    try { const saved = JSON.parse(localStorage.getItem(key()) || 'null');
      if (saved?.rows) state = {...state, ...saved, rows:{...state.rows, ...saved.rows}};
    } catch { el('notice').textContent = '暂存进度不可读，已载入文件中的核对记录。'; }
    el('audio').src = song.audio_url;
    el('info').textContent = `${song.title} · ${song.song_id} · ${(song.audio_duration_ms/1000).toFixed(1)}s · 注意：${song.review_flags.map(f=>flagNames[f] || f).join('；') || '仍需逐行听音确认'}`;
    el('offset').value = state.offset; el('version').checked = state.version;
    el('decision').value = state.decision; save(); render();
  }
  for (const s of songs) { const o = document.createElement('option'); o.value=s.song_id; o.textContent=`${s.title} (${s.song_id})`; el('songs').append(o); }
  el('songs').onchange = () => load(el('songs').value);
  el('offset').onchange = () => {state.offset = Number(el('offset').value); for(const r of Object.values(state.rows)) r.timing=false; save(); render();};
  el('version').onchange = () => {state.version=el('version').checked; save();};
  el('decision').onchange = () => {state.decision=el('decision').value; save();};
  el('audio').addEventListener('timeupdate', () => { if(stopAt !== null && el('audio').currentTime >= stopAt) {el('audio').pause(); stopAt=null;} });
  el('audio').addEventListener('error', () => {el('notice').textContent='音频加载失败：请通过本地 HTTP 服务打开本页，并保留 data/audio 文件。';});
  el('export').onclick = () => {
    const result = makeExport(song,state);
    if (!result.audio_version_confirmed || !Number.isInteger(result.offset_ms) || Math.abs(result.offset_ms)>600000 || !result.lines.length) {
      el('notice').textContent='请确认版本、填写有效整数偏移，并至少完成一行标签及时间核对。'; return;
    }
    const url=URL.createObjectURL(new Blob([JSON.stringify(result,null,2)],{type:'application/json'}));
    const a=document.createElement('a'); a.href=url; a.download=`${song.song_id}-review.json`; a.click();
    setTimeout(()=>URL.revokeObjectURL(url),1000);
    el('notice').textContent=`已导出 ${result.lines.length} 行。尚未核对的行没有被自动确认。`;
  };
  if(songs.length) load(songs[0].song_id);
  else {el('notice').textContent='没有可试听的完整本地候选，请检查清单中的阻塞原因。'; el('export').disabled=true;}
}
