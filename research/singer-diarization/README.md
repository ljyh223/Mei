# 歌词行演唱者识别研究

输入歌曲音频和同步歌词，输出歌曲内匿名歌手的演唱区间／歌词行归属。当前先建设双人歌曲的逐行验证集；后续可扩展到多歌手、重叠演唱和学习型方法。项目目标见 `MISSION.md`，该任务书不代表已经实现的能力。

## 当前状态（2026-11-05）

**第二批筛选结果：**《发现爱》《珊瑚海》《小酒窝》《铁血丹心》《世界真细小》已由用户听音认可为标准候选，其中《发现爱》最适合优先开展逐句标注。五首尚未完成本地录音版本、逐行身份及时间轴确认，不计作已验证的逐行答案。

**最新试听反馈：** Havana 转为片段用途；現夢保留为相似音色困难样本；Chinatown Blues 保留主候选；表白确认为同一人，转为独唱负例，原双角色划分不再有效。旧记录不删除。本地试听页现保留两首可继续逐行核对的候选（現夢、Chinatown Blues）；不是四首都通过。第二批五首候选及采集入口见 [BATCH02.md](BATCH02.md)，音频尚未取得。下文第一批的 4 首／234 行为反馈前的历史盘点。

识别算法尚未实现。已具备候选筛选、音频下载与时长筛查、TTML 导入、本地逐句试听与人工结果校验导入。

重扫 3,585 份 TTML：

| 候选级别 | ID 数 | 按规范化歌词内容去重 |
|---|---:|---:|
| broad / confirmed | 1,676 | 1,108 |
| balanced | 1,095 | 707 |
| strict | 999 | 632 |

这些全部是**候选**，不是已确认的双人歌曲。`confirmed` 为兼容旧输出保留的文件名，只表示符合格式筛选条件。TTML 和 LYS 来自同一歌词库，不构成独立人工证据；脚本只检查 LYS 是否带有对唱布局属性，未核对逐行角色一致性。`person` / `other` 是候选角色类型，不能证明其对应不同的真实人类歌手。

当前 20 个采集 ID：13 个为试听／版本可疑，1 个缺失音频，6 个通过时长筛查。其中 2 首沿用此前人工排除决定，另外 4 首共 234 行进入逐句试听队列。原始标签有 225 行 `reviewed=true`、9 行未核对；这些历史标记完整保留，**不会自动计作本轮已验证答案**。本轮是否可用于评测只看显式导入的逐行人工记录。

## 先开始试听

**正式细标请使用新的完整入口：** [ANNOTATION_HANDBOOK.md](ANNOTATION_HANDBOOK.md)。运行 `python3 tools/prepare_listening.py` 后，在同一本地 HTTP 服务打开 `/data/listening/`。它整理全部候选、保留替代 ID 和源异常，支持身份参照、前中后定位点、句内分段、主唱／背景、同人叠唱、未知、歌词外人声及版本绑定。下方旧 `/data/review/` 页面和 schema 1 用于历史行级记录，不能代替本轮详细分段标注。

详见 [REVIEW_GUIDE.md](REVIEW_GUIDE.md)，其中列出 4 首歌曲的重点问题。

```bash
cd /home/ljyh/code/android/Mei/research/singer-diarization
python3 tools/prepare_review.py
python3 -m http.server 4174 --bind 127.0.0.1 --directory .
```

打开 <http://127.0.0.1:4174/data/review/>。可播放本地音频、点击试听本句、调整全曲偏移，逐行标注并导出 JSON。浏览器暂存不替代导出备份。该服务仅用于本机，停止命令为 Ctrl+C。

把导出文件写回本地数据集：

```bash
python3 tools/prepare_review.py --import-review /实际路径/494865824-review.json
```

导入前检查歌曲、音频／歌词／原标签版本、时间范围、逐行标签和时间确认。导入结果写入 `data/verified/`，不覆盖 `data/labels/`。允许分批试听、累计导出；若新导出遗漏已导入的行，导入会拒绝，以免丢失人工结果。偏移改变后所有行需重新确认时间。

## 标签和确认规则

| 标签 | 含义 |
|---|---|
| A / B | 本曲两位不同歌手，身份编号在全曲内保持一致，不代表男女或真实姓名 |
| A+B | 两位目标歌手在同一歌词行同时演唱；同一个人叠唱不算 |
| unknown | 歌手不清、第三位、句中换人且没有拆分、无法确认重叠等，备注具体原因 |
| skip | 制作信息、无演唱等不参与本阶段歌手归属的行 |

`unknown` 不等于无演唱，也不等于合唱。后续多人和片段级标注应使用活跃歌手集合，不能把所有复杂情形压成 A+B。

每行必须分别完成：选择身份标签、核对时间。歌曲级“音频可用／两个歌手”不能替代逐行核对。歌曲结论为 `uncertain` / `exclude` 的人工结果可以保存，但不会进入本阶段评测；unknown / skip 不计入 A/B/A+B 正确率，评测时必须另外报告它们的比例。

旧的 `tools/duet-annotator` 使用男女标签，不是本研究当前的身份标注入口；其导出不能直接映射成 A/B，必须重新确认身份。

## 文件与可迁移性

```text
data/
  audio/                 本地原音频
  lyrics/                本次复核所用 TTML 快照
  labels/                历史导入标签（保留）
  candidates/            候选筛选／下载／历史试听记录
  songs.jsonl            全部采集样本的版本、状态、偏移和评测资格
  review/                生成的试听页、输入包和 queue.tsv
  verified/              明确导入的逐行人工结果
```

`songs.jsonl` 的音频／歌词路径相对本研究目录，来源绝对路径只用作溯源。音频、歌词以及旧标签内容一起确定 `revision`，源内容变化后旧确认不再自动有效。音频文件哈希记录完全相同的文件，`lyric_group_id` 用于同歌词分组；二者均不能代替音频指纹和人工版本核查。

`offset_ms = 音频时间 - 歌词时间`，原始歌词时间保持不变。例如歌词 10s 对应音频 10.3s，偏移为 +300ms。首次核对前值为 null；试听页初始 0 只是可编辑起点。全曲偏移无法修复剪辑版、速度差和逐句漂移，应将歌曲标为待查。

当前全部 `split=unassigned`。待版本和身份复核后，先建立同曲／同录音分组，再按歌曲划分训练、验证、测试；未知歌手泛化实验还需补充真实歌手身份，并进行歌手隔离。不要随机按歌词行划分，更不要把所有人的 A/B 当跨歌曲固定类别。输入模型时剥离 `agent_ids`、人工标签、角色前缀等答案字段。

`data/` 已被 Git 排除。原音频、歌词和衍生标注的发布权限应分别核对，不随代码仓库公开提交；独立项目迁移时需单独迁移本地数据，不能仅 clone Git。

## 候选筛选

```bash
python3 tools/filter_ncm_duets.py --no-write
python3 tools/filter_ncm_duets.py
```

默认源目录为 `/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics`，可用 `--input-dir` / `--output-dir` 覆盖。

- broad：前两位候选角色各至少 1 行，两位合计占全部非空主歌词行至少 80%，相邻主歌词至少切换一次。
- confirmed：broad，且有 LYS 对唱布局属性，或根本没有 LYS；仅是格式支持，不是人工确认。
- balanced：confirmed，两位各至少 5 行、至少切换 5 次、合计至少 80%。
- strict：confirmed，两位各至少 8 行、至少切换 5 次、合计至少 90%；仅两个候选歌手角色，无 group、缺失／未知角色行，且元数据 ID 不冲突。

组唱、未知角色均计入覆盖率分母；它们或第三人打断相邻切换，不能删除中间行后伪造切换。背景人声另列风险，不据此确认或否认合唱。去重哈希使用规范化歌词文本，不依赖角色类型或时间戳；只是防泄漏分组线索，不证明两段音频相同。

输出保留 `ids_*`、`report.jsonl`、`preview.tsv`、`summary.json` 等旧文件名。每条候选带 `annotation_status=candidate_only`、`review_flags`、`lys_agent_agreement=not_checked`。`find_ncm_duets.py` 是旧的 person-only 扫描器，不再用于全量采集。

## 音频采集与检查

```bash
# MUSIC_U 通过本机环境变量提供，不写入命令记录、数据文件或仓库
python3 tools/fetch_ncm_audio.py --api-base http://127.0.0.1:3000 --limit 20
python3 tools/check_audio_coverage.py
```

下载默认使用新的 `ids_strict_unique.txt`，而不是旧的 person-only ID 池；采集接口仍需用户本机已配置的 api-enhanced。只返回候选 URL 不代表整首音频，`downloaded` 仅表示拿到文件。时长检查输出 `duration_pass`，明确不证明版本正确或歌词同步正确。再次采集时指定单独 manifest 或备份旧清单，避免覆盖历史下载记录。

## 验证

```bash
python3 -m unittest discover -s tests -v
node --test tests/review.test.mjs
```

覆盖筛选分母、伪切换、去重、同源证据语义以及人工确认与历史标记隔离。测试不会安装 APK，不会确认真实歌手身份或实际听感。

## 研究数据参考

- Suda 等（2022）：https://www.gavo.t.u-tokyo.ac.jp/~hitoshi/diarization/
- FruitsMusic：https://huggingface.co/datasets/fruits-music/fruits-music
- IdolSongsJp：https://huggingface.co/datasets/imprt/idol-songs-jp

这些来源需另查音频取得方式和许可。本地候选数量不能当作公开数据集规模，也不代表有可训练的完整音频。
