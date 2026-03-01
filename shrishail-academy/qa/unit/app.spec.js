import { beforeEach, describe, expect, it } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

describe("app.js UI helpers", () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <header id="header"></header>
      <button id="menuToggle"><span></span><span></span><span></span></button>
      <ul id="navLinks">
        <li><a class="nav-link" href="#one">One</a></li>
      </ul>
      <span id="currentYear"></span>
    `;
    loadBrowserScript("app.js");
  });

  it("shows validation message for invalid email fields", () => {
    const wrapper = document.createElement("div");
    const input = document.createElement("input");
    input.type = "email";
    input.required = true;
    input.value = "invalid-email";
    wrapper.appendChild(input);
    document.body.appendChild(wrapper);

    const valid = window.validateField(input);
    expect(valid).toBe(false);
    expect(wrapper.querySelector(".field-error")?.textContent).toMatch(/valid email/i);
  });

  it("toggles mobile menu class when menu button is clicked", () => {
    const navLinks = document.getElementById("navLinks");
    const menuToggle = document.getElementById("menuToggle");

    window.initializeMobileMenu();
    menuToggle.click();
    expect(navLinks.classList.contains("active")).toBe(true);
    menuToggle.click();
    expect(navLinks.classList.contains("active")).toBe(false);
  });
});
