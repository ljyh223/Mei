# 歌词行演唱者识别研究

## 目标

输入一首歌的音频和已同步歌词，输出每行的演唱者集合。产品可据此决定靠左、靠右或保持普通排版。第一阶段限定双人歌曲，标签为 `A`、`B`、`A+B`、`unknown`；`A/B` 仅在一首歌内区分歌手，不代表真实姓名或固定左右位置。

先测量**逐行分配正确率**和**可自动排版的覆盖率**。歌词文本识别、逐字同步和歌手真实姓名识别暂不属于第一阶段。

## 第一阶段：20 首歌曲的小规模数据集

1. 先用 2–3 首歌曲打通导入与人工核对，再扩到 20 首包含明显轮唱的双人歌曲。音频和歌词必须是同一版本；现场版、剪辑版、伴奏版另算一首。
2. 每首保留完整音频、同步歌词和来源记录。先人工核对时间偏移，再标注每行 `A`、`B`、`A+B` 或 `unknown`。一句中途换人且无法准确拆分时标为 `unknown`。
3. 留出 5 首作为测试集，从一开始就不依据它们调算法。训练集与测试集按**歌曲**划分；若要评估陌生歌手，还需按歌手组合划分。
4. 先做无需训练的声音特征基线，再决定是否增加歌曲和训练小模型。记录每类标签的数量，避免只有独唱样本。

`data/` 存放本地音频、歌词和标签，已被本目录的 `.gitignore` 排除。不要把受版权保护的原始音频或歌词提交到仓库。

## 数据格式

每首歌有一个稳定的 `song_id`。建议本地使用：

```text
data/
  audio/<song_id>.wav
  lyrics/<song_id>.ttml
  labels/<song_id>.jsonl
  songs.jsonl
```

`songs.jsonl` 每行是一首歌，记录 `song_id`、`audio_path`、`lyric_path`、`audio_version`、`lyric_source`、`split`（`train`/`test`）以及时间偏移 `offset_ms`。路径相对于本研究目录。`offset_ms` 是“音频时间减歌词时间”，核对后填写。

`labels/<song_id>.jsonl` 每行是一条歌词：

```json
{"song_id":"demo-duet","line_index":0,"start_ms":1000,"end_ms":3000,"text":"示例歌词","agent_ids":["A"],"label":"A","label_source":"manual","reviewed":true}
```

- 时间使用毫秒，且 `start_ms < end_ms`。保存原始歌词时间；分析音频时再加 `offset_ms`。
- `agent_ids` 保存来源中的原始歌手标识；`agent_type` 保存 TTML 声明的 agent 类型。`label` 是人工核对后的研究标签。`A+B` 表示两人同时唱这一行；无法判断填 `unknown`，不猜测。
- `label_source` 可为 `manual`、`ttml` 或 `lrc_role`。从现有歌词导入的标记都先设 `reviewed=false`，听音频核对后再改为 `true`。只用已核对的行计算测试指标。

## 从 TTML 导入候选标签

```bash
python3 tools/import_ttml.py --song-id demo-duet --input data/lyrics/demo-duet.ttml --output data/labels/demo-duet.jsonl
```

脚本提取主歌词行时间、文本和 `ttm:agent`，跳过翻译、音译及 `x-bg` 背景人声，**不自动把 agent 映射为 A/B**。TTML 中的 agent 可能表示合唱组，也可能缺失；背景人声还可能影响 `A+B` 标签，必须结合音频核对。

## 从 NCM TTML 库筛选候选歌曲并取回音频

扫描 `ncm-lyrics/<数字>.ttml`，只将实际歌词行使用了至少两个不同 `type="person"` agent 的文件列为候选。每个文件的网易云 ID 取自文件名；如果文件里声明了 `ncmMusicId`，还会核对它是否包含这个 ID。默认不把 `type="group"` 合唱标记算成独立歌手，也不把 `type="other"` 算入，以降低把独唱加和声误判成对唱的情况。

```bash
python3 tools/find_ncm_duets.py --no-write  # 只预览数量和前 20 个 ID
python3 tools/find_ncm_duets.py             # 写出 ID 和 JSONL 详情
python3 tools/find_ncm_duets.py --max-agents 2 --ids-output data/candidates/two_person_ids.txt --report-output data/candidates/two_person_report.jsonl
```

默认扫描 `/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics`，也可用 `--input-dir` 指定别处。全部候选写入被 git 忽略的 `data/candidates/ids.txt` 和 `report.jsonl`。加 `--max-agents 2` 可只取双人歌曲；报告包含标题、歌手标记及其歌词行数量。

取回音频前，把只含 cookie 值的 `MUSIC_U` 放进环境变量，再给脚本 api-enhanced 的本地地址。脚本默认仅处理前 20 个 ID，使用 `exhigh`，不请求解锁；签名音频 URL 不写入文件，也不会把 cookie 转发给音乐 CDN。

```bash
export MUSIC_U='你的MUSIC_U值'
python3 tools/fetch_ncm_audio.py --api-base http://127.0.0.1:3000 --limit 20
```

也可用 `--level standard` 降低文件体积，或显式添加 `--unblock`。音频保存到 `data/audio/`，歌曲详情和下载状态记录在 `data/candidates/downloads.jsonl`。无可用音频 URL 的歌曲会保留在清单中并标为 `no_audio_url`。

接口可能返回 30/45 秒试听片段，下载成功不等于拿到整首歌。用下面的命令将音频时长与 TTML 最后一行时间比较，只把覆盖率达到 90% 的样本先列为完整音频。该检查需要安装 FFmpeg（含 `ffprobe`）。

```bash
python3 tools/check_audio_coverage.py
```

### 当前第一批结果

20 个下载请求里有 19 个返回了音频文件，但完整度检查发现只有 6 个文件覆盖了整首歌，13 个是 30/45 秒试听或不匹配版本，另有 1 个没有音频 URL。`downloads.jsonl` 里的 `downloaded` 只表示拿到了文件，不表示它是完整歌曲。完整候选 ID：`307081`、`852226`、`494865824`、`524148352`、`545728377`、`1300598277`。

人工试听确认后，首批 6 首完整音频中有 4 首可用：`494865824`（Havana）、`852226`（現夢 -genmu-）、`307081`（表白）和 `545728377`（Chinatown Blues）；`524148352`（一口）几乎是同一人演唱，`1300598277`（The Mob）音色不适合当前研究，已排除。决定记录在 `data/candidates/manual_audio_review.jsonl`。

这 4 首目前共导入 234 行歌词，其中 225 行为已核对的 `A/B` 标签、9 行保留为 `unknown`：Havana 为 A 42 / B 28；現夢为 A 22 / B 29，另有 8 行 `type="other"`；表白为 A 54 / B 4，另有 1 行无可用 person agent；Chinatown Blues 为 A 26 / B 20。表白的网易云详情只列一位歌手，但 TTML 标有两个 person agent；因此保留用户试听确认的歌词角色，歌手身份只用歌曲内部 A/B 表示。每首的映射不跨歌曲复用。

**筛选口径和局限：**扫描本地 3,585 个 TTML 文件，筛出实际歌词行至少用了两个不同 `type="person"` agent 的文件。它只筛 TTML 标注结构，不听音频，也不判断 agent 是否真是两个可区分的歌手；这就是“一口”等误报的原因。结果有 71 个 NCM ID，但按 TTML 文件内容去重后只有 52 份，因为同一份歌词可能对应多个版本或曲库 ID。第一批只下载前 20 个 ID，且其中 13 个是试听片段；可用数据还需经过音频版本和听感复核。要扩到 20 首，需要继续从其余候选中取回完整音频并人工筛选；若本地 TTML 库本身没有足够样本，再换用其他有歌词和演唱者标注的数据源。

## 可参考的数据来源

- [Suda 等人 2022 年论文的数据页](https://www.gavo.t.u-tokyo.ac.jp/~hitoshi/diarization/)有歌手时间段标注，但没有可下载的歌曲音频，因此不能单独构成可训练样本。
- [FruitsMusic](https://huggingface.co/datasets/fruits-music/fruits-music)有“谁在何时唱什么”的标注。它有专门的[许可条款](https://huggingface.co/datasets/fruits-music/fruits-music/blob/main/LICENSE.md)及下载确认流程；使用前先核对条款、音频取得方式和与目标歌曲的差异。它以多人日语偶像歌曲为主，可用于研究参考，不能替代双人歌曲测试集。
- [IdolSongsJp](https://huggingface.co/datasets/imprt/idol-songs-jp)含多轨音频；其[许可条款](https://huggingface.co/datasets/imprt/idol-songs-jp/blob/main/LICENSE.md)区分研究与产品使用，并限制器乐信号用于模型训练。暂不自动下载或并入本项目数据。

目前从本机 TTML 库筛出的歌词仍由其原始库提供；研究目录中的音频、导入标签和试听筛选记录被 `.gitignore` 排除，不应提交到仓库。后续优先处理剩余候选中按歌词内容去重后的歌曲，并按完整音频和实际听感筛选。
