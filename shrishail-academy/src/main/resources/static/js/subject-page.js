(function () {
  async function renderSubjectPage(courseKey) {
    const info = window.COURSE_INFO ? window.COURSE_INFO[courseKey] : null;
    if (!info) {
      return;
    }

    const taglineEl = document.getElementById("courseTagline");
    const descriptionEl = document.getElementById("courseDescription");
    const offeringsGrid = document.getElementById("offeringsGrid");
    const educatorsGrid = document.getElementById("educatorsGrid");
    const highlightsEl = document.getElementById("courseHighlights");
    const outcomesEl = document.getElementById("outcomesContainer");
    const curriculumEl = document.getElementById("curriculumGrid");
    const audienceEl = document.getElementById("audienceContainer");
    const footerEl = document.getElementById("footerPlaceholder");

    if (taglineEl) {
      taglineEl.textContent = info.tagline;
    }
    if (descriptionEl) {
      descriptionEl.textContent = info.description;
    }
    if (offeringsGrid) {
      offeringsGrid.innerHTML = info.offerings
        .map(
          (offering) =>
            '<div class="offering-card">' +
            '<div class="offering-icon">' +
            offering.icon +
            "</div>" +
            "<h3>" +
            offering.title +
            "</h3>" +
            "<p>" +
            offering.desc +
            "</p>" +
            "</div>",
        )
        .join("");
    }
    if (highlightsEl && window.renderCourseHighlights) {
      highlightsEl.innerHTML = renderCourseHighlights(info);
    }
    if (outcomesEl && window.renderCourseOutcomes) {
      outcomesEl.innerHTML = renderCourseOutcomes(info);
    }
    if (curriculumEl && window.renderCourseCurriculum) {
      curriculumEl.innerHTML = renderCourseCurriculum(info);
    }
    if (audienceEl && window.renderCourseAudience) {
      audienceEl.innerHTML = renderCourseAudience(info);
    }
    if (footerEl && window.getFooterHTML) {
      footerEl.innerHTML = getFooterHTML();
    }

    if (!educatorsGrid) {
      return;
    }

    educatorsGrid.innerHTML =
      '<div class="empty-state">Loading assigned faculty...</div>';

    try {
      const course = await window.API.getCourseBySubject(courseKey);
      const educatorProfile = window.buildAssignedEducatorProfile(
        course.teacher,
        info.name,
      );

      if (!educatorProfile) {
        educatorsGrid.innerHTML =
          '<div class="empty-state">Faculty assignment will be available shortly.</div>';
        return;
      }

      educatorsGrid.innerHTML = window.renderTeamCard(educatorProfile);
    } catch (error) {
      console.error("Failed to load assigned faculty", error);
      educatorsGrid.innerHTML =
        '<div class="empty-state">Unable to load faculty details right now.</div>';
    }
  }

  window.renderSubjectPage = renderSubjectPage;
})();
