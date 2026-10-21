/* Mei's LX mobile host. Only the explicit native functions below cross the boundary. */
(() => {
  'use strict';
  const http = globalThis.__meiHttp, cancel = globalThis.__meiCancel;
  const nativeCrypto = globalThis.__meiCrypto, delay = globalThis.__meiDelay, clearTimer = globalThis.__meiClearTimer;
  delete globalThis.__meiHttp; delete globalThis.__meiCancel;
  delete globalThis.__meiCrypto; delete globalThis.__meiDelay; delete globalThis.__meiClearTimer;
  const codec = data => JSON.parse(nativeCrypto(JSON.stringify(data)));
  const bytes = value => typeof value === 'string' ? codec({op:'from', value, format:'utf8'}) : Array.from(value || []);
  const buffer = {
    from(value, format = 'utf8') {
      return new Uint8Array(typeof value === 'string' ? codec({op:'from', value, format}) : bytes(value));
    },
    bufToString(value, format = 'utf8') { return codec({op:'string', value:bytes(value), format}); }
  };
  const timers = new Map();
  let nextId = 0, inited = false, handler, capabilities, scriptInfo, alerted = false;
  const results = new Map();
  const events = Object.freeze({inited:'inited', request:'request', updateAlert:'updateAlert'});
  globalThis.setTimeout = (fn, timeout = 0, ...args) => {
    if (typeof fn !== 'function') throw new TypeError('Callback must be a function');
    if (timers.size >= 64) throw new Error('Too many timers');
    const id = String(++nextId);
    timers.set(id, true);
    delay(id, Math.max(0, Number(timeout) || 0)).then(() => {
      if (timers.delete(id)) fn(...args);
    });
    return id;
  };
  globalThis.clearTimeout = id => { if (timers.delete(String(id))) clearTimer(String(id)); };
  globalThis.console = Object.freeze({log(){}, info(){}, warn(){}, error(){}, debug(){}});
  const lx = {
    version:'2.0.0', env:'mobile', EVENT_NAMES:events,
    get currentScriptInfo() { return scriptInfo; },
    on(event, callback) {
      if (event !== events.request || typeof callback !== 'function') throw new Error('Unsupported event');
      handler = callback;
    },
    send(event, info) {
      if (event === events.inited) {
        if (inited) return Promise.reject(new Error('Script is inited'));
        if (!info || !info.sources || typeof handler !== 'function') throw new Error('Invalid initialization');
        capabilities = JSON.parse(JSON.stringify(info.sources));
        inited = true;
      } else if (event === events.updateAlert) {
        // Never navigate, download, or evaluate an update URL supplied by a script.
        if (alerted) return Promise.reject(new Error('Update already reported'));
        alerted = true;
      } else throw new Error('Unsupported event');
      return Promise.resolve();
    },
    request(url, options = {}, callback) {
      if (typeof options === 'function') { callback = options; options = {}; }
      if (typeof callback !== 'function') throw new TypeError('Callback required');
      if (!/^https?:\/\//i.test(url)) throw new Error('Only HTTP URLs are supported');
      const id = String(++nextId);
      let aborted = false;
      const abort = () => { aborted = true; cancel(id); };
      abort.abort = abort;
      Object.defineProperty(abort, 'aborted', {get:() => aborted});
      http(JSON.stringify({id, url, options})).then(raw => {
        if (!aborted) { const response = JSON.parse(raw); callback(null, response, response.body); }
      }, () => { if (!aborted) callback(new Error('HTTP request failed'), null, null); });
      return abort;
    },
    utils:Object.freeze({
      buffer:Object.freeze(buffer),
      crypto:Object.freeze({
        md5:value => codec({op:'md5', value:typeof value === 'string' ? value : bytes(value)}),
        randomBytes:size => new Uint8Array(codec({op:'random', size})),
        aesEncrypt:(value, mode, key, iv) => new Uint8Array(codec({op:'aes', value:bytes(value), mode, key:bytes(key), iv:bytes(iv)})),
        rsaEncrypt:(value, key) => new Uint8Array(codec({op:'rsa', value:bytes(value), key}))
      })
    })
  };
  Object.defineProperty(globalThis, 'lx', {value:Object.freeze(lx), writable:false, configurable:false});
  Object.defineProperty(globalThis, '__meiHost', {value:Object.freeze({
    setup(info) { if (scriptInfo) throw new Error('Already set up'); scriptInfo = Object.freeze(info); },
    capabilities() {
      if (!inited) throw new Error('Script did not initialize');
      return JSON.stringify(capabilities);
    },
    request(key, info) {
      Promise.resolve().then(() => handler.call(lx, info)).then(
        value => results.set(key, {value}),
        () => results.set(key, {error:true})
      );
    },
    result(key) {
      const item = results.get(key); results.delete(key);
      if (!item || item.error || typeof item.value !== 'string' || item.value.length > 8192 || !/^https?:\/\//i.test(item.value)) {
        throw new Error('Source did not return an audio URL');
      }
      return item.value;
    }
  }), writable:false, configurable:false});
})();
