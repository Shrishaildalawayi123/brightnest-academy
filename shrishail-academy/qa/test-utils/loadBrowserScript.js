import fs from "node:fs";
import path from "node:path";

const STATIC_JS_ROOT = path.resolve(process.cwd(), "../src/main/resources/static/js");

export function loadBrowserScript(fileName) {
  const fullPath = path.join(STATIC_JS_ROOT, fileName);
  const source = fs.readFileSync(fullPath, "utf8");
  window.eval(`${source}\n//# sourceURL=${fullPath.replace(/\\/g, "/")}`);
}
