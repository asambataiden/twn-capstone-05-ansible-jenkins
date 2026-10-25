# Validation and evidence — Capstone 05

## Jenkins Build #8 — two managed EC2 instances (verified)

On **2026-10-25**, Jenkins Build **#8** ran feature-branch revision `c8885feaa0afdf669271b35f70870412adff3ff1` against both EC2 instances. The Ansible `PLAY RECAP` reported `ok=13 changed=0 unreachable=0 failed=0` for **each** host. Final `ansible.builtin.uri` returned HTTP **200** for each, and Jenkins finished **SUCCESS**. The existing application was already in the desired state (`changed=0`); this was not a fresh install.

See [sanitized Build #8 evidence](../evidence/jenkins-build-08-two-ec2-2026-10-25.md) and the [Jenkins Build #8 Stage View screenshot](../evidence/screenshots/jenkins-build-08-stage-view-2026-10-25.jpg). The screenshot confirms all stages were successful; the console `PLAY RECAP` independently establishes the two-host results.

## Earlier Build #7 — one managed EC2 instance

| Field | Observed evidence |
| --- | --- |
| Jenkins job | `TWN-Capstone-05-Ansible-Jenkins` |
| Build | **#7** |
| Date | **22 October 2026** |
| Deployed/validated Git revision | `0e91d7aabd9cd1d6a79729cd5279dbe97b379e0f` |
| Target host | `ec2-16-171-181-122.eu-north-1.compute.amazonaws.com` |
| Final Ansible check | `ansible.builtin.uri`, `GET http://127.0.0.1:8080/` executed from EC2 |
| Observed status | **HTTP 200**, `changed: false`, `OK (215 bytes)` |
| Jenkins completion | **`Finished: SUCCESS`** |

The shared console excerpt directly verifies the final HTTP check and final pipeline outcome. The full **Deploy Application** stage's Ansible `PLAY RECAP` was not included, so individual task changes and database behavior are **not independently asserted** here.

## Capstone acceptance checklist

- [x] Jenkins can read the private Git repository.
- [x] Git-controlled Ansible files are transferred to a revision-specific release directory.
- [x] Ansible syntax check passes for the release.
- [x] Dynamic EC2 inventory identifies `ec2_docker_server`.
- [x] Strict SSH host verification and EC2 key authentication work.
- [x] Automated pipeline reaches final HTTP 200 check.
- [x] Jenkins Build #7 reports `SUCCESS`.
- [x] Archive sanitized **Build #8** two-host `PLAY RECAP` and final HTTP health results.
- [x] Capture and publish Jenkins Build #8 Stage View screenshot for portfolio.
- [x] Two-host Capstone 05 implementation PR #3 merged.

## How to collect evidence safely

1. Open Jenkins job `TWN-Capstone-05-Ansible-Jenkins` → Build `#7`.
2. Save the console log and a pipeline-stage screenshot **after redacting sensitive values**.
3. In the Deploy Application stage, look for `PLAY RECAP` with `failed=0` and `unreachable=0`. Do not claim this without seeing it.
4. Note the checkout Git revision and the HTTP verification status.
5. Keep raw internal host logs and secret-bearing outputs outside the repository.

## Previous debugging evidence

- **Build #4:** EC2 host discovery succeeded but inventory group was absent.
- **Build #5:** Group fixed; EC2 SSH authentication failed because the release didn't explicitly select the private key.
- **Build #7:** Jenkins and application HTTP validation reported success.

See [Pipeline](pipeline.md), [Operations](operations.md) and [Security](security.md).

## Two-host read-only preflight — 2026-10-25

The operator executed the existing successful Build #7 Ansible release inventory from the DigitalOcean controller. AWS dynamic inventory found **two** EC2 instances in `ec2_docker_server`:

- `ec2-56-228-35-22.eu-north-1.compute.amazonaws.com`
- `ec2-16-171-181-122.eu-north-1.compute.amazonaws.com`

With existing SSH known-hosts verification and credentials, **both** returned `ansible.builtin.ping` `pong` and `changed: false`. A read-only `ansible.builtin.uri` check on **both** returned `status: 200`, `msg: OK (215 bytes)`, and `changed: false` for `http://127.0.0.1:8080/`.

**Evidence boundary:** These checks establish existing application availability and controller-to-host connectivity; they do **not** demonstrate that the Jenkins job deployed to both hosts. The subsequent authorized Jenkins Build #8 confirmed two-host pipeline reconciliation and verification; see the evidence above. No new AWS infrastructure was provisioned.
