import { afterEach, beforeEach, vi } from "vitest";

beforeEach(() => {
  document.body.innerHTML = "";
  localStorage.clear();
  sessionStorage.clear();
  document.cookie = "AUTH_TOKEN=; Max-Age=0; path=/";
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  vi.restoreAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});
