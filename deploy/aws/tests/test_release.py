"""Transaction tests use fake Docker/Nginx/curl; never contact a host or registry."""
import importlib.util
import fcntl
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
spec = importlib.util.spec_from_file_location("release_env", SCRIPTS / "validate-release-env.py")
release_env = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release_env)

# One fake executable, symlinked under the names used by the deployment script.
MOCK = r'''#!/usr/bin/env python3
import json, os, re, subprocess, sys
from pathlib import Path
name = Path(sys.argv[0]).name
args = sys.argv[1:]
root = Path(os.environ['FAKE_ROOT'])
state_path = root / 'docker.json'
state = json.loads(state_path.read_text())
def save(): state_path.write_text(json.dumps(state))
if name == 'sudo':
    sys.exit(subprocess.run(args).returncode)
if name == 'sleep': sys.exit(0)
if name == 'nginx':
    count = int((root / 'nginx-count').read_text()) + 1 if (root / 'nginx-count').exists() else 1
    (root / 'nginx-count').write_text(str(count))
    if os.environ.get('FAIL_NGINX') == '1' and count == 2: sys.exit(1)
    sys.exit(0)
if name == 'systemctl':
    if os.environ.get('FAIL_RESTORE') == '1' and ':18080;' in Path(os.environ['UPSTREAM_FILE']).read_text():
        sys.exit(1)
    if os.environ.get('FAIL_RELOAD') == '1' and not (root / 'reload-failed').exists():
        (root / 'reload-failed').touch(); sys.exit(1)
    (root / 'live-upstream').write_text(Path(os.environ['UPSTREAM_FILE']).read_text())
    sys.exit(0)
if name == 'docker':
    if args[:2] == ['network', 'inspect']: sys.exit(0)
    if args[0] == 'pull': sys.exit(0)
    if args[:2] == ['image', 'inspect']:
        print(os.environ.get('IMAGE_LABEL', os.environ['TEST_REVISION'])); sys.exit(0)
    if args[:2] == ['rm', '-f']:
        state['containers'].pop(args[2], None); save(); sys.exit(0)
    if args[0] == 'run':
        container = args[args.index('--name') + 1]
        port = args[args.index('-p') + 1].split(':')[1]
        state['containers'][container] = {'port': port, 'revision': os.environ['TEST_REVISION']}
        state['last_run'] = args
        save(); print('candidate-id'); sys.exit(0)
    sys.exit(2)
if name == 'curl':
    url = args[-1]
    public = url.startswith('https:')
    if public:
        port = re.search(r':(\d+);', (root / 'live-upstream').read_text()).group(1)
    else:
        port = re.search(r':(\d+)/', url).group(1)
    container = next((c for c in state['containers'].values() if c['port'] == port), None)
    revision = container['revision'] if container else 'missing'
    failed = ((not public and os.environ.get('FAIL_CANDIDATE') == '1') or
              (public and os.environ.get('FAIL_PUBLIC') == '1' and revision == os.environ['TEST_REVISION']))
    code = '503' if failed or not container else os.environ.get('PROBE_CODE', '200')
    Path(args[args.index('-o') + 1]).write_text(json.dumps({'status': 'UP', 'revision': revision}))
    print(code, end=''); sys.exit(0)
sys.exit(2)
'''


class EnvironmentTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / '.env'
        self.values = dict(DB_HOST='mysql', DB_PORT='3306', DB_NAME='brightnest', DB_USER='app',
                           DB_PASS='test-only', JWT_SECRET='x' * 64, REDIS_HOST='redis', REDIS_PORT='6379',
                           RESUME_STORAGE_S3_BUCKET='test-bucket', RESUME_STORAGE_S3_REGION='ap-south-1')

    def write(self):
        self.path.write_text(''.join(f'{k}={v}\n' for k, v in self.values.items()))
        self.path.chmod(0o600)

    def test_valid_without_admin_bootstrap(self):
        self.write()
        self.assertEqual(release_env.validate(self.path)['DB_NAME'], 'brightnest')

    def test_missing_loopback_and_unsafe_overrides_are_rejected(self):
        for key, value in [('JWT_SECRET', ''), ('REDIS_HOST', 'localhost'), ('DB_HOST', '127.0.0.1'),
                           ('SPRING_DATASOURCE_URL', 'override'), ('PORT', '9090'),
                           ('RESUME_STORAGE_S3_ENABLED', 'false'), ('ADMIN_EMAIL', 'unpaired@example.com')]:
            with self.subTest(key=key):
                old = self.values.copy()
                self.values[key] = value
                self.write()
                with self.assertRaises(ValueError): release_env.validate(self.path)
                self.values = old

    def test_group_readable_file_is_rejected(self):
        self.write(); self.path.chmod(0o640)
        with self.assertRaises(ValueError): release_env.validate(self.path)


class ReleaseTransactionTests(EnvironmentTests):
    def setUp(self):
        super().setUp()
        self.root = Path(self.tmp.name)
        self.write()
        self.upstream = self.root / 'active-backend.conf'
        self.upstream.write_text('server 127.0.0.1:18080;\n')
        (self.root / 'live-upstream').write_text(self.upstream.read_text())
        self.rev_a = 'a' * 40
        self.image_a = 'ghcr.io/example/app@sha256:' + 'a' * 64
        (self.root / 'active-release').write_text(f'blue {self.image_a} {self.rev_a} 18080\n')
        (self.root / 'docker.json').write_text(json.dumps({'containers': {
            'brightnest-blue': {'port': '18080', 'revision': self.rev_a}}}))
        bin_dir = self.root / 'bin'; bin_dir.mkdir()
        executable = bin_dir / 'mock'; executable.write_text(MOCK); executable.chmod(0o755)
        for name in ('sudo', 'nginx', 'systemctl', 'docker', 'curl', 'sleep'):
            (bin_dir / name).symlink_to(executable)
        self.env = os.environ | dict(PATH=str(bin_dir) + ':' + os.environ['PATH'], FAKE_ROOT=str(self.root),
            RELEASE_DIR=str(self.root), ENV_FILE=str(self.path), UPSTREAM_FILE=str(self.upstream),
            PUBLIC_URL='https://staging.example.com', HEALTH_ATTEMPTS='1', HEALTH_INTERVAL='0')

    def run_release(self, revision='b' * 40, mode='deploy', **overrides):
        self.env['TEST_REVISION'] = revision
        image = 'ghcr.io/example/app@sha256:' + revision[0] * 64
        args = ['bash', str(SCRIPTS / 'release-blue-green.sh'), mode]
        if mode == 'deploy': args += [image, revision]
        return subprocess.run(args, env=self.env | overrides, capture_output=True, text=True)

    def docker(self): return json.loads((self.root / 'docker.json').read_text())

    def test_two_deployments_and_forced_rollback(self):
        first = self.run_release()
        self.assertEqual(first.returncode, 0, first.stderr)
        args = self.docker()['last_run']
        self.assertIn('127.0.0.1:18081:8080', args)
        self.assertIn('--env-file', args)
        self.assertIn('--read-only', args)
        self.assertIn('brightnest-blue', self.docker()['containers'])
        second = self.run_release('c' * 40)
        self.assertEqual(second.returncode, 0, second.stderr)
        self.assertTrue((self.root / 'active-release').read_text().startswith('blue '))
        rollback = self.run_release('c' * 40, mode='rollback')
        self.assertEqual(rollback.returncode, 0, rollback.stderr)
        self.assertIn('b' * 40, (self.root / 'active-release').read_text())
        self.assertEqual(len(self.docker()['containers']), 2)

    def test_candidate_nginx_reload_and_public_failures_restore_serving_release(self):
        for fault in ('FAIL_CANDIDATE', 'FAIL_NGINX', 'FAIL_RELOAD', 'FAIL_PUBLIC'):
            with self.subTest(fault=fault):
                # Clear per-run mock counters; every failure should return to the same baseline.
                for name in ('nginx-count', 'reload-failed'):
                    (self.root / name).unlink(missing_ok=True)
                result = self.run_release(**{fault: '1'})
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(self.rev_a, (self.root / 'active-release').read_text())
                self.assertEqual(self.upstream.read_text(), 'server 127.0.0.1:18080;\n')
                self.assertEqual(list(self.docker()['containers']), ['brightnest-blue'])

    def test_unverified_recovery_retains_both_containers_and_evidence(self):
        result = self.run_release(FAIL_PUBLIC='1', FAIL_RESTORE='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(len(self.docker()['containers']), 2)
        self.assertTrue(list(self.root.glob('release.*[0-9A-Za-z]')))
        self.assertIn('rollback verification failed', result.stderr)

    def test_release_lock_rejects_overlapping_operation(self):
        with (self.root / 'release.lock').open('w') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            result = self.run_release()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.docker()['containers']), ['brightnest-blue'])

    def test_mismatched_image_identity_is_rejected_before_container_changes(self):
        result = self.run_release(IMAGE_LABEL='wrong')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.docker()['containers']), ['brightnest-blue'])

    def test_redirect_is_not_accepted_as_readiness(self):
        result = self.run_release(PROBE_CODE='302')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.docker()['containers']), ['brightnest-blue'])

    def test_inconsistent_upstream_state_aborts_without_deleting_container(self):
        self.upstream.write_text('server 127.0.0.1:18081;\n')
        result = self.run_release()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(self.docker()['containers']), ['brightnest-blue'])


if __name__ == '__main__': unittest.main()
