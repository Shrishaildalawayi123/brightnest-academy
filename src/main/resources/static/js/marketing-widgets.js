(function () {
  const SITE_INFO = {
    name: "BrightNest Academy",
    phoneDisplay: "+91-7204193980",
    phoneHref: "+917204193980",
    city: "Bengaluru",
    address:
      "#662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bengaluru - 560078",
    directionsUrl:
      "https://www.google.com/maps/dir/?api=1&destination=12.907025,77.566184",
    whatsappMessage:
      "Hello BrightNest Academy, I would like to know about tuition classes.",
  };

  SITE_INFO.whatsappUrl = `https://wa.me/917204193980?text=${encodeURIComponent(SITE_INFO.whatsappMessage)}`;

  const CHATBOT_QUICK_REPLIES = [
    "Book a demo",
    "Fees for my child's class",
    "CBSE tuition near me",
    "Maths coaching Bengaluru",
  ];

  const POPUP_DISMISS_KEY = "brightnest_whatsapp_popup_dismissed_at";
  const POPUP_ENGAGED_KEY = "brightnest_whatsapp_engaged";
  const CHATBOT_SESSION_KEY = "brightnest_chatbot_session_id";
  const CHATBOT_MINIMIZED_KEY = "brightnest_chatbot_minimized";
  const LEAD_POPUP_DISMISS_KEY = "brightnest_demo_lead_popup_dismissed_at";

  function getChatbotSessionId() {
    let sessionId = localStorage.getItem(CHATBOT_SESSION_KEY);
    if (sessionId) {
      return sessionId;
    }
    sessionId =
      (window.crypto && window.crypto.randomUUID && window.crypto.randomUUID()) ||
      `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    localStorage.setItem(CHATBOT_SESSION_KEY, sessionId);
    return sessionId;
  }

  function init() {
    if (!isMarketingExperiencePage()) {
      return;
    }

    normalizeContactLinks();
    injectFloatingContactStack();
    initializeChatbot();
    bindWhatsAppTracking();
    scheduleWhatsappPopup();
    scheduleDemoLeadPopup();
    enhanceLeadGenSections();
    injectGlobalConversionSection();
    window.setTimeout(normalizeContactLinks, 200);
  }

  function isMarketingExperiencePage() {
    const path = (window.location.pathname || "").toLowerCase();
    return ![
      "/admin-dashboard.html",
      "/student-dashboard.html",
      "/login.html",
      "/register.html",
      "/manage-assignments.html",
      "/manage-schedules.html",
      "/manage-sessions.html",
    ].includes(path);
  }

  function normalizeContactLinks() {
    document.querySelectorAll('a[href^="tel:"]').forEach((link) => {
      link.href = `tel:${SITE_INFO.phoneHref}`;
    });

    document.querySelectorAll('a[href*="wa.me/917204193980"]').forEach((link) => {
      link.href = SITE_INFO.whatsappUrl;
      if (!link.dataset.sourcePage) {
        link.dataset.sourcePage = window.location.pathname || "/";
      }
    });

    const announcementPhone = document.querySelector(".announcement-bar__phone");
    if (announcementPhone) {
      announcementPhone.href = `tel:${SITE_INFO.phoneHref}`;
      announcementPhone.textContent = SITE_INFO.phoneDisplay;
    }

    const footerBrand = document.querySelector(".footer-nap strong");
    if (footerBrand) {
      footerBrand.textContent = SITE_INFO.name;
    }

    const footerNapItems = document.querySelectorAll(".footer-nap span");
    if (footerNapItems[0]) {
      footerNapItems[0].textContent = SITE_INFO.address;
    }

    const footerPhone = document.querySelector('.footer-col a[href^="tel:"]');
    if (footerPhone) {
      footerPhone.textContent = SITE_INFO.phoneDisplay;
    }

    const footerCall = document.querySelector('.footer-contact a[href^="tel:"]');
    if (footerCall) {
      footerCall.textContent = `Call: ${SITE_INFO.phoneDisplay}`;
    }
  }

  function injectFloatingContactStack() {
    if (document.getElementById("marketingFloatStack")) {
      return;
    }

    const stack = document.createElement("div");
    stack.className = "marketing-float-stack";
    stack.id = "marketingFloatStack";
    stack.innerHTML = `
      <div class="marketing-float-actions" id="marketingFloatActions" hidden>
        <button class="marketing-float-btn marketing-float-btn--chat" id="marketingChatToggle" type="button" aria-controls="marketingChatbot">
          Chat with us
        </button>
        <a class="marketing-float-btn marketing-float-btn--whatsapp" href="${SITE_INFO.whatsappUrl}" target="_blank" rel="noopener" data-source-page="${window.location.pathname || "/"}">
          WhatsApp
        </a>
        <a class="marketing-float-btn marketing-float-btn--demo" href="/demo.html">
          Book Demo
        </a>
      </div>
      <button class="marketing-float-launcher" id="marketingFloatLauncher" type="button" aria-expanded="false" aria-controls="marketingFloatActions" aria-label="Open support actions">
        <span class="marketing-float-launcher__icon">💬</span>
        <span class="marketing-float-launcher__label">Help</span>
      </button>
    `;

    document.body.appendChild(stack);
  }

  function initializeChatbot() {
    if (document.getElementById("marketingChatbot")) {
      return;
    }

    const chatbot = document.createElement("section");
    chatbot.className = "marketing-chatbot";
    chatbot.id = "marketingChatbot";
    chatbot.hidden = true;
    chatbot.setAttribute("aria-hidden", "true");
    chatbot.innerHTML = `
      <div class="marketing-chatbot__header">
        <div>
          <p class="marketing-chatbot__eyebrow">AI Student Assistant</p>
          <h3>BrightNest Academy</h3>
        </div>
        <button class="marketing-chatbot__close" id="marketingChatClose" type="button" aria-label="Close chat">×</button>
      </div>
      <div class="marketing-chatbot__messages" id="marketingChatMessages"></div>
      <div class="marketing-chatbot__quick" id="marketingChatQuickReplies"></div>
      <form class="marketing-chatbot__composer" id="marketingChatForm">
        <input id="marketingChatInput" type="text" maxlength="500" placeholder="Ask about courses, fees, ICSE, demo class..." autocomplete="off" />
        <button type="submit">Send</button>
      </form>
      <a class="marketing-chatbot__human" href="${SITE_INFO.whatsappUrl}" target="_blank" rel="noopener" data-source-page="${window.location.pathname || "/"}">
        Need human help? WhatsApp Us
      </a>
    `;

    document.body.appendChild(chatbot);

    const messages = chatbot.querySelector("#marketingChatMessages");
    addBotMessage(
      messages,
      "Hi 👋 Want to improve your child's marks? 🎯 Book a demo. You can also share Name, Class, and Phone here for a quick callback.",
    );
    renderQuickReplies(chatbot.querySelector("#marketingChatQuickReplies"), CHATBOT_QUICK_REPLIES);

    const toggleButton = document.getElementById("marketingChatToggle");
    const floatStack = document.getElementById("marketingFloatStack");
    const launcherButton = document.getElementById("marketingFloatLauncher");
    const actionsPanel = document.getElementById("marketingFloatActions");
    const closeButton = chatbot.querySelector("#marketingChatClose");
    const form = chatbot.querySelector("#marketingChatForm");
    const input = chatbot.querySelector("#marketingChatInput");

    let miniChip = document.getElementById("marketingChatMiniChip");
    if (!miniChip) {
      miniChip = document.createElement("button");
      miniChip.id = "marketingChatMiniChip";
      miniChip.className = "marketing-chatbot-minichip";
      miniChip.type = "button";
      miniChip.hidden = true;
      miniChip.setAttribute("aria-label", "Reopen chat");
      miniChip.textContent = "Chat";
      document.body.appendChild(miniChip);
    }

    const setActionsPanelOpen = (shouldOpen) => {
      if (!actionsPanel) {
        return;
      }
      actionsPanel.hidden = !shouldOpen;
      launcherButton?.setAttribute("aria-expanded", String(shouldOpen));
      floatStack?.classList.toggle("is-open", shouldOpen);
    };

    const setChatbotOpen = (shouldOpen) => {
      chatbot.hidden = !shouldOpen;
      chatbot.setAttribute("aria-hidden", String(!shouldOpen));
      if (shouldOpen) {
        chatbot.style.display = "";
        if (miniChip) {
          miniChip.hidden = true;
        }
        localStorage.setItem(CHATBOT_MINIMIZED_KEY, "false");
        setActionsPanelOpen(false);
      } else {
        // Force-hide for mobile browsers that may delay hidden state repaint on touch.
        chatbot.style.display = "none";
      }
    };

    const setChatbotMinimized = (shouldMinimize) => {
      if (!miniChip) {
        return;
      }
      miniChip.hidden = !shouldMinimize;
      localStorage.setItem(CHATBOT_MINIMIZED_KEY, shouldMinimize ? "true" : "false");
    };

    const closeChatbot = (event) => {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      setChatbotOpen(false);
      setChatbotMinimized(true);
    };

    const openChatbot = () => {
      setChatbotOpen(true);
      localStorage.setItem(POPUP_ENGAGED_KEY, "true");
      setChatbotMinimized(false);
      hideWhatsappPopup();
      window.setTimeout(() => input.focus(), 50);
    };

    launcherButton?.addEventListener("click", () => {
      const isOpen = launcherButton.getAttribute("aria-expanded") === "true";
      setActionsPanelOpen(!isOpen);
      hideWhatsappPopup();
    });

    actionsPanel?.addEventListener("click", () => {
      setActionsPanelOpen(false);
    });

    toggleButton?.addEventListener("click", () => {
      openChatbot();
    });

    miniChip?.addEventListener("click", () => {
      openChatbot();
    });

    closeButton?.addEventListener("click", closeChatbot);
    closeButton?.addEventListener("touchend", closeChatbot, { passive: false });
    closeButton?.addEventListener("pointerup", closeChatbot);

    form?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const message = input.value.trim();
      if (!message) {
        return;
      }
      input.value = "";
      localStorage.setItem(POPUP_ENGAGED_KEY, "true");
      await handleChatSubmission(chatbot, message);
    });

    chatbot.querySelector("#marketingChatQuickReplies")?.addEventListener("click", async (event) => {
      const button = event.target.closest("button[data-reply]");
      if (!button) {
        return;
      }
      localStorage.setItem(POPUP_ENGAGED_KEY, "true");
      await handleChatSubmission(chatbot, button.dataset.reply);
    });

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") {
        return;
      }
      if (!chatbot.hidden) {
        closeChatbot(event);
      }
      if (launcherButton?.getAttribute("aria-expanded") === "true") {
        setActionsPanelOpen(false);
      }
    });

    document.addEventListener("pointerdown", (event) => {
      const target = event.target;

      const clickedInsideWidget =
        (chatbot && chatbot.contains(target)) ||
        (floatStack && floatStack.contains(target)) ||
        (miniChip && miniChip.contains(target));

      if (clickedInsideWidget) {
        return;
      }

      if (!chatbot.hidden) {
        closeChatbot();
      }

      if (launcherButton?.getAttribute("aria-expanded") === "true") {
        setActionsPanelOpen(false);
      }
    });

    if (localStorage.getItem(CHATBOT_MINIMIZED_KEY) === "true") {
      setChatbotMinimized(true);
    }
  }

  async function handleChatSubmission(chatbot, message) {
    const messages = chatbot.querySelector("#marketingChatMessages");
    const quickReplies = chatbot.querySelector("#marketingChatQuickReplies");
    addUserMessage(messages, message);
    const typingIndicator = addTypingIndicator(messages);

    try {
      const response = await requestChatbotReply(message);
      await enrichQualifiedChatLeadFromMessage(message);
      typingIndicator.remove();
      addBotMessage(messages, response.reply);
      if (response.qualifiedLeadCaptured) {
        addBotMessage(
          messages,
          "Great. Please share: Name, Class, Phone. Example: My name is Kavya, Class 8, 9876543210",
        );
      }
      renderQuickReplies(quickReplies, response.suggestions || CHATBOT_QUICK_REPLIES);
    } catch (error) {
      typingIndicator.remove();
      addBotMessage(messages, fallbackReply(message));
      renderQuickReplies(quickReplies, CHATBOT_QUICK_REPLIES);
    }
  }

  async function requestChatbotReply(message) {
    const response = await fetch(`${resolveApiBaseUrl()}/chatbot/messages`, {
      method: "POST",
      headers: buildApiHeaders(),
      credentials: "include",
      body: JSON.stringify({ message, sessionId: getChatbotSessionId() }),
    });

    if (!response.ok) {
      throw new Error("Chatbot request failed");
    }

    const payload = await response.json();
    const data = payload.data || {};
    if (data.sessionId) {
      localStorage.setItem(CHATBOT_SESSION_KEY, data.sessionId);
    }
    return data;
  }

  function extractLeadDetailsFromMessage(message) {
    const text = (message || "").trim();
    if (!text) return null;

    const emailMatch = text.match(/([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/);
    const phoneMatch = text.match(/(\+?\d[\d\s-]{8,15}\d)/);
    const nameMatch = text.match(/(?:my name is|i am|this is)\s+([a-zA-Z][a-zA-Z\s'.-]{1,50})/i);
    const classMatch = text.match(/(?:class|grade|std|standard)\s*[:\-]?\s*([1-9]|1[0-2])/i);

    const payload = {
      sessionId: getChatbotSessionId(),
      name: nameMatch ? nameMatch[1].trim() : null,
      email: emailMatch ? emailMatch[1].trim().toLowerCase() : null,
      phone: phoneMatch ? phoneMatch[1].replace(/\s+/g, "").trim() : null,
      studentClass: classMatch ? `Class ${classMatch[1]}` : null,
    };

    if (!payload.name && !payload.email && !payload.phone && !payload.studentClass) {
      return null;
    }
    return payload;
  }

  async function enrichQualifiedChatLeadFromMessage(message) {
    const payload = extractLeadDetailsFromMessage(message);
    if (!payload) {
      return;
    }

    try {
      await fetch(`${resolveApiBaseUrl()}/chatbot/leads/enrich`, {
        method: "POST",
        headers: buildApiHeaders(),
        credentials: "include",
        body: JSON.stringify(payload),
      });
    } catch (_) {
      // Silent fail to avoid interrupting chat UX.
    }
  }

  function fallbackReply(message) {
    const normalized = message.toLowerCase();
    if (normalized.includes("fee") || normalized.includes("cost") || normalized.includes("price")) {
      return "Fees depend on subject, grade, and class mode. We share the exact fee after understanding the requirement. Demo class fee is INR 100, adjustable or refundable within 30 days if you do not enroll.";
    }
    if (normalized.includes("icse") || normalized.includes("cbse") || normalized.includes("board")) {
      return "Yes. BrightNest Academy supports ICSE, CBSE, and Karnataka State Board students with board-aligned teaching.";
    }
    if (normalized.includes("demo") || normalized.includes("book")) {
      return "You can book a demo class from the website demo page or chat with us on WhatsApp and our team will schedule it for you.";
    }
    if (normalized.includes("online") || normalized.includes("offline")) {
      return "We provide both online tuition through Google Meet and offline tuition at our Bengaluru center.";
    }
    return "I can help with courses, fees, demo booking, and board-specific tuition. For personal guidance, please chat with our team on WhatsApp.";
  }

  function addBotMessage(container, message) {
    const bubble = document.createElement("div");
    bubble.className = "marketing-chatbot__bubble marketing-chatbot__bubble--bot";
    bubble.textContent = message;
    container.appendChild(bubble);
    scrollChatToBottom(container);
  }

  function addUserMessage(container, message) {
    const bubble = document.createElement("div");
    bubble.className = "marketing-chatbot__bubble marketing-chatbot__bubble--user";
    bubble.textContent = message;
    container.appendChild(bubble);
    scrollChatToBottom(container);
  }

  function addTypingIndicator(container) {
    const bubble = document.createElement("div");
    bubble.className = "marketing-chatbot__bubble marketing-chatbot__bubble--bot marketing-chatbot__bubble--typing";
    bubble.textContent = "Typing...";
    container.appendChild(bubble);
    scrollChatToBottom(container);
    return bubble;
  }

  function scrollChatToBottom(container) {
    container.scrollTop = container.scrollHeight;
  }

  function renderQuickReplies(container, replies) {
    if (!container) {
      return;
    }

    container.innerHTML = (replies || [])
      .slice(0, 4)
      .map(
        (reply) =>
          `<button type="button" class="marketing-chatbot__chip" data-reply="${escapeHtmlAttribute(reply)}">${reply}</button>`,
      )
      .join("");
  }

  function bindWhatsAppTracking() {
    if (document.body.dataset.whatsappTrackingBound === "true") {
      return;
    }

    document.body.dataset.whatsappTrackingBound = "true";
    document.addEventListener("click", async (event) => {
      const link = event.target.closest('a[href*="wa.me/917204193980"]');
      if (!link) {
        return;
      }

      event.preventDefault();
      const url = SITE_INFO.whatsappUrl;
      const sourcePage = link.dataset.sourcePage || window.location.pathname || "/";

      localStorage.setItem(POPUP_ENGAGED_KEY, "true");
      hideWhatsappPopup();

      const trackingPromise = trackWhatsAppLead(sourcePage);

      if (link.target === "_blank") {
        window.open(url, "_blank", "noopener");
        trackingPromise.catch(() => {});
        return;
      }

      window.setTimeout(() => {
        window.location.href = url;
      }, 60);

      try {
        await Promise.race([trackingPromise, wait(140)]);
      } catch (_) {
        // The lead tracking call should never block the actual WhatsApp launch.
      }
    });
  }

  async function trackWhatsAppLead(sourcePage) {
    await fetch(`${resolveApiBaseUrl()}/whatsapp-leads`, {
      method: "POST",
      headers: buildApiHeaders(),
      credentials: "include",
      keepalive: true,
      body: JSON.stringify({ sourcePage }),
    });
  }

  function scheduleWhatsappPopup() {
    if (shouldSuppressPopup()) {
      return;
    }

    window.setTimeout(() => {
      if (shouldSuppressPopup()) {
        return;
      }
      showWhatsappPopup();
    }, 20000);
  }

  function scheduleDemoLeadPopup() {
    if (shouldSuppressLeadPopup()) {
      return;
    }

    window.setTimeout(() => {
      if (shouldSuppressLeadPopup()) {
        return;
      }
      showDemoLeadPopup();
    }, 32000);
  }

  function shouldSuppressLeadPopup() {
    if (localStorage.getItem(POPUP_ENGAGED_KEY) === "true") {
      return true;
    }
    const dismissedAt = Number(localStorage.getItem(LEAD_POPUP_DISMISS_KEY) || "0");
    return dismissedAt > 0 && Date.now() - dismissedAt < 24 * 60 * 60 * 1000;
  }

  function shouldSuppressPopup() {
    if (localStorage.getItem(POPUP_ENGAGED_KEY) === "true") {
      return true;
    }

    const dismissedAt = Number(localStorage.getItem(POPUP_DISMISS_KEY) || "0");
    return dismissedAt > 0 && Date.now() - dismissedAt < 24 * 60 * 60 * 1000;
  }

  function showWhatsappPopup() {
    if (document.getElementById("marketingWhatsappPopup")) {
      document.getElementById("marketingWhatsappPopup").hidden = false;
      return;
    }

    const popup = document.createElement("aside");
    popup.className = "marketing-wa-popup";
    popup.id = "marketingWhatsappPopup";
    popup.innerHTML = `
      <button class="marketing-wa-popup__close" type="button" aria-label="Dismiss WhatsApp popup">×</button>
      <p>Need help choosing a course?</p>
      <h3>Chat with us on WhatsApp.</h3>
      <div class="marketing-wa-popup__actions">
        <a href="${SITE_INFO.whatsappUrl}" target="_blank" rel="noopener" data-source-page="${window.location.pathname || "/"}">Chat now</a>
        <button type="button" class="marketing-wa-popup__dismiss">Maybe later</button>
      </div>
    `;
    document.body.appendChild(popup);

    popup.querySelector(".marketing-wa-popup__close")?.addEventListener("click", dismissWhatsappPopup);
    popup.querySelector(".marketing-wa-popup__dismiss")?.addEventListener("click", dismissWhatsappPopup);
  }

  function hideWhatsappPopup() {
    const popup = document.getElementById("marketingWhatsappPopup");
    if (popup) {
      popup.hidden = true;
    }
  }

  function dismissWhatsappPopup() {
    localStorage.setItem(POPUP_DISMISS_KEY, String(Date.now()));
    hideWhatsappPopup();
  }

  function dismissDemoLeadPopup() {
    localStorage.setItem(LEAD_POPUP_DISMISS_KEY, String(Date.now()));
    const popup = document.getElementById("marketingDemoLeadPopup");
    if (popup) {
      popup.hidden = true;
    }
  }

  function showDemoLeadPopup() {
    if (document.getElementById("marketingDemoLeadPopup")) {
      document.getElementById("marketingDemoLeadPopup").hidden = false;
      return;
    }

    const popup = document.createElement("aside");
    popup.className = "marketing-demo-popup";
    popup.id = "marketingDemoLeadPopup";
    popup.innerHTML = `
      <button class="marketing-demo-popup__close" type="button" aria-label="Dismiss demo popup">×</button>
      <p class="marketing-demo-popup__eyebrow">Limited Demo Slots</p>
      <h3>Book a Demo + Fee Estimate</h3>
      <p class="marketing-demo-popup__text">Share details and get a quick callback from our admission advisor.</p>
      <form class="marketing-demo-popup__form" id="marketingDemoLeadForm">
        <input id="demoLeadName" type="text" maxlength="100" placeholder="Parent name" required>
        <input id="demoLeadClass" type="text" maxlength="30" placeholder="Grade (1-10)" required>
        <input id="demoLeadPhone" type="tel" maxlength="20" placeholder="Phone number" required>
        <button type="submit">Request Callback</button>
      </form>
      <p class="marketing-demo-popup__hint" id="marketingDemoLeadHint">Response usually within 15 minutes.</p>
    `;
    document.body.appendChild(popup);

    popup.querySelector(".marketing-demo-popup__close")?.addEventListener("click", dismissDemoLeadPopup);
    popup.querySelector("#marketingDemoLeadForm")?.addEventListener("submit", submitDemoLeadPopup);
  }

  async function submitDemoLeadPopup(event) {
    event.preventDefault();
    const name = document.getElementById("demoLeadName")?.value.trim();
    const studentClass = document.getElementById("demoLeadClass")?.value.trim();
    const parentPhone = document.getElementById("demoLeadPhone")?.value.trim();
    const hint = document.getElementById("marketingDemoLeadHint");

    if (!name || !studentClass || !parentPhone) {
      if (hint) {
        hint.textContent = "Please fill all fields.";
      }
      return;
    }

    try {
      await fetch(`${resolveApiBaseUrl()}/counseling`, {
        method: "POST",
        headers: buildApiHeaders(),
        credentials: "include",
        body: JSON.stringify({
          studentName: name,
          studentClass,
          board: "Not specified",
          parentPhone,
        }),
      });
      if (hint) {
        hint.textContent = "Thanks. Our advisor will contact you shortly.";
      }
      localStorage.setItem(POPUP_ENGAGED_KEY, "true");
      window.setTimeout(dismissDemoLeadPopup, 1200);
    } catch (_) {
      if (hint) {
        hint.textContent = "Unable to submit now. Please use WhatsApp.";
      }
    }
  }

  function enhanceLeadGenSections() {
    enhanceCoursesPage();
    enhanceDemoPage();
  }

  function injectGlobalConversionSection() {
    if (document.getElementById("marketingGlobalConversion")) {
      return;
    }

    const pagePath = (window.location.pathname || "").toLowerCase();
    if (
      [
        "/demo.html",
        "/login.html",
        "/register.html",
        "/admin-dashboard.html",
        "/student-dashboard.html",
      ].includes(pagePath)
    ) {
      return;
    }

    const host = document.querySelector("main") || document.body;
    const referenceSection =
      host.querySelector(".cta-section") ||
      host.querySelector(".section:last-of-type") ||
      null;

    const section = document.createElement("section");
    section.id = "marketingGlobalConversion";
    section.className = "marketing-conversion-strip";
    section.innerHTML = `
      <div class="container marketing-conversion-strip__inner">
        <div class="marketing-conversion-strip__copy">
          <p class="marketing-conversion-strip__eyebrow">Admissions Open</p>
          <h2>Get a personalized study plan for your child this week</h2>
          <p>Talk to our advisor, compare online and offline options, and book a demo class in under 2 minutes.</p>
        </div>
        <div class="marketing-conversion-strip__actions">
          <a href="/demo.html" class="marketing-conversion-strip__btn marketing-conversion-strip__btn--primary">Book Demo Class</a>
          <a href="${SITE_INFO.whatsappUrl}" target="_blank" rel="noopener" class="marketing-conversion-strip__btn marketing-conversion-strip__btn--whatsapp" data-source-page="${window.location.pathname || "/"}">WhatsApp Advisor</a>
          <a href="tel:${SITE_INFO.phoneHref}" class="marketing-conversion-strip__btn marketing-conversion-strip__btn--call">Call ${SITE_INFO.phoneDisplay}</a>
        </div>
      </div>
    `;

    if (referenceSection && referenceSection.parentNode) {
      referenceSection.parentNode.insertBefore(section, referenceSection);
      return;
    }

    host.appendChild(section);
  }

  function enhanceCoursesPage() {
    if (!window.location.pathname.endsWith("/courses.html") && window.location.pathname !== "/courses.html") {
      return;
    }

    if (document.getElementById("coursesLeadProof")) {
      return;
    }

    const ctaSection = document.querySelector(".cta-section");
    if (!ctaSection) {
      return;
    }

    const proof = document.createElement("section");
    proof.className = "marketing-proof-section section";
    proof.id = "coursesLeadProof";
    proof.innerHTML = `
      <div class="container">
        <div class="section-header">
          <p class="eyebrow">Results That Build Trust</p>
          <h2 class="section-title">Families choose BrightNest Academy for steady progress</h2>
          <p class="section-subtitle">Board-aligned teaching, responsive communication, and measurable academic improvement across Bengaluru.</p>
        </div>
        <div class="marketing-proof-grid">
          <article>
            <strong>500+</strong>
            <span>Students trained across Bengaluru and online</span>
          </article>
          <article>
            <strong>4.9/5</strong>
            <span>Average parent satisfaction from repeated referrals</span>
          </article>
          <article>
            <strong>98%</strong>
            <span>Families who continue after structured demo feedback</span>
          </article>
        </div>
      </div>
    `;

    ctaSection.before(proof);
  }

  function enhanceDemoPage() {
    if (!window.location.pathname.endsWith("/demo.html") && window.location.pathname !== "/demo.html") {
      return;
    }

    if (document.getElementById("demoLeadTrust")) {
      return;
    }

    const demoSection = document.querySelector(".demo-section .container");
    if (!demoSection) {
      return;
    }

    const trust = document.createElement("div");
    trust.className = "marketing-demo-trust";
    trust.id = "demoLeadTrust";
    trust.innerHTML = `
      <div class="marketing-demo-trust__card">
        <h3>What parents get after booking a demo</h3>
        <ul>
          <li>Callback within 24 hours from the BrightNest Academy team</li>
          <li>Board and grade fit assessment before scheduling</li>
          <li>Clear fee guidance after the student's learning need is understood</li>
        </ul>
        <a href="${SITE_INFO.whatsappUrl}" target="_blank" rel="noopener" data-source-page="${window.location.pathname || "/"}">Need help before booking? WhatsApp Us</a>
      </div>
    `;

    demoSection.prepend(trust);
  }

  function resolveApiBaseUrl() {
    if (typeof window.API_BASE_URL === "string" && window.API_BASE_URL.trim()) {
      return window.API_BASE_URL.trim().replace(/\/$/, "");
    }
    if (typeof window.__API_BASE_URL__ === "string" && window.__API_BASE_URL__.trim()) {
      return window.__API_BASE_URL__.trim().replace(/\/$/, "");
    }
    if (window.location && window.location.protocol === "file:") {
      return "http://localhost:8080/api";
    }
    return "/api";
  }

  function buildApiHeaders() {
    return {
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-Tenant-ID": localStorage.getItem("tenantKey") || "default",
    };
  }

  function wait(ms) {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
  }

  function escapeHtmlAttribute(value) {
    return String(value).replace(/"/g, "&quot;");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init, { once: true });
  } else {
    init();
  }
})();
