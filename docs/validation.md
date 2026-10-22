# Validation and evidence — Capstone 05

## Demonstrated result

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
- [ ] Archive sanitized full Build #7 console output, including deployment `PLAY RECAP`.
- [ ] Capture Jenkins stage-view screenshot for portfolio.
- [ ] Review and merge the Capstone 05 PR.

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
