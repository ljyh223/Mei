These are generated, copyright-free test fixtures: half a second of a 440 Hz sine wave and a 16 × 16 solid-color cover.

Regenerate them with FFmpeg:

```sh
ffmpeg -f lavfi -i 'sine=frequency=440:sample_rate=44100' -t 0.5 -q:a 9 -map_metadata -1 -y blank.mp3
ffmpeg -f lavfi -i 'sine=frequency=440:sample_rate=44100' -t 0.5 -c:a flac -map_metadata -1 -y blank.flac
ffmpeg -f lavfi -i 'sine=frequency=440:sample_rate=44100' -t 0.5 -c:a aac -b:a 64k -map_metadata -1 -y blank.m4a
ffmpeg -f lavfi -i 'color=c=purple:s=16x16:d=0.1' -frames:v 1 -y cover.jpg
ffmpeg -f lavfi -i 'color=c=purple:s=16x16:d=0.1' -frames:v 1 -y cover.png
```
