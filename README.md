
# TWN DevOps Bootcamp — Capstone 05

## Ansible Integration in Jenkins

This project demonstrates infrastructure automation and
application deployment using Jenkins and Ansible.

### Objectives

- Build a declarative Jenkins CI/CD pipeline.
- Validate Ansible inventory and playbook syntax.
- Implement idempotent Ansible deployment roles.
- Manage SSH authentication through Jenkins credentials.
- Deploy a sample application to an isolated Linux server.
- Verify deployment using HTTP health checks.
- Capture reproducible project evidence.

### Technology Stack

- Jenkins LTS
- Ansible
- Docker and Docker Compose
- Linux
- Git and GitHub
- SSH

### Project Status

Phase 01: Project initialization and controller validation.

### Security Principles

- No SSH private keys or passwords in Git.
- SSH host-key validation remains enabled.
- Least-privilege deployment credentials.
- No unrestricted Docker socket access.
- Infrastructure changes must be reproducible.
- Validate before deployment.

### Local Validation

```bash
ansible-playbook \
  -i ansible/inventory/local.ini \
  ansible/playbooks/verify-controller.yml \
  --syntax-check

ansible-playbook \
  -i ansible/inventory/local.ini \
  ansible/playbooks/verify-controller.yml
```

### Portfolio

GitHub account: asambataiden

Part of the TechWorld with Nana DevOps Bootcamp
Certified DevOps Practitioner portfolio.
