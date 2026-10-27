// cdp_client.js - Zero-dependency Chrome DevTools Protocol Client using Node 24 native WebSocket

export class CdpSession {
  constructor(wsUrl) {
    this.wsUrl = wsUrl;
    this.ws = null;
    this.id = 0;
    this.callbacks = new Map();
    this.eventListeners = new Map();
  }

  async connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.wsUrl);
      this.ws.onopen = () => resolve();
      this.ws.onerror = (err) => reject(err);
      this.ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.id && this.callbacks.has(msg.id)) {
          const { resolve, reject } = this.callbacks.get(msg.id);
          this.callbacks.delete(msg.id);
          if (msg.error) {
            reject(new Error(msg.error.message));
          } else {
            resolve(msg.result);
          }
        } else if (msg.method) {
          const listeners = this.eventListeners.get(msg.method) || [];
          for (const cb of listeners) {
            cb(msg.params);
          }
        }
      };
    });
  }

  on(event, callback) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event).push(callback);
  }

  async send(method, params = {}) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      this.callbacks.set(id, { resolve, reject });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }

  async close() {
    if (this.ws) {
      this.ws.close();
    }
  }

  async evaluate(expression) {
    const res = await this.send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true,
    });
    if (res.exceptionDetails) {
      throw new Error(`Eval error: ${JSON.stringify(res.exceptionDetails)}`);
    }
    return res.result?.value;
  }

  async navigate(url) {
    await this.send('Page.enable');
    const loadPromise = new Promise((resolve) => {
      const handler = () => {
        resolve();
      };
      this.on('Page.loadEventFired', handler);
      // Fallback timeout after 10s
      setTimeout(resolve, 10000);
    });
    await this.send('Page.navigate', { url });
    await loadPromise;
    // Wait a brief moment for Angular hydration/rendering
    await this.sleep(1000);
  }

  async waitForSelector(selector, timeoutMs = 10000) {
    const startTime = Date.now();
    while (Date.now() - startTime < timeoutMs) {
      const exists = await this.evaluate(`!!document.querySelector(${JSON.stringify(selector)})`);
      if (exists) return true;
      await this.sleep(250);
    }
    throw new Error(`Timeout waiting for selector: ${selector}`);
  }

  async click(selector) {
    await this.waitForSelector(selector);
    const clicked = await this.evaluate(`
      (() => {
        const el = document.querySelector(${JSON.stringify(selector)});
        if (!el) return false;
        el.scrollIntoView({ block: 'center', inline: 'center' });
        el.click();
        return true;
      })()
    `);
    if (!clicked) throw new Error(`Could not click selector: ${selector}`);
    await this.sleep(400);
  }

  async type(selector, text) {
    await this.waitForSelector(selector);
    await this.evaluate(`
      (() => {
        const el = document.querySelector(${JSON.stringify(selector)});
        if (!el) return;
        el.focus();
        el.value = ${JSON.stringify(text)};
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
      })()
    `);
    await this.sleep(250);
  }

  async getText(selector) {
    await this.waitForSelector(selector);
    return await this.evaluate(`
      (() => {
        const el = document.querySelector(${JSON.stringify(selector)});
        return el ? (el.innerText || el.textContent || el.value || '') : null;
      })()
    `);
  }

  async captureScreenshot(filePath) {
    await this.send('Page.enable');
    const res = await this.send('Page.captureScreenshot', { format: 'png' });
    const fs = await import('node:fs/promises');
    await fs.writeFile(filePath, Buffer.from(res.data, 'base64'));
    return filePath;
  }

  sleep(ms) {
    return new Promise((r) => setTimeout(r, ms));
  }
}

export async function createOrAttachPage(url = 'http://localhost:4200') {
  const listRes = await fetch('http://127.0.0.1:9222/json/list');
  const tabs = await listRes.json();
  let pageTab = tabs.find((t) => t.type === 'page' && !t.url.startsWith('chrome://inspect'));

  if (!pageTab) {
    const newRes = await fetch(`http://127.0.0.1:9222/json/new?${encodeURIComponent(url)}`, { method: 'PUT' });
    pageTab = await newRes.json();
  }

  const session = new CdpSession(pageTab.webSocketDebuggerUrl);
  await session.connect();
  await session.send('DOM.enable');
  await session.send('Runtime.enable');
  await session.send('Page.enable');
  return session;
}
