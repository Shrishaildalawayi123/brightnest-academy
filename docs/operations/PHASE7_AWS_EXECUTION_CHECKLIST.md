# Phase 7 AWS Security Hardening Execution Checklist

Use this as the operator worksheet for the cloud-side security hardening items in Phase 7.

## 0) Account Layout Inputs (fill once)

- [ ] AWS account ID: `____________________`
- [ ] Region: `____________________`
- [ ] Environment: `production`
- [ ] VPC ID: `____________________`
- [ ] Public ALB name: `____________________`
- [ ] Public ALB ARN: `____________________`
- [ ] ALB security group ID: `____________________`
- [ ] App security group ID: `____________________`
- [ ] DB security group ID: `____________________`
- [ ] App port: `8080`
- [ ] DB port: `3306`
- [ ] SSH admin CIDR(s): `____________________`
- [ ] Secrets prefix (example): `brightnest/prod`

## 1) Preparation

- [ ] AWS CLI v2 installed and authenticated (`aws sts get-caller-identity` works).
- [ ] Operator role has IAM permissions for WAFv2, EC2 security groups, CloudTrail, Config, IAM, S3, SNS, Secrets Manager, and Lambda.
- [ ] Change ticket ID and maintenance window recorded.

Optional shell variables for command reuse:

```bash
export AWS_REGION="us-east-1"
export ACCOUNT_ID="123456789012"
export ALB_ARN="arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/brightnest-prod-alb/abc123"
export ALB_SG_ID="sg-aaaaaaaa"
export APP_SG_ID="sg-bbbbbbbb"
export DB_SG_ID="sg-cccccccc"
export APP_PORT="8080"
export DB_PORT="3306"
```

## 2) WAF Hardening

### 2.1 Create and attach Web ACL

- [ ] Create Web ACL `brightnest-waf` in same region as ALB.
- [ ] Scope: `REGIONAL`.
- [ ] Associate with production ALB.

CLI verification:

```bash
aws wafv2 list-web-acls --scope REGIONAL --region "$AWS_REGION"
aws wafv2 list-resources-for-web-acl --web-acl-arn "<WEB_ACL_ARN>" --resource-type APPLICATION_LOAD_BALANCER --region "$AWS_REGION"
```

### 2.2 Add AWS managed rule groups

- [ ] `AWSManagedRulesCommonRuleSet`
- [ ] `AWSManagedRulesKnownBadInputsRuleSet`
- [ ] `AWSManagedRulesSQLiRuleSet`
- [ ] `AWSManagedRulesLinuxRuleSet`

- [ ] Start in `COUNT` mode for 24h if production risk is high.
- [ ] Move to `BLOCK` after false-positive review.

### 2.3 Add rate limit rules

- [ ] Global API throttle (example: 2000 requests/5 min/IP).
- [ ] Login endpoint throttle (example: 100 requests/5 min/IP on `/api/auth/login`).
- [ ] Optional geo controls only if business-approved.

### 2.4 WAF logging

- [ ] Enable WAF logging to CloudWatch Logs or Kinesis Firehose + S3.
- [ ] Retention policy set (minimum 30 days, recommend 90+).

Acceptance checks:

- [ ] WAF sampled requests visible.
- [ ] Login brute-force simulation is blocked.
- [ ] Legitimate login and course pages still pass.

## 3) Security Group Hardening

Target model:

- ALB SG inbound: `80/443` from internet (`0.0.0.0/0`, `::/0`), outbound only to app SG on app port.
- App SG inbound: app port from ALB SG only.
- App SG outbound: DB port to DB SG, plus `443` egress for required external services.
- DB SG inbound: DB port from app SG only.

### 3.1 Inventory and snapshot current SG rules

- [ ] Export pre-change SG rules for rollback.

```bash
aws ec2 describe-security-groups --group-ids "$ALB_SG_ID" "$APP_SG_ID" "$DB_SG_ID" --region "$AWS_REGION" > sg-before-phase7.json
```

### 3.2 Enforce ALB to app-only path

- [ ] Remove open app-port ingress from public CIDRs.
- [ ] Ensure app SG ingress is only source SG = ALB SG.

### 3.3 Lock down SSH

- [ ] Restrict `22/tcp` to approved admin CIDR list.
- [ ] Remove temporary broad admin access.

### 3.4 Verify connectivity

- [ ] ALB health checks remain healthy.
- [ ] Direct internet access to app port is blocked.

Acceptance checks:

- [ ] `describe-security-groups` output matches intended policy.
- [ ] External scan confirms app and DB ports are not public.

## 4) CloudTrail and AWS Config

### 4.1 CloudTrail

- [ ] Create/verify multi-region trail: `brightnest-audit-trail`.
- [ ] Management events: read + write enabled.
- [ ] Log file validation enabled.
- [ ] CloudWatch Logs integration enabled.
- [ ] S3 bucket has encryption and least-privilege bucket policy.

Verification:

```bash
aws cloudtrail describe-trails --region "$AWS_REGION"
aws cloudtrail get-trail-status --name "brightnest-audit-trail" --region "$AWS_REGION"
```

### 4.2 AWS Config

- [ ] Recorder enabled for all supported resource types.
- [ ] Delivery channel configured (encrypted S3 + SNS optional).
- [ ] Baseline managed rules enabled:
  - [ ] `encrypted-volumes`
  - [ ] `rds-snapshots-public-prohibited`
  - [ ] `s3-bucket-public-read-prohibited`
  - [ ] `vpc-sg-open-only-to-authorized-ports`

Verification:

```bash
aws configservice describe-configuration-recorders --region "$AWS_REGION"
aws configservice describe-delivery-channels --region "$AWS_REGION"
aws configservice describe-config-rules --region "$AWS_REGION"
```

Acceptance checks:

- [ ] New IAM/EC2/API actions are visible in CloudTrail events.
- [ ] Config rules are evaluating and returning compliance states.

## 5) Secrets Rotation Program

### 5.1 Inventory secrets

- [ ] DB credentials secret.
- [ ] JWT signing secret.
- [ ] Admin bootstrap credentials (if any).
- [ ] Third-party API keys used by production.

### 5.2 Enable/validate rotation

- [ ] DB credentials in Secrets Manager with automatic rotation (30 days).
- [ ] Rotation Lambda created and tested.
- [ ] App deploy process reloads rotated DB creds safely.

For non-database secrets (JWT/API keys):

- [ ] Define manual rotation SOP (for example every 60-90 days).
- [ ] Define dual-key overlap strategy for JWT if zero downtime is required.
- [ ] Define rollback key retention window.

CLI verification:

```bash
aws secretsmanager list-secrets --region "$AWS_REGION"
aws secretsmanager describe-secret --secret-id "<SECRET_ID>" --region "$AWS_REGION"
aws secretsmanager list-secret-version-ids --secret-id "<SECRET_ID>" --region "$AWS_REGION"
```

Acceptance checks:

- [ ] Rotation timestamp and next rotation are visible.
- [ ] Application still authenticates and reads DB after rotation.
- [ ] Old key behavior matches policy (invalidated or overlap).

## 6) Evidence Collection

Attach evidence for audit and incident-readiness:

- [ ] Screenshots or CLI output for WAF rules + association.
- [ ] `sg-before-phase7.json` and `sg-after-phase7.json`.
- [ ] CloudTrail trail status and recent event sample.
- [ ] AWS Config recorder/rule compliance export.
- [ ] Secrets rotation status and last successful rotation event.
- [ ] Post-change smoke test results (`/health`, login, core flows).

## 7) Rollback Plan (if regression appears)

- [ ] WAF: set suspected blocking rule to `COUNT` or disable temporarily.
- [ ] Security groups: restore from `sg-before-phase7.json` baseline.
- [ ] CloudTrail/Config: do not disable; fix permissions if delivery fails.
- [ ] Secrets: roll back to prior version only with documented approval.

## 8) Sign-off

- [ ] Security owner sign-off: `____________________`
- [ ] Platform owner sign-off: `____________________`
- [ ] Date/time completed (UTC): `____________________`
- [ ] Ticket/Change record: `____________________`
