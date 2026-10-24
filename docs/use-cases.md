# Use cases — Capstone 05

## Actors and boundaries

- **Developer:** pushes a change to the configured Git feature branch and checks Jenkins output.
- **Jenkins:** orchestrates validation, release transfer, Ansible invocation and health checks.
- **Ansible controller:** performs discovery, authentication and execution against AWS EC2.
- **EC2 Docker host:** hosts the existing Java/MySQL/phpMyAdmin Compose stack.

## UC-01 — Trigger a Git-controlled deployment

**Preconditions:** Jenkins can check out the private repository; target branch is configured; required credentials exist.

**Main flow:**
1. Jenkins checks out a revision and validates expected source files.
2. Jenkins sends the Git archive over authenticated SSH to the Ansible controller.
3. Ansible validates the playbook and resolves the expected EC2 inventory group.
4. Ansible authenticates to the selected EC2 host using the controller's key and verified host key.
5. The application playbook reconciles Compose configuration and checks the running services.
6. The application answers HTTP 200 from EC2 loopback.
7. Jenkins reports `SUCCESS`.

**Alternative flows:** Source file absent, inventory has no matching host, SSH authentication fails, Vault decryption fails, role fails, or HTTP status is not 200. Jenkins stops and reports failure. The pipeline does not automatically roll back a failed deployment.

**Acceptance:** Correct Git revision, correct EC2 target, zero failed/unreachable hosts in full Ansible recap, and HTTP 200.

## UC-02 — Validate without deploying

**Purpose:** Catch source/inventory/SSH errors before application changes.

**Checks:** required files, encrypted Vault availability, Ansible syntax check, `ansible.builtin.ping`, expected inventory hostname.

**Acceptance:** All checks pass; no EC2 application changes before the deployment stage.

## UC-03 — Keep secrets out of Git

**Preconditions:** Jenkins credentials configured and encrypted Vault file is present on the Ansible controller.

**Main flow:** Git archive contains only versioned nonsecret files; Jenkins provides the Vault password through a masked credential; EC2 SSH private key and trusted host keys remain on controller.

**Acceptance:** No plaintext Vault, private SSH key, database passwords or active access tokens committed to the repository.

## UC-04 — Limit change blast radius

**Scenario:** Dynamic inventory discovers two EC2 instances but only the first is the intended deployment target.

**Main flow:** Jenkins passes `--limit ec2-16-171-181-122.eu-north-1.compute.amazonaws.com` to Ansible for ping, deployment, and verification.

**Acceptance:** The second EC2 instance is not a deployment target.

## UC-05 — Diagnose a failed pipeline

**Scenario:** A build fails while discovering the group or authenticating via SSH.

**Main flow:** Read failed stage output; compare inventory graph; check tagged grouping; ensure explicit EC2 SSH key, user, known_hosts; rerun a non-deploying connectivity check; commit any configuration correction.

**Acceptance:** Failures are diagnosable from stage output without exposing credentials.

## Non-goals

Business feature development, database backup/restore automation, infrastructure-as-code conversion, automatic rollback, production HA and additional client tenancy are excluded.
