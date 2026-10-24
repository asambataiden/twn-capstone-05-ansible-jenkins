# Operations and troubleshooting — Capstone 05

## Routine workflow

1. Make changes on `feat/aws-ec2-ansible-roles` (or the configured trusted branch).
2. Review staged diff and run secret scanning before pushing.
3. Push the branch and run the `TWN-Capstone-05-Ansible-Jenkins` job.
4. Verify release transfer, Ansible connectivity, deploy stage, Ansible recap and final HTTP check.
5. Record Jenkins build number and Git revision.

**There is no manual approval stage.** Build execution can change the existing Compose application; treat repository pushes and build permissions accordingly.

## Non-deploying controller checks

Execute these **on the DigitalOcean Ansible controller**, not the Mac:

```bash
cd /root/twn-capstone-05/releases/<revision>-<build>/ansible

ansible-inventory -i inventory_aws_ec2.yaml --graph

ANSIBLE_HOST_KEY_CHECKING=True \
ANSIBLE_PRIVATE_KEY_FILE=/root/ansible-jenkins-key-pair.pem \
ANSIBLE_REMOTE_USER=ec2-user \
ANSIBLE_SSH_ARGS="-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=/root/.ssh/known_hosts_capstone05" \
ansible ec2_docker_server \
  -i inventory_aws_ec2.yaml \
  --limit ec2-16-171-181-122.eu-north-1.compute.amazonaws.com \
  -m ansible.builtin.ping
```

The `<revision>-<build>` segment is a placeholder; substitute an actual release directory.

## Failure reference

| Symptom | Investigation / fix |
| --- | --- |
| Inventory shows `aws_ec2` hosts but no `ec2_docker_server` | Check `Role=docker-server` tag and use `tags.Role` for installed `amazon.aws 7.2.0`. |
| `Permission denied (publickey,...)` | Confirm Ansible explicitly chooses the controller-side EC2 key and `ec2-user`; do not disable host-key checks. |
| Host key verification fails | Recheck trusted AWS console fingerprints; do not blindly accept changed host keys. |
| `config file = None` | Relative working directory does not load `/root/ansible.cfg` automatically; pipeline supplies connection settings explicitly. |
| `discovered_interpreter_python` warning | Informational interpreter discovery; pin only to a verified installed path if needed. |
| Syntax check passes, deployment fails | Syntax checks do not prove credentials, connectivity, Compose compatibility or runtime health. Inspect task logs and target limits. |
| HTTP status not 200 | Check Compose service status, Java container logs, and whether the app is listening at `127.0.0.1:8080`. |

## Recovery boundaries

This capstone does not implement automated rollback. If a build fails during deployment, stop further automatic runs, preserve the logs, identify the last known application/image/Compose configuration and restore deliberately after verifying any database impact. Never use `docker compose down -v` as a routine recovery step.

## Scope and housekeeping

The unrelated empty `jenkins/capstone05.py` file is a candidate for removal in a separate cleanup commit. A complete deployment recap and redacted screenshots should be archived as portfolio evidence rather than adding sensitive controller logs to Git.
