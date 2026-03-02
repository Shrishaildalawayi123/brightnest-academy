/**
 * BrightNest Academy - Shared Data
 * Team members, FAQ, testimonials, course info used across pages
 */

const TEAM_DATA = [
  {
    id: "founder",
    name: "Bharati R Satappagol",
    role: "Founder & Lead Educator",
    subjects: ["Kannada", "English", "Science", "Mathematics"],
    bio: "With 25 years of experience across thousands of diverse teaching scenarios, consistently delivering uncompromised quality and impactful learning.",
    avatar: "👩‍🏫",
    photo: "images/bharati.jpg",
    color: "#6366f1",
  },
  {
    id: "aayusmita",
    name: "Miss Aayusmita Dey",
    role: "Educator",
    subjects: ["Sanskrit", "Mathematics", "Science"],
    bio: "A young and enthusiastic teacher who builds strong connections with students, inspiring rapid and meaningful progress through trust and motivation.",
    avatar: "👩‍🔬",
    color: "#ec4899",
  },
  {
    id: "arundathi",
    name: "Mrs. Arundhati V M",
    role: "Educator",
    subjects: ["Kannada", "English"],
    bio: "Beloved by students for her clear, simplified teaching style that makes even the toughest topics easy to grasp and enjoy.",
    avatar: "👩‍🏫",
    color: "#14b8a6",
  },
  {
    id: "kamalakshi",
    name: "Mrs. Kamalakshi B K",
    role: "Educator",
    subjects: ["Kannada", "Hindi"],
    bio: "A methodical and supportive teacher who expertly guides students from the basics to confident mastery — often in just a few weeks.",
    avatar: "👩‍🏫",
    color: "#f59e0b",
  },
  {
    id: "madhvi",
    name: "Miss Madhvi Chawla",
    role: "Educator",
    subjects: ["English"],
    bio: "Expert English educator blending academic rigor with an engaging, student-loved teaching style that makes learning a joy.",
    avatar: "👩‍💼",
    color: "#8b5cf6",
  },
  {
    id: "meghna",
    name: "Mrs. Meghna Roy",
    role: "Educator",
    subjects: ["English", "Mathematics", "Science", "Hindi"],
    bio: "A highly effective teacher who turns everyday lessons into confident learning and joyful discovery, inspiring students to excel with ease.",
    avatar: "👩‍🔬",
    color: "#06b6d4",
  },
  {
    id: "sushma",
    name: "Dr. Sushma Narayan",
    role: "Senior Educator",
    subjects: ["Sanskrit", "Hindi"],
    bio: "Empowering students for over 20 years with expert Hindi instruction that is both professional and warmly student-centered.",
    avatar: "👩‍🎓",
    color: "#10b981",
  },
  {
    id: "vimala",
    name: "Mrs. Vimala Jayaram",
    role: "Senior Educator",
    subjects: ["Sanskrit", "Hindi"],
    bio: "Versatile Sanskrit expert with deep knowledge and a gift for connecting with learners of all ages through clear, compassionate teaching.",
    avatar: "👩‍🏫",
    color: "#3b82f6",
  },
  {
    id: "bharati",
    name: "Ms. Bharati R S",
    role: "Educator",
    subjects: ["Kannada", "English", "Mathematics", "Science"],
    bio: "A dedicated educator providing personalized, interactive instruction to help students achieve fluency and reach their learning goals.",
    avatar: "👩‍🏫",
    color: "#d946ef",
  },
  {
    id: "saraswati",
    name: "Mrs. Saraswati Hegde",
    role: "Senior Educator",
    subjects: ["Sanskrit"],
    bio: "A Sanskrit educator with 48 years of experience. Her friendly teaching approach creates a stress-free environment that fosters appreciation for Sanskrit's cultural heritage.",
    avatar: "👩‍🎓",
    color: "#0ea5e9",
  },
  {
    id: "shrishail",
    name: "Mr. Shrishail Dalawayi",
    role: "German Language Educator",
    subjects: ["German"],
    bio: "A passionate German language educator dedicated to making the language accessible and enjoyable for school students through interactive and structured teaching methods.",
    avatar: "👨‍🏫",
    photo: "images/shrishail.jpg",
    color: "#ef4444",
  },
];

const FAQ_DATA = {
  general: [
    {
      q: "What courses are offered at BrightNest Academy?",
      a: "We offer Kannada, English, Hindi, Sanskrit, German, Mathematics, and Science. Our expert tutors provide personalized tuition to help students excel.",
    },
    {
      q: "What are the modes of class delivery?",
      a: "We offer both Online and Offline classes. Online classes are conducted via Google Meet. Offline classes take place at our center in Kumaraswamy Layout, Bengaluru.",
    },
    {
      q: "How can I enroll in a course?",
      a: "Book a demo class through our website, or call us at +91-6363464005. After the demo, our team will guide you through enrollment.",
    },
    {
      q: "Where do offline classes take place?",
      a: "#662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bengaluru – 560078.",
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
      a: "We support ICSE, CBSE, and Karnataka State Board curricula for grades 1 to 10.",
    },
    {
      q: "Do you offer spoken language courses?",
      a: "Yes! We offer Spoken Hindi, Spoken English, Spoken Kannada, and Spoken German courses for school students.",
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
      q: "Do you prepare students for school-level competitive exams?",
      a: "Yes, we offer preparation for school Olympiads, NTSE, and other school-level tests.",
    },
  ],
  feePayment: [
    {
      q: "What is the demo class fee?",
      a: "The demo class fee is ₹100. It gets adjusted in first month fees or refunded within 30 days if you don't enroll.",
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
    text: "My daughter's Mathematics score improved from 65% to 95% in just 3 months. The teachers explain every concept step-by-step with great patience.",
    author: "Rahul Sharma",
    course: "Mathematics",
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
    icon: "🕉️",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Read and write fluently in Devanagari script",
      "Strong command of Sanskrit grammar (Vyakarana)",
      "Understand and appreciate Sanskrit prose & poetry",
      "Chant popular slokas with correct pronunciation",
      "Confident exam performance in board exams",
      "Build vocabulary that strengthens Hindi and other languages",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Alphabet, basic words, simple sentences, slokas, rhymes in Sanskrit",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Grammar fundamentals, sandhi, vibhakti, prose & poetry comprehension, essay writing",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced grammar, literature analysis, translation, composition, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 studying Sanskrit under CBSE, ICSE, or Karnataka State Board. Also ideal for students who want to learn slokas, spoken Sanskrit, or build a strong language foundation.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Comprehensive tuition aligned with CBSE, ICSE, and Karnataka State Board for grades 1–10. Our classes focus on grammar, prose, poetry, and exam preparation.",
        icon: "📚",
      },
      {
        title: "Sloka & Spoken Sanskrit",
        desc: "Learn to chant popular slokas with correct pronunciation and explore conversational Sanskrit through engaging, interactive sessions.",
        icon: "🗣️",
      },
      {
        title: "Reading & Writing",
        desc: "Master Devanagari script, build vocabulary, and develop comprehension skills through structured reading and creative writing exercises.",
        icon: "✍️",
      },
    ],
  },
  hindi: {
    name: "Hindi",
    tagline: "Master India's National Language with Confidence",
    description:
      "Our Hindi course helps students build fluency, reading skills, and academic excellence. From school curriculum support to spoken Hindi for daily life, we cover it all with a warm, engaging approach.",
    color: "#f59e0b",
    icon: "📖",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Fluent reading and writing in Hindi (Devanagari script)",
      "Strong grammar foundation — tenses, genders, cases",
      "Confident spoken Hindi for everyday communication",
      "Excellent comprehension and creative writing skills",
      "Score high in board and school exams",
      "Appreciation for Hindi literature and poetry",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Hindi alphabet (varnamala), simple sentences, stories, poems, picture description",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Grammar (vyakaran), essay writing, letter writing, comprehension, literature",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced grammar, prose & poetry analysis, creative writing, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 studying Hindi under CBSE, ICSE, or Karnataka State Board. Also ideal for non-Hindi speakers who want to learn spoken Hindi or improve conversational skills.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and State Board syllabi for grades 1–10. We cover grammar, literature, comprehension, and creative writing with exam-focused preparation.",
        icon: "📚",
      },
      {
        title: "Spoken Hindi",
        desc: "Conversational Hindi classes for students and adults who want to speak fluently. Ideal for non-native speakers or those looking to improve communication skills.",
        icon: "🗣️",
      },
      {
        title: "Reading & Writing",
        desc: "Develop reading comprehension, essay writing, and Hindi script mastery through progressive, interactive exercises.",
        icon: "✍️",
      },
    ],
  },
  english: {
    name: "English",
    tagline: "Build Strong Communication & Academic Skills",
    description:
      "Our English course empowers students to read, write, and communicate with confidence. From grammar fundamentals to creative expression, we build a solid English foundation for academic and real-world success.",
    color: "#3b82f6",
    icon: "📝",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Strong grammar foundation — tenses, voice, narration, articles",
      "Confident spoken English with clear pronunciation",
      "Creative and academic writing skills — essays, letters, stories",
      "Deep comprehension and literature analysis abilities",
      "Vocabulary enrichment for better expression",
      "High scores in school and board examinations",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Phonics, basic grammar, sentence formation, storytelling, picture composition, reading practice",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Parts of speech, tenses, active/passive voice, comprehension, essay & letter writing, literature",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced grammar, prose & poetry analysis, creative writing, formal writing, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 across CBSE, ICSE, and State Board who want to strengthen English grammar, improve spoken skills, or excel in school and board examinations.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete support for CBSE, ICSE, and State Board English — literature analysis, grammar, writing skills, and comprehensive exam preparation.",
        icon: "📚",
      },
      {
        title: "Spoken English",
        desc: "Interactive speaking practice focusing on pronunciation, vocabulary, fluency, and confidence building for students and adults alike.",
        icon: "🗣️",
      },
      {
        title: "Reading & Writing",
        desc: "Comprehension, creative writing, essay composition, and literary appreciation through structured and engaging sessions.",
        icon: "✍️",
      },
    ],
  },
  kannada: {
    name: "Kannada",
    tagline: "Connect with Karnataka's Rich Language & Culture",
    description:
      "Learn Kannada from expert native-speaking educators. Whether for school exams or daily communication, our Kannada course builds reading, writing, and speaking skills in a supportive environment.",
    color: "#10b981",
    icon: "🏛️",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Fluent reading and writing in Kannada script",
      "Strong foundation in Kannada grammar (Vyakarana)",
      "Appreciate Kannada literature — prose, poetry, and stories",
      "Confident spoken Kannada for daily communication",
      "Excel in school and board examinations",
      "Cultural connection to Karnataka's rich heritage",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Kannada alphabet (Aksharamala), basic words, simple sentences, stories, poems, picture description",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Grammar (Vyakarana), prose & poetry comprehension, essay writing (Prabandha), letter writing",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced grammar, Gadyabhaga & Padyabhaga analysis, creative writing, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 studying Kannada under CBSE, ICSE, or Karnataka State Board. Also great for non-Kannada speakers and new residents of Karnataka who want to learn the language.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and Karnataka State Board syllabi. Grammar, prose, poetry, and exam-focused preparation for grades 1–10.",
        icon: "📚",
      },
      {
        title: "Spoken Kannada",
        desc: "Conversational Kannada for non-native speakers, new residents of Karnataka, and students wanting to improve everyday communication.",
        icon: "🗣️",
      },
      {
        title: "Reading & Writing",
        desc: "Kannada script mastery, reading comprehension, story writing, and vocabulary building through interactive lessons.",
        icon: "✍️",
      },
    ],
  },
  maths: {
    name: "Mathematics",
    tagline: "Build Strong Problem-Solving & Analytical Skills",
    description:
      "Our Mathematics course helps students develop a deep understanding of concepts, strong calculation skills, and confident problem-solving ability. From basic arithmetic to advanced topics, we make Mathematics approachable and enjoyable for school students.",
    color: "#6366f1",
    icon: "📐",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Strong conceptual understanding of all mathematical topics",
      "Fast and accurate calculation skills",
      "Confident problem-solving with step-by-step approach",
      "Ability to tackle word problems and application-based questions",
      "High scores in school exams, Olympiads, and NTSE",
      "Love for Mathematics through engaging teaching",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Numbers, basic arithmetic (addition, subtraction, multiplication, division), shapes, patterns, measurement, time & money",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Fractions, decimals, percentages, integers, basic algebra, geometry, data handling, ratio & proportion",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced algebra, linear equations, quadratic equations, geometry & mensuration, trigonometry, statistics, probability, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 across CBSE, ICSE, and Karnataka State Board who want to build a strong mathematics foundation, improve problem-solving skills, or prepare for competitive exams like Olympiads and NTSE.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Complete coverage of CBSE, ICSE, and Karnataka State Board Mathematics for grades 1–10. Arithmetic, algebra, geometry, mensuration, statistics and exam-focused practice.",
        icon: "📚",
      },
      {
        title: "Concept Building",
        desc: "Step-by-step conceptual learning that builds a solid foundation. We break down complex topics into easy-to-understand segments for lasting understanding.",
        icon: "🧮",
      },
      {
        title: "Problem Practice & Exams",
        desc: "Extensive practice with solved examples, worksheets, past papers, and timed tests to build speed, accuracy, and exam confidence.",
        icon: "✍️",
      },
    ],
  },
  science: {
    name: "Science",
    tagline: "Explore the World Through Curiosity & Discovery",
    description:
      "Our Science course nurtures curiosity, critical thinking, and a love for discovery. Covering Physics, Chemistry, and Biology, we help students understand the natural world and excel in their board exams.",
    color: "#10b981",
    icon: "🔬",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE · State Board" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Clear conceptual understanding of Physics, Chemistry & Biology",
      "Ability to explain scientific phenomena and processes",
      "Strong diagram and practical/lab knowledge",
      "Critical thinking and analytical reasoning skills",
      "Excellent performance in school and board exams",
      "Readiness for Olympiads, NTSE, and competitive exams",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "Living & non-living things, plants & animals, human body basics, water, air, weather, simple machines, materials",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Cell biology, ecosystems, physical & chemical changes, light, sound, electricity basics, measurements",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Physics (force, motion, electricity, magnetism), Chemistry (atoms, reactions, acids & bases, metals), Biology (life processes, genetics, evolution), board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 across CBSE, ICSE, and Karnataka State Board looking to master Physics, Chemistry, and Biology concepts and prepare for board exams, school Olympiads, and NTSE.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE, ICSE, and Karnataka State Board Science syllabi for grades 1–10. Covers Physics, Chemistry, and Biology with clear conceptual understanding.",
        icon: "📚",
      },
      {
        title: "Practical & Lab Concepts",
        desc: "Clear explanations of experiments, diagrams, and practical concepts that help students excel in both theory and practical examinations.",
        icon: "⚗️",
      },
      {
        title: "Exam Preparation",
        desc: "Focused preparation for school exams, Olympiads, and NTSE with structured revision, practice tests, and concept enrichment.",
        icon: "🏆",
      },
    ],
  },
  german: {
    name: "German",
    tagline: "Learn German — Open Doors to Global Opportunities",
    description:
      "Our German course introduces school students to one of Europe's most widely spoken languages. From basic greetings and grammar to reading and writing, we build a strong foundation that helps students excel academically and prepares them for future opportunities.",
    color: "#ef4444",
    icon: "🇩🇪",
    highlights: [
      { icon: "🎓", label: "Grades 1–10" },
      { icon: "📋", label: "CBSE · ICSE" },
      { icon: "💻", label: "Online & Offline" },
      { icon: "👥", label: "Small Batch Size" },
    ],
    outcomes: [
      "Basic to intermediate German communication skills",
      "Strong foundation in German grammar and sentence structure",
      "Confident reading and comprehension in German",
      "Writing skills — essays, letters, descriptions in German",
      "High scores in school and board German exams",
      "Cultural awareness of German-speaking countries",
    ],
    curriculum: [
      {
        grade: "Grades 1–4",
        topics:
          "German alphabet, greetings, numbers, colors, family, animals, basic phrases and vocabulary",
      },
      {
        grade: "Grades 5–7",
        topics:
          "Sentence structure, present tense, articles (der/die/das), everyday conversations, short compositions",
      },
      {
        grade: "Grades 8–10",
        topics:
          "Advanced grammar (cases, tenses, prepositions), comprehension, essay & letter writing, board exam preparation",
      },
    ],
    targetAudience:
      "Students in grades 1–10 studying German as a second or third language under CBSE or ICSE. Also ideal for beginners who want to start learning German from scratch.",
    offerings: [
      {
        title: "School Curriculum",
        desc: "Aligned with CBSE and ICSE German syllabi for grades 1–10. We cover grammar, comprehension, vocabulary, and exam-focused preparation.",
        icon: "📚",
      },
      {
        title: "Spoken German",
        desc: "Interactive conversational German classes to build pronunciation, fluency, and confidence in everyday communication.",
        icon: "🗣️",
      },
      {
        title: "Reading & Writing",
        desc: "Develop reading comprehension, essay writing, and German script mastery through progressive, interactive exercises.",
        icon: "✍️",
      },
    ],
  },
};

const COURSE_EDUCATORS = {
  sanskrit: ["founder", "aayusmita", "sushma", "vimala", "saraswati"],
  hindi: ["founder", "kamalakshi", "sushma", "vimala", "meghna"],
  english: ["meghna", "madhvi", "arundathi", "bharati"],
  kannada: ["arundathi", "kamalakshi", "bharati"],
  maths: ["aayusmita", "meghna", "bharati", "founder"],
  science: ["aayusmita", "meghna", "bharati", "founder"],
  german: ["shrishail", "founder", "madhvi", "meghna"],
};

/* Helper Functions */
function getEducatorsForSubject(subject) {
  const ids = COURSE_EDUCATORS[subject.toLowerCase()] || [];
  return TEAM_DATA.filter((t) => ids.includes(t.id));
}

function renderTeamCard(member) {
  const avatarContent = member.photo
    ? `<img src="${member.photo}" alt="${member.name}" class="team-photo" loading="lazy">`
    : `<span>${member.avatar}</span>`;
  return `
        <div class="team-card" data-aos="fade-up">
            <div class="team-avatar${member.photo ? " has-photo" : ""}" style="background:${member.color}">
                ${avatarContent}
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
                <div class="testimonial-stars">${"★".repeat(t.rating)}</div>
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
              <img src="images/logo.png?v=2" alt="BrightNest Academy" class="logo-icon">
                <span>BrightNest Academy</span>
            </a>
            <ul class="nav-links" id="navLinks">
                <li><a href="index.html" class="nav-link ${activePage === "home" ? "active" : ""}">Home</a></li>
                <li class="nav-dropdown">
                    <a href="courses.html" class="nav-link ${activePage === "courses" ? "active" : ""}">Courses <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-left:4px;vertical-align:middle"><polyline points="6 9 12 15 18 9"/></svg></a>
                    <ul class="dropdown-menu">
                        <li><a href="kannada.html">Kannada</a></li>
                        <li><a href="english.html">English</a></li>
                        <li><a href="hindi.html">Hindi</a></li>
                        <li><a href="sanskrit.html">Sanskrit</a></li>
                        <li><a href="maths.html">Mathematics</a></li>
                        <li><a href="science.html">Science</a></li>
                    </ul>
                </li>
                <li><a href="about.html" class="nav-link ${activePage === "about" || activePage === "team" ? "active" : ""}">About</a></li>
                <li><a href="faq.html" class="nav-link ${activePage === "faq" ? "active" : ""}">FAQ</a></li>
                <li><a href="contact.html" class="nav-link ${activePage === "contact" ? "active" : ""}">Contact</a></li>
                <li><a href="careers.html" class="nav-link ${activePage === "careers" ? "active" : ""}">Careers</a></li>
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
    <footer class="bg-slate-900 text-slate-400 mt-10" style="font-family:inherit;">

      <!-- Top Section -->
      <div class="container pt-16 pb-12">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-10 gap-y-12">

          <!-- Col 1: Brand + Description + Social -->
          <div class="lg:col-span-1 flex flex-col">
            <!-- Logo + Name -->
            <div class="flex items-center gap-3 mb-4">
              <img src="images/logo.png?v=2" alt="BrightNest Academy"
                   style="height:52px;width:auto;flex-shrink:0;">
              <div class="leading-tight">
                <span class="block text-white font-bold text-base tracking-tight">BrightNest</span>
                <span class="block text-indigo-400 font-semibold text-sm tracking-wide">Academy</span>
              </div>
            </div>
            <!-- Tagline -->
            <p class="text-sm text-slate-400 leading-relaxed mb-6" style="max-width:260px;">
              Because every bright mind deserves the right nest. Personalized education in Languages, Mathematics &amp; Science for Grades 1–10.
            </p>
            <!-- Social row -->
            <div class="flex items-center gap-2.5 mt-auto">
              <a href="https://www.facebook.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Facebook"
                 class="w-9 h-9 rounded-full bg-slate-700 hover:bg-blue-600 flex items-center justify-center transition-colors duration-200 flex-shrink-0">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
              </a>
              <a href="https://www.instagram.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Instagram"
                 class="w-9 h-9 rounded-full bg-slate-700 hover:bg-pink-600 flex items-center justify-center transition-colors duration-200 flex-shrink-0">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/></svg>
              </a>
              <a href="https://www.youtube.com/@brightnestacademy" target="_blank" rel="noopener" aria-label="YouTube"
                 class="w-9 h-9 rounded-full bg-slate-700 hover:bg-red-600 flex items-center justify-center transition-colors duration-200 flex-shrink-0">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/></svg>
              </a>
              <a href="https://wa.me/916363464005?text=Hi%20BrightNest%20Academy!%20I%27d%20like%20to%20know%20more%20about%20your%20courses%20and%20enrollment." target="_blank" rel="noopener" aria-label="WhatsApp"
                 class="w-9 h-9 rounded-full bg-slate-700 hover:bg-green-500 flex items-center justify-center transition-colors duration-200 flex-shrink-0">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.019-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
              </a>
            </div>
          </div>

          <!-- Col 2: Subjects -->
          <div>
            <h4 class="text-white font-semibold text-xs uppercase tracking-[0.15em] pb-3 mb-5"
                style="border-bottom:1px solid rgba(99,102,241,0.35);">Subjects</h4>
            <ul class="space-y-3 text-sm">
              <li><a href="kannada.html"  class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Kannada</a></li>
              <li><a href="english.html"  class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>English</a></li>
              <li><a href="hindi.html"    class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Hindi</a></li>
              <li><a href="sanskrit.html" class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Sanskrit</a></li>
              <li><a href="maths.html"    class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Mathematics</a></li>
              <li><a href="science.html"  class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Science</a></li>
              <li><a href="german.html"   class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>German</a></li>
            </ul>
          </div>

          <!-- Col 3: Quick Links + Legal -->
          <div>
            <h4 class="text-white font-semibold text-xs uppercase tracking-[0.15em] pb-3 mb-5"
                style="border-bottom:1px solid rgba(99,102,241,0.35);">Quick Links</h4>
            <ul class="space-y-3 text-sm mb-8">
              <li><a href="about.html"       class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>About Us</a></li>
              <li><a href="faq.html"         class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>FAQ</a></li>
              <li><a href="blog.html"        class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Blog</a></li>
              <li><a href="demo.html"        class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Book a Demo</a></li>
              <li><a href="contact.html"     class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Contact Us</a></li>
              <li><a href="careers.html"     class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Careers</a></li>
              <li><a href="fee-payment.html" class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Pay Fees</a></li>
            </ul>
            <h4 class="text-white font-semibold text-xs uppercase tracking-[0.15em] pb-3 mb-4"
                style="border-bottom:1px solid rgba(99,102,241,0.35);">Legal</h4>
            <ul class="space-y-3 text-sm">
              <li><a href="privacy-policy.html"       class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Privacy Policy</a></li>
              <li><a href="terms-conditions.html"     class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Terms &amp; Conditions</a></li>
              <li><a href="pricing-cancellation.html" class="flex items-center gap-1.5 hover:text-white hover:gap-2.5 transition-all duration-150"><span class="text-indigo-500 text-xs">›</span>Pricing &amp; Cancellation</a></li>
            </ul>
          </div>

          <!-- Col 4: Contact Info -->
          <div>
            <h4 class="text-white font-semibold text-xs uppercase tracking-[0.15em] pb-3 mb-5"
                style="border-bottom:1px solid rgba(99,102,241,0.35);">Get In Touch</h4>
            <ul class="space-y-5 text-sm">
              <!-- Phone -->
              <li class="flex items-center gap-3">
                <span class="w-8 h-8 rounded-md bg-slate-800 flex items-center justify-center flex-shrink-0 text-indigo-400">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z"/></svg>
                </span>
                <a href="tel:+916363464005" class="hover:text-white transition-colors leading-snug">+91-6363464005</a>
              </li>
              <!-- Email -->
              <li class="flex items-center gap-3">
                <span class="w-8 h-8 rounded-md bg-slate-800 flex items-center justify-center flex-shrink-0 text-indigo-400">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                </span>
                <a href="mailto:info@brightnest-academy.com" class="hover:text-white transition-colors leading-snug" style="word-break:break-all;">info@brightnest-academy.com</a>
              </li>
              <!-- Address -->
              <li class="flex items-start gap-3">
                <span class="w-8 h-8 rounded-md bg-slate-800 flex items-center justify-center flex-shrink-0 text-indigo-400 mt-0.5">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
                </span>
                <span class="leading-relaxed">Kumaraswamy Layout,<br>Bengaluru – 560078</span>
              </li>
              <!-- CTA -->
              <li class="pt-1">
                <a href="demo.html"
                   class="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium px-5 py-2.5 rounded-lg transition-colors duration-200">
                  Book a Free Demo
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                </a>
              </li>
            </ul>
          </div>

        </div>
      </div>

      <!-- Divider -->
      <div class="container">
        <div class="border-t border-slate-700/50"></div>
      </div>

      <!-- Bottom Bar -->
      <div class="container py-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
        <p class="text-slate-500">© <span id="year"></span> BrightNest Academy. All rights reserved.</p>
        <div class="flex flex-wrap justify-center items-center gap-x-5 gap-y-1">
          <a href="privacy-policy.html"   class="hover:text-slate-300 transition-colors">Privacy</a>
          <span class="text-slate-700">·</span>
          <a href="terms-conditions.html" class="hover:text-slate-300 transition-colors">Terms</a>
          <span class="text-slate-700">·</span>
          <a href="course-delivery.html"  class="hover:text-slate-300 transition-colors">Course Delivery</a>
          <span class="text-slate-700">·</span>
          <a href="fee-payment.html"      class="hover:text-slate-300 transition-colors">Fee &amp; Payment</a>
        </div>
        <div class="flex flex-col items-center sm:items-end leading-snug">
          <p class="text-xs text-slate-500 opacity-80 hover:opacity-100 transition duration-300">
            <a 
              href="https://www.linkedin.com/in/shrishail-dalawayi-sd-b42718130/"
              target="_blank"
              rel="noopener noreferrer"
              class="hover:text-slate-300 inline-flex items-center gap-1"
            >
              Designed &amp; Developed by Shrishail Dalawayi
              <span class="text-[10px]">↗</span>
            </a>
          </p>
        </div>
      </div>

    </footer>
    <script>
      document.getElementById("year").textContent = new Date().getFullYear();
    </script>
    <!-- WhatsApp Floating Button -->
    <a href="https://wa.me/916363464005?text=Hi%20BrightNest%20Academy!%20I%27m%20interested%20in%20enrolling%20my%20child.%20Please%20share%20course%20details%2C%20schedule%20and%20fees." target="_blank" rel="noopener"
       style="position:fixed;bottom:24px;right:24px;z-index:9999;width:60px;height:60px;border-radius:50%;background:#25D366;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 16px rgba(0,0,0,0.2);transition:transform .2s;text-decoration:none;"
       aria-label="Chat on WhatsApp"
       onmouseover="this.style.transform='scale(1.1)'" onmouseout="this.style.transform='scale(1)'">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="white"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.019-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
    </a>
  `;
}

/* Course Detail Renderers */
function renderCourseHighlights(info) {
  if (!info.highlights) return "";
  return info.highlights
    .map(
      (h) => `
    <div class="highlight-badge">
      <span class="highlight-icon">${h.icon}</span>
      <span class="highlight-label">${h.label}</span>
    </div>
  `,
    )
    .join("");
}

function renderCourseOutcomes(info) {
  if (!info.outcomes) return "";
  return `<ul class="outcomes-list">
    ${info.outcomes.map((o) => `<li><svg class="check-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg><span>${o}</span></li>`).join("")}
  </ul>`;
}

function renderCourseCurriculum(info) {
  if (!info.curriculum) return "";
  return info.curriculum
    .map(
      (c) => `
    <div class="curriculum-card">
      <div class="curriculum-grade">${c.grade}</div>
      <p class="curriculum-topics">${c.topics}</p>
    </div>
  `,
    )
    .join("");
}

function renderCourseAudience(info) {
  if (!info.targetAudience) return "";
  return `<div class="audience-box">
    <div class="audience-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></div>
    <p>${info.targetAudience}</p>
  </div>`;
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
window.renderCourseHighlights = renderCourseHighlights;
window.renderCourseOutcomes = renderCourseOutcomes;
window.renderCourseCurriculum = renderCourseCurriculum;
window.renderCourseAudience = renderCourseAudience;
