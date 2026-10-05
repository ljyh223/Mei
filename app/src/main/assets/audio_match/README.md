The bundled `afp.js` and `afp.wasm.js` come from
[NeteaseCloudMusicApiEnhanced/api-enhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced/tree/2aab9957dfd5231e5b192aecdb177f93ace4c92e/public/audio_match_demo)
at commit `2aab9957dfd5231e5b192aecdb177f93ace4c92e`.
The upstream demo credits mos9527/ncm-afp. The upstream MIT license is in `LICENSE.txt`.

`index.html` is the app-local bridge. Only the generated fingerprint is sent to the match API;
the recorded samples are passed to the bundled JavaScript in an app asset WebView.
