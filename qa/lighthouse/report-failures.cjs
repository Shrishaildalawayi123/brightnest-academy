// Print actionable public-page accessibility evidence even when assertions fail.
const fs = require('node:fs');
const path = require('node:path');
const directory = '.lighthouseci';
if (!fs.existsSync(directory)) process.exit(0);
for (const filename of fs.readdirSync(directory).filter(name => name.endsWith('.json'))) {
  let report;
  try { report = JSON.parse(fs.readFileSync(path.join(directory, filename), 'utf8')); }
  catch { continue; }
  const category = report.categories?.accessibility;
  if (!category) continue;
  const failures = category.auditRefs.filter(ref => ref.weight > 0).map(ref => report.audits[ref.id])
    .filter(audit => audit && audit.score !== null && audit.score < 1)
    .map(audit => ({
      id: audit.id, title: audit.title,
      nodes: (audit.details?.items || []).slice(0, 8).map(item => ({
        selector: item.node?.selector, explanation: item.node?.explanation,
        snippet: item.node?.snippet, related: item.relatedData?.contrastRatio,
      })),
    }));
  console.log(JSON.stringify({url: report.finalUrl, accessibility: category.score, failures}, null, 2));
}
