# QA Automation (Unit + E2E)

## Prerequisites
- Node.js 20+
- Application running at `http://127.0.0.1:8080` (or set `E2E_BASE_URL`)

## Install
```bash
cd qa
npm install
npm run install:browsers
```

## Run Tests
```bash
# JS unit tests (api.js, auth.js, app.js)
npm run test:unit

# E2E tests (Chromium only)
npm run test:e2e:chromium

# E2E tests (Chromium + Firefox + WebKit)
npm run test:e2e:cross-browser
```

## Environment Variables
- `E2E_BASE_URL`: Base URL for Playwright tests (default: `http://127.0.0.1:8080`)
- `CROSS_BROWSER=true`: enable all browser projects in Playwright config
