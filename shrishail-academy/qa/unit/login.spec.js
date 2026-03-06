import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

async function flushAsyncWork() {
  await Promise.resolve();
  await Promise.resolve();
}

describe("login page flow", () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="errorAlert" style="display:none"></div>
      <div id="successAlert" style="display:none"></div>
      <form id="loginForm">
        <input id="email" type="email" />
        <input id="password" type="password" />
        <button id="loginBtn" type="submit">Sign In</button>
      </form>
      <div id="footerPlaceholder"></div>
    `;

    window.API = {};
    window.Auth = {
      login: vi.fn().mockResolvedValue({ role: "ADMIN" }),
      isLoggedIn: vi.fn().mockReturnValue(false),
    };
    window.getFooterHTML = vi.fn().mockReturnValue("<footer>Footer</footer>");
    vi.spyOn(window, "setTimeout").mockImplementation(() => 0);
  });

  it("submits credentials through Auth.login and shows success feedback", async () => {
    loadBrowserScript("login.js");
    document.dispatchEvent(new Event("DOMContentLoaded"));

    document.getElementById("email").value = "admin@example.com";
    document.getElementById("password").value = "Admin@123!";

    document
      .getElementById("loginForm")
      .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));

    await flushAsyncWork();

    expect(window.Auth.login).toHaveBeenCalledWith(
      "admin@example.com",
      "Admin@123!",
    );
    expect(document.getElementById("successAlert").textContent).toContain(
      "Login successful",
    );
    expect(document.getElementById("successAlert").style.display).toBe("block");
    expect(document.getElementById("loginBtn").disabled).toBe(false);
    expect(document.getElementById("loginBtn").textContent).toBe("Sign In");
    expect(window.setTimeout).toHaveBeenCalled();
    expect(document.getElementById("footerPlaceholder").innerHTML).toContain(
      "Footer",
    );
  });
});
