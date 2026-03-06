import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

describe("subject page assigned faculty", () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="courseTagline"></div>
      <div id="courseDescription"></div>
      <div id="offeringsGrid"></div>
      <div id="educatorsGrid"></div>
      <div id="courseHighlights"></div>
      <div id="outcomesContainer"></div>
      <div id="curriculumGrid"></div>
      <div id="audienceContainer"></div>
      <div id="footerPlaceholder"></div>
    `;

    window.COURSE_INFO = {
      english: {
        name: "English",
        tagline: "Build confident communication.",
        description: "English subject description.",
        offerings: [
          { icon: "A", title: "Grammar", desc: "Learn grammar." },
        ],
      },
    };
    window.API = {
      getCourseBySubject: vi.fn().mockResolvedValue({
        teacher: {
          id: 7,
          name: "Prema G",
          email: "prema@brightnest-academy.com",
          role: "TEACHER",
        },
      }),
    };
    window.buildAssignedEducatorProfile = vi.fn().mockReturnValue({
      id: "prema",
      name: "Prema G",
      subjects: ["English"],
    });
    window.renderTeamCard = vi
      .fn()
      .mockReturnValue('<article class="team-card">Prema G</article>');
    window.renderCourseHighlights = vi.fn().mockReturnValue("<div>Highlights</div>");
    window.renderCourseOutcomes = vi.fn().mockReturnValue("<div>Outcomes</div>");
    window.renderCourseCurriculum = vi.fn().mockReturnValue("<div>Curriculum</div>");
    window.renderCourseAudience = vi.fn().mockReturnValue("<div>Audience</div>");
    window.getFooterHTML = vi.fn().mockReturnValue("<footer>Footer</footer>");
  });

  it("renders only the assigned faculty for a subject", async () => {
    loadBrowserScript("subject-page.js");

    await window.renderSubjectPage("english");

    expect(window.API.getCourseBySubject).toHaveBeenCalledWith("english");
    expect(window.buildAssignedEducatorProfile).toHaveBeenCalledWith(
      {
        id: 7,
        name: "Prema G",
        email: "prema@brightnest-academy.com",
        role: "TEACHER",
      },
      "English",
    );
    expect(window.renderTeamCard).toHaveBeenCalledWith({
      id: "prema",
      name: "Prema G",
      subjects: ["English"],
    });
    expect(document.getElementById("courseTagline").textContent).toBe(
      "Build confident communication.",
    );
    expect(document.getElementById("educatorsGrid").innerHTML).toContain(
      "Prema G",
    );
    expect(document.getElementById("footerPlaceholder").innerHTML).toContain(
      "Footer",
    );
  });
});