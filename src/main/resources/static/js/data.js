/**
 * BrightNest Academy - Shared Data
 * Team members, FAQ, testimonials, course info used across pages
 */

// ---------------------------------------------------------------------------
// Backend API URL bootstrap
// Loaded before api.js on every page, so this is the right place to set the
// backend URL for split deployments (e.g. Amplify frontend + EC2 API).
//
// Priority (highest first):
//   1. window.__API_BASE_URL__  — set by api-config.js or programmatically
//   2. <meta name="x-api-base-url" content="...">  — set at server / CI time
//   3. BACKEND_API_URL injected at Amplify build time into api.js (fallback)
//
// For same-origin deployments (EC2 monolith) leave everything untouched;
// api.js will use "/api" by default.
// ---------------------------------------------------------------------------
(function () {
  if (window.__API_BASE_URL__) return; // already set
  var metaTag = document.querySelector('meta[name="x-api-base-url"]');
  if (metaTag && metaTag.content) {
    window.__API_BASE_URL__ = metaTag.content.replace(/\/+$/, "");
  }
})();

const TEAM_DATA = [
  {
    id: "founder",
    name: "Bharati R Satappagol",
    email: "bharati@brightnest-academy.com",
    role: "Founder & Lead Educator",
    subjects: ["Kannada", "English", "Science", "Mathematics"],
    expertise: "Board strategy, language mastery, and parent mentoring",
    experienceYears: 10,
    bio: "10 years of teaching experience — consistently delivering uncompromised quality and impactful learning.",
    avatar: "👩‍🏫",
    photo: "images/bharati_updated_pic.jpeg?v=1",
    color: "#6366f1",
  },
  {
    id: "chetana",
    name: "Chetana",
    email: "chetana@brightnest-academy.com",
    role: "English Educator",
    subjects: ["English"],
    expertise: "Grammar, comprehension, and spoken confidence",
    experienceYears: 8,
    bio: "8 years of teaching experience — focused on grammar, writing skills, reading comprehension, and spoken confidence with clear explanations and regular practice.",
    avatar: "👩‍🏫",
    photo: "images/Chetana_English.jpeg",
    color: "#3b82f6",
  },
  {
    id: "mahadev",
    name: "Mahadev S",
    email: "mahadev@brightnest-academy.com",
    role: "Spoken Language Educator",
    subjects: ["Kannada (Spoken)", "Hindi (Spoken)"],
    expertise: "Conversational fluency and pronunciation coaching",
    experienceYears: 17,
    bio: "17 years of teaching experience — helping learners build everyday speaking confidence with structured conversational practice, pronunciation support, and role-play based learning.",
    avatar: "👨‍🏫",
    photo: "images/Mahadev%20S_Spoken%20Kannada%20Spoken%20Hindi.jpeg",
    color: "#10b981",
  },
  {
    id: "pooja",
    name: "Pooja",
    email: "pooja@brightnest-academy.com",
    role: "Science Educator",
    subjects: ["Science"],
    expertise: "Physics-Chemistry-Biology concept clarity",
    experienceYears: 5,
    bio: "5 years of teaching experience — explains concepts with real-life examples and step-by-step clarity, helping students strengthen fundamentals and score better in school exams.",
    avatar: "👩‍🔬",
    photo: "images/Pooja_Science.jpg",
    color: "#10b981",
  },
  {
    id: "nagesh",
    name: "Nagesh Kumar M U",
    email: "nagesh@brightnest-academy.com",
    role: "Senior Educator",
    subjects: ["Mathematics"],
    expertise: "Advanced problem-solving and exam score strategy",
    experienceYears: 25,
    bio: "25 years of teaching experience — concept-first Mathematics with a strong focus on fundamentals, problem-solving, and exam readiness.",
    avatar: "👨‍🔬",
    photo: "images/Nagesh%20Kumar%20M%20U_MSc%20Bed.jpeg",
    color: "#06b6d4",
  },
  {
    id: "preeti",
    name: "Preeti R S",
    email: "preeti@brightnest-academy.com",
    role: "Mathematics Educator",
    subjects: ["Mathematics"],
    expertise: "Foundation maths, worksheets, and speed building",
    experienceYears: 6,
    bio: "6 years of teaching experience — builds strong fundamentals, problem-solving speed, and confidence through guided practice, worksheets, and exam-oriented preparation.",
    avatar: "👩‍🏫",
    photo: "images/Preeti_Mathematics.jpg",
    color: "#6366f1",
  },
  {
    id: "prema",
    name: "Prema G",
    email: "prema@brightnest-academy.com",
    role: "English & Mathematics Educator",
    subjects: ["English", "Mathematics"],
    expertise: "Dual-subject support for school performance",
    experienceYears: 7,
    bio: "Supports English and Mathematics learning with concept clarity, practice-driven sessions, and steady academic guidance for school students.",
    avatar: "👩‍🏫",
    photo: "images/Prema%20G_English-Maths.png",
    color: "#8b5cf6",
  },
  {
    id: "shrishail",
    name: "Mr. Shrishail Dalawayi",
    email: "shrishail@brightnest-academy.com",
    role: "Educator",
    subjects: ["German", "Kannada", "Computer Science"],
    expertise: "German language foundation and practical CS basics",
    experienceYears: 2,
    bio: "2 years of teaching experience — supports German, Kannada, and Computer Science learning through interactive and structured teaching methods.",
    avatar: "👨‍🏫",
    photo: "images/shrishail.jpg",
    color: "#ef4444",
  },
];

const FAQ_DATA = {
  general: [
    {
      q: "Which grades and boards do you currently support?",
      a: "We support Grades 1-10 for CBSE, ICSE, and Karnataka State Board with board-aligned worksheets, weekly tests, and revision plans.",
    },
    {
      q: "Do you provide both online and offline classes?",
      a: "Yes. Students can learn online on Google Meet or attend offline classes at our Banashankari center in Bengaluru.",
    },
    {
      q: "How do parents start admission?",
      a: "Book a demo from our website or call +91-7204193980. After a quick need-analysis call, we suggest the right batch and share the enrollment steps.",
    },
    {
      q: "Where is your coaching center located?",
      a: "#662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bengaluru - 560078.",
    },
    {
      q: "What are your operating hours?",
      a: "We are open all days from 9:00 AM to 9:00 PM for calls, WhatsApp support, counseling, and classes.",
    },
    {
      q: "How often do parents receive progress updates?",
      a: "Parents receive regular updates through tests, assignment reviews, and faculty feedback sessions so progress stays transparent.",
    },
  ],
  courseSpecific: [
    {
      q: "How are Mathematics classes structured for Grades 6-10?",
      a: "Math classes focus on concept clarity, chapterwise worksheets, timed practice, and error-analysis to improve both accuracy and speed.",
    },
    {
      q: "Do you run separate batches for Science by grade level?",
      a: "Yes. Science batches are grouped by grade bands so Physics, Chemistry, and Biology depth matches school expectations.",
    },
    {
      q: "Can my child enroll in two subjects together?",
      a: "Absolutely. We routinely combine subjects like Maths + Science or English + Kannada and assign non-overlapping time slots.",
    },
    {
      q: "Do you support spoken language improvement as well as academics?",
      a: "Yes. Along with school syllabus support, we provide spoken English, spoken Kannada, and spoken Hindi practice modules.",
    },
    {
      q: "How do you help board exam students in Grades 9-10?",
      a: "Board students receive weekly test cycles, revision plans, previous-year paper practice, and focused doubt-clearing support.",
    },
  ],
  feePayment: [
    {
      q: "Do you have indicative monthly fee ranges?",
      a: "Yes. Indicative ranges are published on our Pricing page by grade group and subject, and final fees are confirmed after demo assessment.",
    },
    {
      q: "What is the demo class fee?",
      a: "The demo class fee is INR 100. It is adjusted in first-month tuition on enrollment or refundable within 30 days if you do not enroll.",
    },
    {
      q: "Which payment options do you accept?",
      a: "We accept UPI, Razorpay links, and bank transfer. Payment confirmation is shared by receipt and tracked in your student record.",
    },
    {
      q: "Do you offer monthly, quarterly, or annual plans?",
      a: "Yes. We offer monthly, quarterly, and annual plans. Families choosing longer plans may receive preferential pricing.",
    },
    {
      q: "Can I see batch timings before confirming admission?",
      a: "Yes. We share available days, timings, and batch-size options by subject before enrollment so parents can choose comfortably.",
    },
  ],
};

const TESTIMONIALS = [
  {
    text: "My daughter moved from 81% to 96% in Sanskrit and from 78% to 94% in Hindi within one academic year. The teaching quality and follow-up were excellent.",
    author: "Sandeep Patil",
    relation: "Parent",
    childGrade: "Grade 10 (CBSE)",
    course: "Sanskrit & Hindi",
    rating: 5,
  },
  {
    text: "My son improved from 64% to 86% in Kannada in two terms. The teachers gave clear worksheets and regular revision support.",
    author: "Priya Menon",
    relation: "Parent",
    childGrade: "Grade 7 (State Board)",
    course: "Kannada",
    rating: 5,
  },
  {
    text: "My daughter's Mathematics score improved from 65% to 95% in just 3 months. The teachers explain every concept step-by-step with great patience.",
    author: "Rahul Sharma",
    relation: "Parent",
    childGrade: "Grade 9 (ICSE)",
    course: "Mathematics",
    rating: 5,
  },
  {
    text: "English improved from 58% to 84% after targeted grammar and writing sessions. My child now participates confidently in school.",
    author: "Meera Iyer",
    relation: "Parent",
    childGrade: "Grade 8 (CBSE)",
    course: "English",
    rating: 5,
  },
  {
    text: "Online Hindi sessions helped my daughter move from 69% to 90% while balancing school workload. Very interactive and well structured.",
    author: "Arvind Kumar",
    relation: "Parent",
    childGrade: "Grade 6 (State Board)",
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
  sanskrit: ["founder"],
  hindi: ["founder", "mahadev"],
  english: ["founder", "chetana", "prema"],
  kannada: ["founder", "mahadev", "shrishail"],
  maths: ["founder", "preeti", "nagesh", "prema"],
  science: ["founder", "pooja"],
  german: ["founder", "shrishail"],
};

/* Helper Functions */
function getEducatorsForSubject(subject) {
  const ids = COURSE_EDUCATORS[subject.toLowerCase()] || [];
  return TEAM_DATA.filter((t) => ids.includes(t.id));
}

function findTeamMemberByTeacher(teacher) {
  if (!teacher) {
    return null;
  }

  const teacherEmail = (teacher.email || "").trim().toLowerCase();
  const teacherName = (teacher.name || "").trim().toLowerCase();

  return (
    TEAM_DATA.find((member) => {
      const memberEmail = (member.email || "").trim().toLowerCase();
      const memberName = (member.name || "").trim().toLowerCase();
      return (
        (teacherEmail && memberEmail === teacherEmail) ||
        (teacherName && memberName === teacherName)
      );
    }) || null
  );
}

function buildAssignedEducatorProfile(teacher, subjectName) {
  if (!teacher) {
    return null;
  }

  const matchedMember = findTeamMemberByTeacher(teacher);
  if (matchedMember) {
    return matchedMember;
  }

  return {
    id: `assigned-${teacher.id || "faculty"}`,
    name: teacher.name || "Assigned Faculty",
    email: teacher.email || "",
    role: teacher.role === "ADMIN" ? "Lead Educator" : "Subject Educator",
    subjects: subjectName ? [subjectName] : [],
    bio: subjectName
      ? `Assigned faculty for ${subjectName} at BrightNest Academy.`
      : "Assigned faculty at BrightNest Academy.",
    avatar: "👩‍🏫",
    color: "#3b82f6",
  };
}

function renderTeamCard(member) {
  const avatarContent = member.photo
    ? `<img src="${member.photo}" alt="${member.name}" class="team-photo" loading="lazy">`
    : `<span>${member.avatar}</span>`;
  const expertise = member.expertise
    ? `<p class="team-role" style="font-size:0.85rem;opacity:0.9;">Expertise: ${member.expertise}</p>`
    : "";
  const experience = member.experienceYears
    ? `<p class="team-role" style="font-size:0.85rem;font-weight:600;">Experience: ${member.experienceYears}+ years</p>`
    : "";
  return `
        <div class="team-card" data-aos="fade-up">
            <div class="team-avatar${member.photo ? " has-photo" : ""}" style="background:${member.color}">
                ${avatarContent}
            </div>
            <h3 class="team-name">${member.name}</h3>
            <p class="team-role">${member.role}</p>
            ${experience}
            ${expertise}
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
    const authorLine = [
      t.author,
      t.relation || "Parent",
      t.childGrade ? `Child: ${t.childGrade}` : "",
    ]
      .filter(Boolean)
      .join(" • ");
    container.innerHTML = `
            <div class="testimonial-card">
                <div class="testimonial-stars">${"★".repeat(t.rating)}</div>
                <p class="testimonial-text">"${t.text}"</p>
                <div class="testimonial-author">
                    <strong>${authorLine}</strong>
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
                    <li><a href="admissions.html" class="nav-link ${activePage === "admissions" ? "active" : ""}">Admissions</a></li>
                    <li><a href="team.html" class="nav-link ${activePage === "team" ? "active" : ""}">Faculty</a></li>
                    <li><a href="testimonials.html" class="nav-link ${activePage === "testimonials" ? "active" : ""}">Testimonials</a></li>
                    <li><a href="blog.html" class="nav-link ${activePage === "blog" ? "active" : ""}">Blog</a></li>
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
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div>
            <a href="index.html" class="footer-logo">
              <img src="images/logo.png?v=2" alt="BrightNest Academy" class="logo-icon">
              <span>BrightNest Academy</span>
            </a>
            <p class="footer-desc">
              Because every bright mind deserves the right nest. Personalized tuition in Languages, Mathematics and Science for Grades 1 to 10.
            </p>
            <div class="footer-social">
              <a href="https://g.page/r/CV8X7tU64yV5EBM/review" target="_blank" rel="noopener" aria-label="Google Reviews"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l2.7 5.46L20.7 8.3l-4.35 4.24 1.03 6L12 15.9 6.62 18.54l1.03-6L3.3 8.3l6-.84L12 2z"/></svg></a>
              <a href="https://www.facebook.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Facebook"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12S0 5.446 0 12.073c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg></a>
              <a href="https://www.instagram.com/brightnestacademy" target="_blank" rel="noopener" aria-label="Instagram"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069z"/></svg></a>
              <a href="https://wa.me/917204193980?text=Hello%20BrightNest%20Academy%2C%20I%20want%20to%20know%20about%20classes." target="_blank" rel="noopener" aria-label="WhatsApp"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.52 3.48A11.82 11.82 0 0012.11 0C5.6 0 .3 5.3.3 11.8c0 2.08.54 4.11 1.56 5.9L0 24l6.5-1.83a11.77 11.77 0 005.61 1.43h.01c6.5 0 11.8-5.3 11.8-11.8a11.7 11.7 0 00-3.4-8.32zM12.12 21.6h-.01a9.8 9.8 0 01-4.98-1.36l-.36-.22-3.86 1.09 1.03-3.76-.23-.38a9.75 9.75 0 01-1.5-5.18c0-5.4 4.4-9.8 9.82-9.8 2.62 0 5.08 1.02 6.93 2.87a9.72 9.72 0 012.86 6.93c0 5.42-4.4 9.81-9.8 9.81z"/></svg></a>
              <a href="https://www.youtube.com/@brightnestacademy" target="_blank" rel="noopener" aria-label="YouTube"><svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/></svg></a>
            </div>
          </div>

          <div class="footer-col">
            <h4>Subjects</h4>
            <ul>
              <li><a href="kannada.html">Kannada</a></li>
              <li><a href="english.html">English</a></li>
              <li><a href="hindi.html">Hindi</a></li>
              <li><a href="sanskrit.html">Sanskrit</a></li>
              <li><a href="maths.html">Mathematics</a></li>
              <li><a href="science.html">Science</a></li>
              <li><a href="german.html">German</a></li>
            </ul>
          </div>

          <div class="footer-col">
            <h4>Quick Links</h4>
            <ul>
              <li><a href="about.html">About Us</a></li>
              <li><a href="admissions.html">Admissions</a></li>
              <li><a href="team.html">Faculty</a></li>
              <li><a href="testimonials.html">Testimonials</a></li>
              <li><a href="blog.html">Blog</a></li>
              <li><a href="faq.html">FAQ</a></li>
              <li><a href="contact.html">Contact</a></li>
              <li><a href="demo.html">Book a Demo</a></li>
              <li><a href="privacy-policy.html">Privacy Policy</a></li>
              <li><a href="terms-conditions.html">Terms and Conditions</a></li>
              <li><a href="pricing-cancellation.html">Pricing and Cancellation</a></li>
            </ul>
          </div>

          <div class="footer-col">
            <h4>Get In Touch</h4>
            <ul>
              <li><a href="tel:+917204193980">+91-7204193980</a></li>
              <li><a href="mailto:info@brightnest-academy.com">info@brightnest-academy.com</a></li>
              <li><a href="https://www.google.com/maps/dir/?api=1&destination=12.9047330,77.5595019" target="_blank" rel="noopener">Get Directions</a></li>
            </ul>
            <div class="footer-nap">
              <strong>BrightNest Academy</strong>
              <span>#662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bengaluru - 560078</span>
              <span>Open all days: 9:00 AM - 9:00 PM</span>
            </div>
          </div>
        </div>

        <div class="footer-contact">
          <span><a href="tel:+917204193980">Call: +91-7204193980</a></span>
          <span><a href="https://www.google.com/maps/dir/?api=1&destination=12.9047330,77.5595019" target="_blank" rel="noopener">Directions</a></span>
        </div>

        <div class="footer-bottom">
          <p>© <span id="year"></span> BrightNest Academy. All rights reserved.</p>
          <p>Designed and Developed by Shrishail Dalawayi |
            <a href="https://www.linkedin.com/in/shrishail-dalawayi-b42718130/" target="_blank" rel="noopener">LinkedIn</a>
          </p>
        </div>
      </div>
    </footer>
    <script>
      document.getElementById("year").textContent = new Date().getFullYear();
    </script>
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
window.findTeamMemberByTeacher = findTeamMemberByTeacher;
window.buildAssignedEducatorProfile = buildAssignedEducatorProfile;
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
