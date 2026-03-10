import { afterEach, beforeEach, vi } from "vitest";

/**
 * Node v25 + jsdom: the built-in localStorage/sessionStorage are unreliable
 * (methods like getItem/setItem may not be functions). Replace them with a
 * simple in-memory implementation that is fully spec-compliant for tests.
 */
class InMemoryStorage {
  constructor() {
    this._store = Object.create(null);
  }
  get length() {
    return Object.keys(this._store).length;
  }
  key(index) {
    return Object.keys(this._store)[index] ?? null;
  }
  getItem(key) {
    return Object.prototype.hasOwnProperty.call(this._store, key)
      ? this._store[key]
      : null;
  }
  setItem(key, value) {
    this._store[key] = String(value);
  }
  removeItem(key) {
    delete this._store[key];
  }
  clear() {
    this._store = Object.create(null);
  }
}

// Install mock storages once, before any test module loads
const mockLocalStorage = new InMemoryStorage();
const mockSessionStorage = new InMemoryStorage();

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
  writable: true,
  configurable: true,
});
Object.defineProperty(window, "sessionStorage", {
  value: mockSessionStorage,
  writable: true,
  configurable: true,
});

beforeEach(() => {
  document.body.innerHTML = "";
  mockLocalStorage.clear();
  mockSessionStorage.clear();
  document.cookie = "AUTH_TOKEN=; Max-Age=0; path=/";
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  vi.restoreAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});
