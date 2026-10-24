# Security and trust boundaries — Capstone 05

This is an educational DevOps deployment pipeline, with security controls appropriate to its tested scope. **It is not a production security certification.**

## Authentication and secrets

| Credential/material | Stored in | Usage |
| --- | --- | --- |
| GitHub checkout credential | Jenkins Credentials | Read private GitHub repository |
| `ansible-server-key` | Jenkins Credentials | SSH from Jenkins to remote controller |
| `ansible-vault-password` | Jenkins Credentials (secret text) | Decrypt controller-side Ansible Vault |
| EC2 SSH private key | Ansible controller: `/root/ansible-jenkins-key-pair.pem` | SSH from controller to AWS EC2 |
| Encrypted Vault | Ansible controller: `/root/secrets.vault.yml` | Application/database secret variables |
| Known host keys | Ansible controller: `/root/.ssh/known_hosts_capstone05` | EC2 server identity verification |

Do not commit secrets, upload decrypted Vault values or paste raw Jenkins credential output. Git archive transfers only selected versioned playbooks, inventory, variables and roles.

## Network access

- Jenkins uses strict SSH host-key verification to the controller.
- Ansible to EC2 enforces `StrictHostKeyChecking=yes` and uses independently verified host identities.
- Shared AWS EC2 security group `sg-0ce8f337a182c2c9b` was verified with SSH inbound `46.101.229.169/32` only, and both instances returned `pong` afterward.
- The example application listens on host port 8080, phpMyAdmin on 8081, and standalone MySQL on 3306. These were not open in the inspected AWS security-group inbound rules. Verify any additional routing/firewalls independently.

## Change safety

- Only the selected EC2 instance is targeted for application deployment using `--limit`.
- The redeployment playbook checks the existing Compose application and known database volume/container before running roles.
- The application role can reconcile/recreate Docker containers. The workflow does **not** provide database backup, automatic rollback or production change approval.
- Automatic deployment requires trusted Jenkins job permissions and trusted Git branches.

## Known risks / deferred improvements

1. Legacy public training repository previously contained a Docker Hub credential: **rotate/revoke it** if not already done. Never treat deletion of Git content as revocation.
2. Jenkins lab executes jobs on its controller and has Docker socket exposure; remove/segregate these privileges before production reuse.
3. `mysql:5.7` is end-of-life; upgrading requires a separately planned data migration.
4. Docker Compose download checksum pinning remains incomplete in the bootcamp configuration. Verify integrity before production use.
5. Publicly bound application/phpMyAdmin listeners are acceptable only behind appropriately verified network controls.
6. Vault password passing via command stdin is scoped to the lab implementation. Avoid CLI flags or build logs that reveal it.

## Out of scope

Terraform or other IaC conversion, zero-trust rollout, dedicated Jenkins agents, backup systems, comprehensive vulnerability scanning and production HA are not Capstone 05 deliverables.
