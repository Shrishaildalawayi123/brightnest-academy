(function () {
  const SUBJECT_LINKS = [
    { href: "kannada.html", label: "Kannada" },
    { href: "english.html", label: "English" },
    { href: "hindi.html", label: "Hindi" },
    { href: "sanskrit.html", label: "Sanskrit" },
    { href: "maths.html", label: "Mathematics" },
    { href: "science.html", label: "Science" },
    { href: "german.html", label: "German" },
  ];

  function getCurrentPageName() {
    const path = window.location.pathname || "";
    const page = path.split("/").pop();
    return (page || "index.html").toLowerCase();
  }

  function isCoursesActive(currentPage) {
    if (currentPage === "courses.html") {
      return true;
    }

    return SUBJECT_LINKS.some((subject) => subject.href === currentPage);
  }

  function renderNav(activePage) {
    const header = document.getElementById("header");
    if (!header) {
      return;
    }

    const currentPage = (activePage || getCurrentPageName()).toLowerCase();
    const coursesActive = isCoursesActive(currentPage) ? " active" : "";

    header.innerHTML = `
      <nav class="nav container">
        <a href="index.html" class="logo">
          <img src="images/logo.png" alt="BrightNest Academy" class="logo-icon" width="80" height="80" decoding="async">
          <span>BrightNest Academy</span>
        </a>
        <ul class="nav-links" id="navLinks">
          <li><a href="index.html" class="nav-link">Home</a></li>
          <li class="nav-dropdown">
            <a href="courses.html" class="nav-link${coursesActive}">Courses <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-left:4px;vertical-align:middle"><polyline points="6 9 12 15 18 9"/></svg></a>
            <ul class="dropdown-menu">
              ${SUBJECT_LINKS.map((subject) => `<li><a href="${subject.href}">${subject.label}</a></li>`).join("")}
            </ul>
          </li>
          <li><a href="about.html" class="nav-link">About</a></li>
          <li><a href="faq.html" class="nav-link">FAQ</a></li>
          <li><a href="contact.html" class="nav-link">Contact</a></li>
          <li><a href="careers.html" class="nav-link">Careers</a></li>
          <li><a href="login.html" class="nav-link">Login</a></li>
          <li><a href="demo.html" class="btn btn-accent btn-sm">Book a Demo</a></li>
        </ul>
        <button class="menu-toggle" id="menuToggle" aria-label="Toggle menu" aria-expanded="false"><span></span><span></span><span></span></button>
      </nav>
    `;

    const navLinks = header.querySelectorAll(".nav-link");
    navLinks.forEach((link) => {
      const href = (link.getAttribute("href") || "").toLowerCase();
      if (href === currentPage || (href === "index.html" && currentPage === "")) {
        link.classList.add("active");
        link.setAttribute("aria-current", "page");
      }
    });
  }

  function renderFooter() {
    const footerPlaceholder = document.getElementById("footerPlaceholder");
    if (!footerPlaceholder) {
      return;
    }

    footerPlaceholder.innerHTML = `
      <footer class="footer">
        <div class="container">
          <div class="footer-content">
            <div class="footer-column">
              <h3>BrightNest Academy</h3>
              <p>Personalized tuition for Grades 1-10 with experienced educators in Bengaluru.</p>
              <p class="footer-meta">Mon-Sat, 9:00 AM to 6:00 PM</p>
            </div>
            <div class="footer-column">
              <h4>Quick Links</h4>
              <a href="about.html">About</a>
              <a href="courses.html">Courses</a>
              <a href="team.html">Educators</a>
              <a href="contact.html">Contact</a>
            </div>
            <div class="footer-column">
              <h4>Contact</h4>
              <a href="tel:+917204193980">+91-7204193980</a>
              <a href="mailto:info@brightnest-academy.com">info@brightnest-academy.com</a>
              <a href="https://wa.me/917204193980" target="_blank" rel="noopener">WhatsApp</a>
            </div>
          </div>
          <p class="footer-bottom">&copy; <span id="currentYear"></span> BrightNest Academy. All rights reserved.</p>
        </div>
      </footer>
    `;
  }

  window.renderNav = renderNav;
  window.renderFooter = renderFooter;
})();
