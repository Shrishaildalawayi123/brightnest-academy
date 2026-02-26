/**
 * BrightNest Academy - Shared Data
 * Team members, FAQ, testimonials, course info used across pages
 */

const TEAM_DATA = [
  {
    id: "founder",
    name: "Bharati Satappagol",
    role: "Founder & Lead Educator",
    subjects: ["Kannada", "English", "Science", "Maths"],
    bio: "With 25 years of experience across thousands of diverse teaching scenarios, consistently delivering uncompromised quality and impactful learning.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#6366f1",
  },
  {
    id: "aayusmita",
    name: "Miss Aayusmita Dey",
    role: "Educator",
    subjects: ["Sanskrit", "Maths", "Science"],
    bio: "A young and enthusiastic teacher who builds strong connections with students, inspiring rapid and meaningful progress through trust and motivation.",
    avatar: "ðŸ‘©â€ðŸ”¬",
    color: "#ec4899",
  },
  {
    id: "arundathi",
    name: "Mrs. Arundhati V M",
    role: "Educator",
    subjects: ["Kannada", "English"],
    bio: "Beloved by students for her clear, simplified teaching style that makes even the toughest topics easy to grasp and enjoy.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#14b8a6",
  },
  {
    id: "kamalakshi",
    name: "Mrs. Kamalakshi B K",
    role: "Educator",
    subjects: ["Kannada", "Hindi"],
    bio: "A methodical and supportive teacher who expertly guides students from the basics to confident mastery â€” often in just a few weeks.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#f59e0b",
  },
  {
    id: "madhvi",
    name: "Miss Madhvi Chawla",
    role: "Educator",
    subjects: ["English"],
    bio: "Expert English educator blending academic rigor with an engaging, student-loved teaching style that makes learning a joy.",
    avatar: "ðŸ‘©â€ðŸ’¼",
    color: "#8b5cf6",
  },
  {
    id: "meghna",
    name: "Mrs. Meghna Roy",
    role: "Educator",
    subjects: ["English", "Maths", "Science", "Hindi"],
    bio: "A highly effective teacher who turns everyday lessons into confident learning and joyful discovery, inspiring students to excel with ease.",
    avatar: "ðŸ‘©â€ðŸ”¬",
    color: "#06b6d4",
  },
  {
    id: "priya",
    name: "Mrs. Priya K",
    role: "Educator",
    subjects: ["German"],
    bio: "Experienced German teacher who brings language to life with engaging lessons, cultural flair, and a deep commitment to student success.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#f59e0b",
  },
  {
    id: "sushma",
    name: "Dr. Sushma Narayan",
    role: "Senior Educator",
    subjects: ["Sanskrit", "Hindi"],
    bio: "Empowering students for over 20 years with expert Hindi instruction that is both professional and warmly student-centered.",
    avatar: "ðŸ‘©â€ðŸŽ“",
    color: "#10b981",
  },
  {
    id: "vimala",
    name: "Mrs. Vimala Jayaram",
    role: "Senior Educator",
    subjects: ["Sanskrit", "Hindi"],
    bio: "Versatile Sanskrit expert with deep knowledge and a gift for connecting with learners of all ages through clear, compassionate teaching.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#3b82f6",
  },
  {
    id: "bharati",
    name: "Ms. Bharati R S",
    role: "Educator",
    subjects: ["Kannada", "English", "Maths", "Science"],
    bio: "A dedicated educator providing personalized, interactive instruction to help students achieve fluency and reach their learning goals.",
    avatar: "ðŸ‘©â€ðŸ«",
    color: "#d946ef",
  },
  {
    id: "saraswati",
    name: "Mrs. Saraswati Hegde",
    role: "Senior Educator",
    subjects: ["Sanskrit"],
    bio: "A Sanskrit educator with 48 years of experience. Her friendly teaching approach creates a stress-free environment that fosters appreciation for Sanskrit's cultural heritage.",
    avatar: "ðŸ‘©â€ðŸŽ“",
    color: "#0ea5e9",
  },
];

const FAQ_DATA = {
  general: [
    {
      q: "What courses are offered at BrightNest Academy?",
      a: "We offer Kannada, English, Hindi, Sanskrit, French, Maths, Science, and German. Our expert tutors provide personalized tuition to help students excel.",
    },
    {
      q: "What are the modes of class delivery?",
      a: "We offer both Online and Offline classes. Online classes are conducted via Google Meet. Offline classes take place at our center in Kumaraswamy Layout, Bengaluru.",
    },
    {
      q: "How can I enroll in a course?",
      a: "Book a demo class through our website, or call us at +91-7204193980. After the demo, our team will guide you through enrollment.",
    },
    {
      q: "Where do offline classes take place?",
      a: "#662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bengaluru â€“ 560078.",
    },
    {
      q: "Where can I learn about the teachers?",
      a: "Visit our Team page to learn about each educator's qualifications, experience, and specializations.",
    },
    {
      q: "On which platform are online classes conducted?",
      a: "All online classes use Google Meet. You receive a link before each class. Just need a device with internet and a browser.",
    },
  ],
  courseSpecific: [
    {
      q: "What curriculum boards do you support?",
      a: "We support ICSE, CBSE, and Karnataka State Board curricula for grades I to XII.",
    },
    {
      q: "Do you offer spoken language courses?",
      a: "Yes! We offer Spoken Hindi, Spoken English, Spoken Kannada, Spoken French, and Spoken German courses for all ages.",
    },
    {
      q: "Can I take multiple courses simultaneously?",
      a: "Absolutely! We create flexible schedules to accommodate multiple courses without overlap.",
    },
    {
      q: "What does the Reading & Writing course cover?",
      a: "Comprehension, creative writing, grammar, and vocabulary building through structured, interactive lessons.",
    },
    {
      q: "Do you prepare students for competitive exams?",
      a: "Yes, we offer preparation for various language proficiency tests and competitive exams.",
    },
  ],
  feePayment: [
    {
      q: "What is the demo class fee?",
      a: "The demo class fee is â‚¹100. It gets adjusted in first month fees or refunded within 30 days if you don't enroll.",
    },
    {
      q: "What payment methods are accepted?",
      a: "Bank transfers, UPI (Google Pay, PhonePe, Paytm), and online payment through our portal.",
    },
    {
      q: "Is there a refund policy?",
      a: "Yes, please see our Fee Payment & Cancellation policy page for complete details on refunds.",
    },
    {
      q: "How is the fee structure determined?",
      a: "Fees vary by course, delivery mode, and sessions per week. Contact us for a personalized quote.",
    },
  ],
};

const TESTIMONIALS = [
  {
    text: "My daughter scored 100% in Sanskrit and 99.9% in Hindi in her CBSE 10th board exams. The teachers at BrightNest Academy are truly exceptional.",
    author: "Sandeep Patil",
    course: "Sanskrit & Hindi",
    rating: 5,
  },
  {
    text: "The personalized attention my son receives in his Kannada classes has transformed his confidence. He now loves the language!",
    author: "Priya Menon",
    course: "Kannada",
    rating: 5,
  },
  {
    text: "My daughter's Maths score improved from 65% to 95% in just 3 months. The teachers explain every concept step-by-step with great patience.",
    author: "Rahul Sharma",
    course: "Maths",
    rating: 5,
  },
  {
    text: "BrightNest Academy helped my child go from struggling with English to topping the class. The teaching method is outstanding!",
    author: "Meera Iyer",
    course: "English",
    rating: 5,
  },
  {
    text: "The online classes are as effective as offline ones. My daughter loves the interactive sessions and has improved remarkably.",
    author: "Arvind Kumar",
    course: "Hindi",
    rating: 5,
  },
];

const COURSE_INFO = {
  sanskrit: {
    name: "Sanskrit",
    tagline: "Discover the Beauty of the Ancient Language",
    description:
      "Our Sanskrit course is designed to nurture a deep understanding and appreciation for one of the world's oldest languages. Whether your child is beginning their Sanskrit journey or preparing for board exams, we provide personalized guidance every step of the way.",
    color: "#6366f1",
    icon: "ðŸ•‰ï¸",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Comprehensive tuition aligned with CBSE, ICSE, and Karnataka State Board for grades Iâ€“XII. Our classes focus on grammar, prose, poetry, and exam preparation.",
        icon: "ðŸ“š",
      },
      {
        title: "Sloka & Spoken Sanskrit",
        desc: "Learn to chant popular slokas with correct pronunciation and explore conversational Sanskrit through engaging, interactive sessions.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "Master Devanagari script, build vocabulary, and develop comprehension skills through structured reading and creative writing exercises.",
        icon: "âœï¸",
      },
    ],
  },
  hindi: {
    name: "Hindi",
    tagline: "Master India's National Language with Confidence",
    description:
      "Our Hindi course helps students build fluency, reading skills, and academic excellence. From school curriculum support to spoken Hindi for daily life, we cover it all with a warm, engaging approach.",
    color: "#f59e0b",
    icon: "ðŸ“–",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and State Board syllabi for grades Iâ€“XII. We cover grammar, literature, comprehension, and creative writing with exam-focused preparation.",
        icon: "ðŸ“š",
      },
      {
        title: "Spoken Hindi",
        desc: "Conversational Hindi classes for students and adults who want to speak fluently. Ideal for non-native speakers or those looking to improve communication skills.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "Develop reading comprehension, essay writing, and Hindi script mastery through progressive, interactive exercises.",
        icon: "âœï¸",
      },
    ],
  },
  english: {
    name: "English",
    tagline: "Build Strong Communication & Academic Skills",
    description:
      "Our English course empowers students to read, write, and communicate with confidence. From grammar fundamentals to creative expression, we build a solid English foundation for academic and real-world success.",
    color: "#3b82f6",
    icon: "ðŸ“",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete support for CBSE, ICSE, and State Board English â€” literature analysis, grammar, writing skills, and comprehensive exam preparation.",
        icon: "ðŸ“š",
      },
      {
        title: "Spoken English",
        desc: "Interactive speaking practice focusing on pronunciation, vocabulary, fluency, and confidence building for students and adults alike.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "Comprehension, creative writing, essay composition, and literary appreciation through structured and engaging sessions.",
        icon: "âœï¸",
      },
    ],
  },
  kannada: {
    name: "Kannada",
    tagline: "Connect with Karnataka's Rich Language & Culture",
    description:
      "Learn Kannada from expert native-speaking educators. Whether for school exams or daily communication, our Kannada course builds reading, writing, and speaking skills in a supportive environment.",
    color: "#10b981",
    icon: "ðŸ›ï¸",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and Karnataka State Board syllabi. Grammar, prose, poetry, and exam-focused preparation for grades Iâ€“XII.",
        icon: "ðŸ“š",
      },
      {
        title: "Spoken Kannada",
        desc: "Conversational Kannada for non-native speakers, new residents of Karnataka, and students wanting to improve everyday communication.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "Kannada script mastery, reading comprehension, story writing, and vocabulary building through interactive lessons.",
        icon: "âœï¸",
      },
    ],
  },
  french: {
    name: "French",
    tagline: "Explore the Language of Art, Culture & Diplomacy",
    description:
      "Our French course introduces students to one of the world's most beautiful languages. From classroom learning to real-world conversation, we make French accessible and enjoyable.",
    color: "#ef4444",
    icon: "ðŸ‡«ðŸ‡·",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete support for CBSE and ICSE French syllabi. Grammar, comprehension, writing, and exam preparation tailored to each student's level.",
        icon: "ðŸ“š",
      },
      {
        title: "Spoken French",
        desc: "Learn to speak French confidently with our interactive conversation classes. Perfect for beginners and intermediate learners alike.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "French reading comprehension, composition, and writing exercises designed to build fluency and appreciation for French literature.",
        icon: "âœï¸",
      },
    ],
  },
  maths: {
    name: "Maths",
    tagline: "Build Strong Problem-Solving & Analytical Skills",
    description:
      "Our Mathematics course helps students develop a deep understanding of concepts, strong calculation skills, and confident problem-solving ability. From basic arithmetic to advanced calculus, we make Maths approachable and enjoyable.",
    color: "#6366f1",
    icon: "ðŸ“",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete coverage of CBSE, ICSE, and Karnataka State Board Maths for grades Iâ€“XII. Algebra, geometry, trigonometry, calculus, statistics and exam-focused practice.",
        icon: "ðŸ“š",
      },
      {
        title: "Concept Building",
        desc: "Step-by-step conceptual learning that builds a solid foundation. We break down complex topics into easy-to-understand segments for lasting understanding.",
        icon: "ðŸ§®",
      },
      {
        title: "Problem Practice & Exams",
        desc: "Extensive practice with solved examples, worksheets, past papers, and timed tests to build speed, accuracy, and exam confidence.",
        icon: "âœï¸",
      },
    ],
  },
  science: {
    name: "Science",
    tagline: "Explore the World Through Curiosity & Discovery",
    description:
      "Our Science course nurtures curiosity, critical thinking, and a love for discovery. Covering Physics, Chemistry, and Biology, we help students understand the natural world and excel in their board exams.",
    color: "#10b981",
    icon: "ðŸ”¬",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and Karnataka State Board Science syllabi for grades Iâ€“X, and Physics/Chemistry/Biology for grades XIâ€“XII.",
        icon: "ðŸ“š",
      },
      {
        title: "Practical & Lab Concepts",
        desc: "Clear explanations of experiments, diagrams, and practical concepts that help students excel in both theory and practical examinations.",
        icon: "âš—ï¸",
      },
      {
        title: "Competitive Exam Prep",
        desc: "Targeted preparation for competitive exams including NEET, JEE foundation, and Olympiads with additional concept enrichment.",
        icon: "ðŸ†",
      },
    ],
  },
  german: {
    name: "German",
    tagline: "Open Doors to Europe's Most Spoken Language",
    description:
      "Our German course builds language skills from the ground up â€” from alphabet and basic grammar to fluent conversation and advanced writing. German opens doors to global education, career, and cultural experiences.",
    color: "#f59e0b",
    icon: "ðŸ‡©ðŸ‡ª",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete support for CBSE and ICSE German syllabi. Grammar, vocabulary, reading comprehension, writing, and board exam preparation.",
        icon: "ðŸ“š",
      },
      {
        title: "Spoken German",
        desc: "Interactive conversation classes to develop confident everyday German. Ideal for beginners, travelers, and students preparing for study abroad.",
        icon: "ðŸ—£ï¸",
      },
      {
        title: "Reading & Writing",
        desc: "German script mastery, reading comprehension, essay writing, and vocabulary enrichment through structured and progressive exercises.",
        icon: "âœï¸",
      },
    ],
  },
};

const COURSE_EDUCATORS = {
  sanskrit: ["founder", "aayusmita", "sushma", "vimala", "saraswati"],
  hindi: ["founder", "kamalakshi", "sushma", "vimala", "meghna"],
  english: ["meghna", "madhvi", "arundathi", "bharati"],
  kannada: ["arundathi", "kamalakshi", "bharati"],
  french: ["priya"],
  maths: ["aayusmita", "meghna", "bharati", "founder"],
  science: ["aayusmita", "meghna", "bharati", "founder"],
  german: ["priya"],
};

/* Helper Functions */
function getEducatorsForSubject(subject) {
  const ids = COURSE_EDUCATORS[subject.toLowerCase()] || [];
  return TEAM_DATA.filter((t) => ids.includes(t.id));
}

function renderTeamCard(member) {
  return `
        <div class="team-card" data-aos="fade-up">
            <div class="team-avatar" style="background:${member.color}">
                <span>${member.avatar}</span>
            </div>
            <h3 class="team-name">${member.name}</h3>
            <p class="team-role">${member.role}</p>
            <div class="team-subjects">
                ${member.subjects.map((s) => `<span class="subject-tag">${s}</span>`).join("")}
            </div>
            <p class="team-bio">${member.bio}</p>
        </div>`;
}

function renderFaqAccordion(items) {
  return items
    .map(
      (item, i) => `
        <div class="accordion-item">
            <button class="accordion-header" onclick="toggleAccordion(this)" aria-expanded="false">
                <span>${item.q}</span>
                <svg class="accordion-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div class="accordion-body"><p>${item.a}</p></div>
        </div>`,
    )
    .join("");
}

function toggleAccordion(btn) {
  const item = btn.parentElement;
  const wasOpen = item.classList.contains("open");
  // Close all siblings in the same container
  item.parentElement
    .querySelectorAll(".accordion-item.open")
    .forEach((el) => el.classList.remove("open"));
  if (!wasOpen) {
    item.classList.add("open");
    btn.setAttribute("aria-expanded", "true");
  }
}

function renderTestimonialSlider(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;
  let idx = 0;
  function render() {
    const t = TESTIMONIALS[idx];
    container.innerHTML = `
            <div class="testimonial-card">
                <div class="testimonial-stars">${"â˜…".repeat(t.rating)}</div>
                <p class="testimonial-text">"${t.text}"</p>
                <div class="testimonial-author">
                    <strong>${t.author}</strong>
                    <span>${t.course}</span>
                </div>
            </div>
            <div class="testimonial-dots">
                ${TESTIMONIALS.map((_, i) => `<button class="dot ${i === idx ? "active" : ""}" onclick="window.__setTestimonial(${i})"></button>`).join("")}
            </div>`;
  }
  window.__setTestimonial = function (i) {
    idx = i;
    render();
  };
  render();
  setInterval(() => {
    idx = (idx + 1) % TESTIMONIALS.length;
    render();
  }, 5000);
}

/* Navigation Template */
function getNavHTML(activePage) {
  const isLoggedIn = localStorage.getItem("user");
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const isAdmin = user.role === "ADMIN";

  let authLinks;
  if (isLoggedIn) {
    const dashLink = isAdmin
      ? "admin-dashboard.html"
      : "student-dashboard.html";
    authLinks = `
            <li><a href="${dashLink}" class="nav-link">Dashboard</a></li>
            <li><a href="#" class="nav-link" onclick="if(window.Auth){Auth.logout();}else{localStorage.removeItem('user');window.location.href='index.html';}">Logout</a></li>`;
  } else {
    authLinks = `<li><a href="login.html" class="nav-link">Login</a></li>`;
  }

  return `
    <header class="header" id="header">
        <nav class="nav container">
            <a href="index.html" class="logo">
                <span class="logo-icon">ðŸŽ“</span>
                <span>BrightNest Academy</span>
            </a>
            <ul class="nav-links" id="navLinks">
                <li><a href="index.html" class="nav-link ${activePage === "home" ? "active" : ""}">Home</a></li>
                <li class="nav-dropdown">
                    <a href="courses.html" class="nav-link ${activePage === "courses" ? "active" : ""}">Courses <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-left:4px;vertical-align:middle"><polyline points="6 9 12 15 18 9"/></svg></a>
                    <ul class="dropdown-menu">
                        <li><a href="courses.html">All Courses</a></li>
                        <li><a href="kannada.html">Kannada</a></li>
                        <li><a href="english.html">English</a></li>
                        <li><a href="hindi.html">Hindi</a></li>
                        <li><a href="sanskrit.html">Sanskrit</a></li>
                        <li><a href="french.html">French</a></li>
                        <li><a href="maths.html">Maths</a></li>
                        <li><a href="science.html">Science</a></li>
                        <li><a href="german.html">German</a></li>
                    </ul>
                </li>
                <li><a href="about.html" class="nav-link ${(activePage === "about" || activePage === "team") ? "active" : ""}">About</a></li>
                <li><a href="faq.html" class="nav-link ${activePage === "faq" ? "active" : ""}">FAQ</a></li>
                <li><a href="contact.html" class="nav-link ${activePage === "contact" ? "active" : ""}">Contact</a></li>
                ${authLinks}
                <li><a href="demo.html" class="btn btn-accent btn-sm">Book a Demo</a></li>
            </ul>
            <button class="menu-toggle" id="menuToggle" aria-label="Toggle menu">
                <span></span><span></span><span></span>
            </button>
        </nav>
    </header>`;
}

function getFooterHTML() {
  return `
    <footer class="footer">
        <div class="container">
            <div class="footer-grid">
                <div class="footer-col">
                    <div class="footer-logo">
                        <span class="logo-icon">ðŸŽ“</span>
                        <span>BrightNest Academy</span>
                    </div>
                    <p class="footer-desc">Building confidence in learning through personalized education in Languages, Maths & Science.</p>
                    <div class="footer-social">
                        <a href="https://www.facebook.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Facebook">ðŸ“˜</a>
                        <a href="https://www.instagram.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Instagram">ðŸ“¸</a>
                        <a href="https://www.youtube.com/@brightnestacademy" target="_blank" rel="noopener" aria-label="YouTube">â–¶ï¸</a>
                        <a href="https://wa.me/917204193980?text=Hi%20BrightNest%20Academy" target="_blank" rel="noopener" aria-label="WhatsApp">ðŸ’¬</a>
                    </div>
                </div>
                <div class="footer-col">
                    <h4>Subjects</h4>
                    <ul>
                        <li><a href="kannada.html">Kannada</a></li>
                        <li><a href="english.html">English</a></li>
                        <li><a href="hindi.html">Hindi</a></li>
                        <li><a href="sanskrit.html">Sanskrit</a></li>
                        <li><a href="french.html">French</a></li>
                        <li><a href="maths.html">Maths</a></li>
                        <li><a href="science.html">Science</a></li>
                        <li><a href="german.html">German</a></li>
                    </ul>
                </div>
                <div class="footer-col">
                    <h4>Quick Links</h4>
                    <ul>
                        <li><a href="about.html">About</a></li>
                        <li><a href="faq.html">FAQ</a></li>
                        <li><a href="blog.html">Blog</a></li>
                        <li><a href="demo.html">Book a Demo</a></li>
                        <li><a href="contact.html">Contact Us</a></li>
                        <li><a href="fee-payment.html">Pay Fees</a></li>
                    </ul>
                </div>
                <div class="footer-col">
                    <h4>Legal</h4>
                    <ul>
                        <li><a href="privacy-policy.html">Privacy Policy</a></li>
                        <li><a href="terms-conditions.html">Terms & Conditions</a></li>
                        <li><a href="course-delivery.html">Course Delivery</a></li>
                        <li><a href="pricing-cancellation.html">Pricing & Cancellation</a></li>
                        <li><a href="fee-payment.html">Fee & Payment</a></li>
                    </ul>
                </div>
            </div>
            <div class="footer-contact">
                <span>ðŸ“ž +91-7204193980</span>
                <span>âœ‰ï¸ info@brightnest-academy.com</span>
                <span>ðŸ“ Kumaraswamy Layout, Bengaluru â€“ 560078</span>
            </div>
            <div class="footer-bottom">
                <p>&copy; ${new Date().getFullYear()} BrightNest Academy. All rights reserved.</p>
            </div>
        </div>
    </footer>
    <!-- WhatsApp Floating Button -->
    <a href="https://wa.me/917204193980?text=Hi%20BrightNest%20Academy%2C%20I%20have%20a%20query" target="_blank" rel="noopener"
       style="position:fixed;bottom:24px;right:24px;z-index:9999;width:60px;height:60px;border-radius:50%;background:#25D366;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 16px rgba(0,0,0,0.2);transition:transform .2s;text-decoration:none;"
       aria-label="Chat on WhatsApp"
       onmouseover="this.style.transform='scale(1.1)'" onmouseout="this.style.transform='scale(1)'">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="white"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.019-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
    </a>`;
}

/* Exports */
window.TEAM_DATA = TEAM_DATA;
window.FAQ_DATA = FAQ_DATA;
window.TESTIMONIALS = TESTIMONIALS;
window.COURSE_INFO = COURSE_INFO;
window.COURSE_EDUCATORS = COURSE_EDUCATORS;
window.getEducatorsForSubject = getEducatorsForSubject;
window.renderTeamCard = renderTeamCard;
window.renderFaqAccordion = renderFaqAccordion;
window.toggleAccordion = toggleAccordion;
window.renderTestimonialSlider = renderTestimonialSlider;
window.getNavHTML = getNavHTML;
window.getFooterHTML = getFooterHTML;

