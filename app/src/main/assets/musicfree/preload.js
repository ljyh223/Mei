/* MusicFree CommonJS host, using Mei's existing HTTP and timer utilities. */
(() => {
  'use strict';
  const utilities = globalThis.lx;
  const modules = Object.assign(Object.create(null), globalThis.__musicFreeModules);
  delete globalThis.__musicFreeModules;
  let environment;
  const results = new Map();

  class HostBuffer extends Uint8Array {
    static from(value, format = 'utf8') {
      if (value instanceof ArrayBuffer) return new HostBuffer(new Uint8Array(value));
      return new HostBuffer(utilities.utils.buffer.from(value, format));
    }
    static alloc(length) { return new HostBuffer(length); }
    static isBuffer(value) { return value instanceof HostBuffer; }
    static concat(values) {
      const result = new HostBuffer(values.reduce((size, value) => size + value.length, 0));
      let offset = 0;
      for (const value of values) { result.set(value, offset); offset += value.length; }
      return result;
    }
    toString(format = 'utf8') { return utilities.utils.buffer.bufToString(this, format); }
  }
  globalThis.Buffer = HostBuffer;
  globalThis.TextEncoder = class { encode(value) { return HostBuffer.from(String(value)); } };
  globalThis.TextDecoder = class { decode(value) { return HostBuffer.from(value).toString(); } };
  globalThis.crypto = { getRandomValues(value) {
    const bytes = utilities.utils.crypto.randomBytes(value.byteLength);
    new Uint8Array(value.buffer, value.byteOffset, value.byteLength).set(bytes);
    return value;
  } };
  globalThis.atob = value => Array.from(HostBuffer.from(value, 'base64'), byte => String.fromCharCode(byte)).join('');
  globalThis.btoa = value => HostBuffer.from(Array.from(value, char => char.charCodeAt(0))).toString('base64');

  function axios(config, options) {
    if (typeof config === 'string') config = Object.assign({}, options, { url: config });
    if (config.responseType === 'arraybuffer' || config.responseType === 'stream') {
      return Promise.reject(new Error('Binary HTTP responses are not supported'));
    }
    if (config.params) {
      const params = config.paramsSerializer ? config.paramsSerializer(config.params) :
        modules.qs.stringify(config.params, { arrayFormat: 'repeat' });
      config = Object.assign({}, config, { url: config.url + (config.url.includes('?') ? '&' : '?') + params });
    }
    if (config.baseURL && !/^https?:\/\//i.test(config.url)) {
      config = Object.assign({}, config, { url: config.baseURL.replace(/\/$/, '') + '/' + config.url.replace(/^\//, '') });
    }
    return new Promise((resolve, reject) => {
      if (config.signal?.aborted) return reject(new Error('Request cancelled'));
      const headers = Object.assign({}, config.headers);
      if (config.data != null && typeof config.data === 'object' && !Object.keys(headers).some(key => key.toLowerCase() === 'content-type')) {
        headers['Content-Type'] = 'application/json';
      }
      const abort = utilities.request(config.url, {
        method: String(config.method || 'get').toUpperCase(), headers, body: config.data, timeout: config.timeout
      }, (error, response) => {
        if (error) return reject(error);
        let data = response.body;
        if (config.responseType === 'text' && typeof data !== 'string') data = JSON.stringify(data);
        if (config.responseType !== 'text' && typeof data === 'string') {
          try { data = JSON.parse(data); } catch (_) {}
        }
        const result = { data, status: response.statusCode, statusText: response.statusMessage, headers: response.headers, config };
        const valid = config.validateStatus ? config.validateStatus(result.status) : result.status >= 200 && result.status < 300;
        if (valid) resolve(result); else {
          const failed = new Error('HTTP request failed');
          failed.response = result;
          reject(failed);
        }
      });
      config.signal?.addEventListener?.('abort', abort, { once: true });
    });
  }
  function addMethods(client) {
    client.request = client;
    for (const method of ['get', 'delete', 'head', 'options']) client[method] = (url, config) => client(Object.assign({}, config, { method, url }));
    for (const method of ['post', 'put', 'patch']) client[method] = (url, data, config) => client(Object.assign({}, config, { method, url, data }));
    return client;
  }
  addMethods(axios);
  axios.create = defaults => addMethods((config, options) => {
    const input = typeof config === 'string' ? Object.assign({}, options, { url: config }) : config;
    return axios(Object.assign({}, defaults, input, { headers: Object.assign({}, defaults?.headers, input?.headers) }));
  });
  axios.default = axios;
  axios.isAxiosError = value => Boolean(value?.response);
  modules.axios = axios;
  modules.buffer = { Buffer: HostBuffer };
  globalThis.require = name => {
    if (!Object.prototype.hasOwnProperty.call(modules, name)) throw new Error('Unsupported plugin module: ' + name);
    return modules[name];
  };
  globalThis.module = { exports: {} };
  globalThis.exports = globalThis.module.exports;
  globalThis.fetch = async (url, options = {}) => {
    const response = await axios({ url, method: options.method, headers: options.headers, data: options.body, signal: options.signal,
      validateStatus: () => true, responseType: 'text' });
    return { ok: response.status >= 200 && response.status < 300, status: response.status,
      text: async () => typeof response.data === 'string' ? response.data : JSON.stringify(response.data),
      json: async () => typeof response.data === 'string' ? JSON.parse(response.data) : response.data };
  };
  Object.defineProperty(globalThis, '__mfHost', { value: Object.freeze({
    setup(value) {
      if (environment) throw new Error('Environment already set');
      environment = value;
      globalThis.env = Object.freeze({ os: value.os, appVersion: value.appVersion,
        getUserVariables: () => Object.assign({}, value.variables) });
    },
    inspect() {
      const plugin = module.exports;
      if (!plugin || typeof plugin.platform !== 'string' || !plugin.platform.trim()) throw new Error('Invalid MusicFree plugin');
      const canSearch = typeof plugin.search === 'function' &&
        (!plugin.supportedSearchType || plugin.supportedSearchType.includes('music'));
      const canResolve = typeof plugin.getMediaSource === 'function';
      if (!canSearch && !canResolve) throw new Error('Plugin has no music capability');
      const variables = Array.isArray(plugin.userVariables) ? plugin.userVariables.slice(0, 30)
        .filter(item => item && typeof item.key === 'string')
        .map(item => ({ key: item.key.slice(0, 128), name: String(item.name || item.key).slice(0, 128) })) : [];
      return JSON.stringify({ name: plugin.platform.slice(0, 128), version: String(plugin.version || '').slice(0, 64),
        author: typeof plugin.author === 'string' ? plugin.author.slice(0, 128) : '',
        canSearch, canResolve, userVariables: variables });
    },
    call(key, method, args) {
      if (!['search', 'getMediaSource'].includes(method)) throw new Error('Unsupported operation');
      Promise.resolve().then(() => {
        const implementation = module.exports[method];
        if (typeof implementation !== 'function') return method === 'getMediaSource' ? args[0] : null;
        return implementation.apply(module.exports, args);
      }).then(value => {
        const result = JSON.stringify(value ?? null);
        results.set(key, result.length <= 2 * 1024 * 1024 ? { result } : { error: true });
      }, () => results.set(key, { error: true }));
    },
    result(key) {
      const value = results.get(key);
      results.delete(key);
      if (!value || value.error) throw new Error('Plugin request failed');
      return value.result;
    }
  }) });
})();
