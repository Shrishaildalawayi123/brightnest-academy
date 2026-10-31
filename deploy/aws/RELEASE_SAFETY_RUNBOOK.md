# Release startup and rollback safety

This procedure replaces the inline blue-green deployment in `deploy.yml`. Do not
run it on production until its staging acceptance checklist has passed. The PR
itself does not initialize Nginx, migrate a database, rotate credentials, or deploy.

## Runtime contract

- Java 21. The image is built with the tested Git revision in its OCI revision
  label and `RELEASE_REVISION`. Deployment accepts a lowercase GHCR digest reference
  and verifies the label before creating a container.
- `/opt/brightnest/.env`: deployment-user owned, mode `600`, literal Docker
  `KEY=value` syntax (no shell sourcing, quotes, substitution, or duplicate keys).
  Start from `.env.release.example`; fill secrets privately.
- A Docker network named `brightnest` must already exist. Use explicit database and
  Redis endpoints reachable on that network, not `localhost`/`127.0.0.1` inside the
  container. For external RDS/Redis, verify EC2 egress, security groups and TLS.
  Local host services need an explicitly provisioned reachable address. The
  deployment never guesses or changes their networking.
- The database name must match the prepared schema. Production retains
  `ddl-auto=validate`; never substitute `update` to get startup past validation.
  MySQL requires TLS. Check the negotiated session cipher and certificate policy.
- Redis is required in production and initialized eagerly. Do not switch to an
  in-memory limiter as a workaround. Configure Redis authentication/TLS as required.
- Production resumes use S3 only. Provide the bucket, region, and an EC2 IAM role
  permitting required object access. Confirm SDK credentials are reachable from
  containers (including the EC2 metadata configuration). S3 failure rejects the
  upload; it never reports success using ephemeral disk. Local storage remains
  available for development only. Back up existing local uploads before migration;
  changing this policy does not migrate old files or add an administrator download UI.
- Leave both `ADMIN_EMAIL` and `ADMIN_PASSWORD` blank unless creating an absent
  administrator intentionally. Existing passwords are never synchronized on restart.
  Unsafe defaults are rejected before the startup initializer runs.
- `/health` returns HTTP 200 only when the readiness group is UP, otherwise 503.
  Production readiness includes application readiness state, MySQL and Redis. It
  returns the release revision but no dependency details. S3 upload/retrieval is a
  separate smoke check, not a dependency check on every health request.
- Candidates bind only `127.0.0.1:18080` or `:18081`. Containers use a read-only
  root filesystem, writable temporary memory, UID 10001, and bounded logs/resources.
  Verify the EC2 instance can run two 512 MiB containers plus database/Redis/OS.
- The deployment user needs Docker access and narrowly configured noninteractive
  sudo for Nginx validation/reload and installation of the upstream include. Do not
  grant blanket sudo as a shortcut. Python 3, curl, flock, and GNU coreutils are required.
- Authenticate the host to GHCR in advance if the package is private. The workflow
  does not send registry credentials to EC2 or make registry packages public.

## One-time host preparation (staging first; operator controlled)

1. Back up and inspect the effective Nginx configuration. Resolve duplicate upstream
   definitions intentionally. The new configuration expects one named upstream with
   this include inside it:

   ```nginx
   upstream brightnest_backend {
       include /etc/nginx/brightnest/active-backend.conf;
       keepalive 32;
   }
   ```

2. Create `/etc/nginx/brightnest/active-backend.conf` with exactly one line pointing
   at the **currently serving** app. For the legacy service at port 8080:

   ```nginx
   server 127.0.0.1:8080;
   ```

   Confirm the enabled site actually uses `brightnest_backend`. The Nginx `/health`
   location must forward `X-Forwarded-Proto https`. Validate and reload only after
   preserving the old configuration. Set up `/opt/brightnest/releases` so the deployment
   user can receive commit-specific tools from the workflow.

3. Create `/opt/brightnest/active-release`, owned by the deployment user, with:

   ```text
   legacy legacy legacy 8080
   ```

   Only use this record when the existing service really is on 8080. Legacy migration
   verifies status but cannot verify a revision that the old app never returned.
   After migration, all blue/green records require exact digest and revision identity.
   Never fabricate a blue/green record. Do not stop the legacy service until both
   deployment and rollback have been rehearsed. Its automatic restart configuration
   should remain available during migration.

4. Confirm the public HTTPS `/health` is healthy, database backup/restore is tested,
   S3 access works, and the environment validator passes:

   ```bash
   python3 deploy/aws/scripts/validate-release-env.py /opt/brightnest/.env
   ```

   Do not print `.env`, Docker environment inspection, or secrets into CI logs.

## Deployment and rollback

The workflow invokes the tools from `/opt/brightnest/releases/<sha>/deploy/aws/scripts`.
It only deploys from main after its existing Java/frontend/dependency/image gates;
coverage thresholds and security scan thresholds are unchanged by this patch.
The separate main CI and staging workflows are not yet unified into a promotion gate.

For a staging rehearsal, use staging-specific paths, environment and HTTPS origin:

```bash
PUBLIC_URL=https://staging.example.com \
RELEASE_DIR=/opt/brightnest-staging \
UPSTREAM_FILE=/etc/nginx/brightnest-staging/active-backend.conf \
DOCKER_NETWORK=brightnest-staging \
bash deploy/aws/scripts/release-blue-green.sh deploy "$SCANNED_IMAGE_DIGEST" "$TESTED_GIT_SHA"
```

Use a separate staging EC2 host (slot container names and ports are fixed). For a
production rollback after approval, invoke the exact tools from the successful release:

```bash
bash /opt/brightnest/releases/<sha>/deploy/aws/scripts/release-blue-green.sh rollback
```

Do not use the old Compose `rollback-release.sh` on this deployment. The blue-green
script keeps the previous container running with its original configuration. Rollback
changes routing to it and verifies its recorded revision; it does not recreate it with
possibly changed credentials. This cannot roll back database schema changes.

## Failure handling

- Validate state, active upstream, baseline HTTPS health, configuration and image
  identity before deleting any inactive container.
- Wait for candidate dependency readiness on the loopback port with forwarded HTTPS.
- Atomically replace the upstream include, validate Nginx, reload, and verify the
  public HTTPS origin returns the candidate revision. Redirects fail verification.
- On validation/reload/post-switch failure, restore the previous include and metadata,
  reload, verify the previous revision, and remove the failed candidate only after
  successful recovery. Keep the previous container after success too.
- If recovery cannot be verified, retain both containers and transaction evidence;
  the command exits nonzero and requires operator investigation.
- `flock` prevents overlapping deployment/rollback processes. SIGINT/SIGTERM trigger
  recovery. SIGKILL, power loss or host failure cannot execute a trap: compare the
  active include, running containers, HTTPS revision and retained transaction directory
  before restoring state. The next run refuses inconsistent metadata instead of guessing.

## Acceptance checklist (must run before production)

- [ ] Clean Java 21 tests and generated JaCoCo report; no lowered gates.
- [ ] `python3 -m unittest discover -s deploy/aws/tests -v` and shellcheck pass.
- [ ] Frontend tests pass; dashboard test waits for observable async completion.
- [ ] Build the image and prove UID/filesystem/S3-mode startup without a writable `/app`.
- [ ] Run against a restored production-like MySQL schema with TLS and required Redis.
- [ ] Missing JWT/database/Redis/S3 configuration fails before traffic changes.
- [ ] Two consecutive deployments alternate slots and return the tested SHA through HTTPS.
- [ ] Rehearse candidate failure, Nginx validation/reload failure and post-switch failure.
- [ ] Force rollback after the second deployment; previous SHA returns and both containers remain.
- [ ] Upload/retrieve a resume before replacement and after rollback; S3 failure yields an error.
- [ ] Student/administrator/tenant-isolation, teacher applications and lead submission smoke checks pass.

Other audit findings (dashboard aggregates/stats, auth alias policy, sitemap, full QA
script repair and shared promotion gates) remain separate work. This patch alone is
not a declaration that production is ready.
