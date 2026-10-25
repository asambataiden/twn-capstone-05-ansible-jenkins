# TWN Capstone 05 — Jenkins Build #8: two EC2 hosts verified

**Date:** 2026-10-25  
**Jenkins job:** `TWN-Capstone-05-Ansible-Jenkins`  
**Build:** `#8` — **Finished: SUCCESS**  
**Git branch:** `feat/cap05-two-ec2-jenkins-targets`  
**Checked-out Git revision:** `c8885feaa0afdf669271b35f70870412adff3ff1`  
**Deployment controller:** existing DigitalOcean Ansible control node  
**AWS region:** `eu-north-1`

## Observed Jenkins execution

The supplied Jenkins console log confirms the stages:
1. Source checkout of the two-target feature branch and file checks.
2. Secure SSH communication to the Ansible controller (Ansible core 2.16.3).
3. Transfer of Git revision-specific Ansible release `c8885feaa0afdf669271b35f70870412adff3ff1-8`.
4. Vault-backed Ansible syntax check succeeded.
5. Ansible `ping` from the controller to **both** EC2 hosts succeeded (`pong`).
6. Ansible `deploy-application.yml` ran against **both** hosts; the application directory, existing MySQL volume/container, environment file, Compose configuration, running services and HTTP health were checked.
7. Independent final Ansible `uri` health check returned **HTTP 200** (`OK (215 bytes)`) from `127.0.0.1:8080` on each host.
8. Jenkins completed: `Finished: SUCCESS`.

## Deployment recap — literal excerpts

```text
PLAY RECAP
ec2-16-171-181-122.eu-north-1.compute.amazonaws.com : ok=13 changed=0 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
ec2-56-228-35-22.eu-north-1.compute.amazonaws.com : ok=13 changed=0 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

Both hosts reported `Running services: java-app, mysql, phpmyadmin`.

```text
Deployment successful on ec2-56-228-35-22.eu-north-1.compute.amazonaws.com. Application HTTP status: 200.
Deployment successful on ec2-16-171-181-122.eu-north-1.compute.amazonaws.com. Application HTTP status: 200.
Application HTTP 200 verified.
SUCCESS: Jenkins-Ansible deployment verified.
Finished: SUCCESS
```

## What this proves (and does not)

**Proven:** Jenkins checked out the intended feature branch, transferred a versioned release to a separate Ansible controller and triggered a successful Ansible reconciliation/verification of the existing Docker Compose Java application on **two EC2 instances**. Both hosts were reachable and both returned HTTP 200. This aligns the exercise with TWN's two-managed-instance requirement.

**Important qualification:** `changed=0` indicates no changes were required to reach the desired state during Build #8: the application was already deployed. This demonstrates an idempotent reconciliation run, **not** a fresh installation of both hosts, database migration, or a newly built image.

**Security:** The password came through Jenkins protected credentials and Ansible Vault; SSH host-key checking stayed enabled. Do not commit raw logs containing secrets or credentials; this document retains only sanitized excerpts and source-level hostnames.

**Evidence source:** user-supplied Jenkins Build #8 full console output in the conversation, 2026-10-25. It has not been uploaded verbatim into GitHub.

## TWN badge description — draft

I integrated Jenkins and Ansible to manage a Docker Compose-based Java application on two AWS EC2 instances through a separate Ansible controller. Jenkins checks out version-controlled playbooks, transfers a revision-specific release to the controller, validates its syntax using Ansible Vault credentials, verifies SSH connectivity with strict host-key checking, runs the application deployment playbook on both EC2 hosts, and independently checks HTTP health. In Jenkins Build #8, the Ansible play completed successfully on both hosts with `failed=0` and `unreachable=0`, and both applications returned HTTP 200. Since the configuration was already current, the play was idempotent (`changed=0`). I learned Jenkins pipeline integration, AWS EC2 dynamic inventory, Ansible roles and Vault, secure SSH, Docker Compose operations, infrastructure idempotency, and evidence-based deployment verification.
