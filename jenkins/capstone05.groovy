def runPipeline() {

    withEnv([
        'ANSIBLE_HOST=46.101.229.169',
        'ANSIBLE_USER=root',
        'RELEASE_ROOT=/root/twn-capstone-05/releases',
        'TARGET_HOST=ec2-16-171-181-122.eu-north-1.compute.amazonaws.com',
        'EC2_PRIVATE_KEY=/root/ansible-jenkins-key-pair.pem',
        'EC2_KNOWN_HOSTS=/root/.ssh/known_hosts_capstone05',
        'EC2_SSH_USER=ec2-user'
    ]) {

    stage('Validate Source') {
    sh '''
                set -eu

                test -f ansible/deploy-application.yml
                test -f ansible/deploy-docker-new-user.yaml
                test -f ansible/inventory_aws_ec2.yaml
                test -f ansible/ec2-docker-vars.yaml
                test ! -e ansible/secrets.vault.yml

                for role in \
                    linux_baseline docker_storage docker_engine \
                    linux_users docker_compose docker_registry \
                    application_deploy application_verify
                do
                    test -f "ansible/roles/$role/tasks/main.yml"
                done

                echo "Source validation passed."
            '''
}

stage('Verify Ansible Controller') {
    sshagent(credentials: ['ansible-server-key']) {
    sh '''
                    set -eu

                    ssh \
                      -o BatchMode=yes \
                      -o StrictHostKeyChecking=yes \
                      -o ConnectTimeout=10 \
                      "$ANSIBLE_USER@$ANSIBLE_HOST" \
                      'hostname && ansible --version | head -n 1'
                '''
}
}

stage('Transfer Versioned Release') {
    sshagent(credentials: ['ansible-server-key']) {
    sh '''
                    set -eu

                    REVISION="$(git rev-parse --verify HEAD)"
                    RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                    ARCHIVE="$(mktemp)"
                    trap 'rm -f "$ARCHIVE"' EXIT

                    git archive --format=tar HEAD \
                      ansible/deploy-application.yml \
                      ansible/deploy-docker-new-user.yaml \
                      ansible/inventory_aws_ec2.yaml \
                      ansible/ec2-docker-vars.yaml \
                      ansible/roles \
                      > "$ARCHIVE"

                    ssh \
                      -T \
                      -o BatchMode=yes \
                      -o StrictHostKeyChecking=yes \
                      -o ConnectTimeout=10 \
                      "$ANSIBLE_USER@$ANSIBLE_HOST" \
                      "set -eu;
                       umask 077;
                       install -d -m 0700 '$RELEASE_ROOT';
                       test ! -e '$RELEASE_DIR';
                       install -d -m 0700 '$RELEASE_DIR';
                       tar -xf - -C '$RELEASE_DIR';
                       test -f /root/secrets.vault.yml;
                       ln -s /root/secrets.vault.yml \
                         '$RELEASE_DIR/ansible/secrets.vault.yml';
                       printf '%s\\n' '$REVISION' \
                         > '$RELEASE_DIR/REVISION'" \
                      < "$ARCHIVE"

                    echo "Transferred revision: $REVISION"
                '''
}
}

stage('Validate Ansible Release') {
    sshagent(credentials: ['ansible-server-key']) {
    withCredentials([
        string(
            credentialsId: 'ansible-vault-password',
    variable: 'VAULT_PASSWORD'
)
]) {
    sh '''
                        set +x
                        set -eu

                        REVISION="$(git rev-parse --verify HEAD)"
                        RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                        printf '%s\\n' "$VAULT_PASSWORD" |
                          ssh \
                            -T \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            "$ANSIBLE_USER@$ANSIBLE_HOST" \
                            "set -eu;
                             cd '$RELEASE_DIR/ansible';
                             ansible-playbook \
                               -i inventory_aws_ec2.yaml \
                               deploy-application.yml \
                               --syntax-check \
                               --vault-password-file /dev/stdin"

                        echo "Ansible syntax check passed."
                    '''
}
}
}

stage('Verify EC2 Connectivity') {
    sshagent(credentials: ['ansible-server-key']) {
    sh '''
                    set -eu

                    REVISION="$(git rev-parse --verify HEAD)"
                    RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                    ssh \
                      -T \
                      -o BatchMode=yes \
                      -o StrictHostKeyChecking=yes \
                      "$ANSIBLE_USER@$ANSIBLE_HOST" \
                      "set -eu;
                       cd '$RELEASE_DIR/ansible';

                       test -r '$EC2_PRIVATE_KEY';
                       test -f '$EC2_KNOWN_HOSTS';

                       export ANSIBLE_HOST_KEY_CHECKING=True;
                       export ANSIBLE_PRIVATE_KEY_FILE='$EC2_PRIVATE_KEY';
                       export ANSIBLE_REMOTE_USER='$EC2_SSH_USER';
                       export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                       ansible ec2_docker_server \
                         -i inventory_aws_ec2.yaml \
                         --limit '$TARGET_HOST' \
                         -m ansible.builtin.ping"

                    echo "EC2 connectivity passed."
                '''
}
}

stage('Deploy Application') {
    sshagent(credentials: ['ansible-server-key']) {
    withCredentials([
        string(
            credentialsId: 'ansible-vault-password',
    variable: 'VAULT_PASSWORD'
)
]) {
    sh '''
                        set +x
                        set -eu

                        REVISION="$(git rev-parse --verify HEAD)"
                        RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                        echo "Deploying revision: $REVISION"
                        echo "Target: $TARGET_HOST"

                        printf '%s\\n' "$VAULT_PASSWORD" |
                          ssh \
                            -T \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            "$ANSIBLE_USER@$ANSIBLE_HOST" \
                            "set -eu;
                             cd '$RELEASE_DIR/ansible';

                             export ANSIBLE_HOST_KEY_CHECKING=True;
                             export ANSIBLE_PRIVATE_KEY_FILE='$EC2_PRIVATE_KEY';
                             export ANSIBLE_REMOTE_USER='$EC2_SSH_USER';
                             export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                             ansible-playbook \
                               -i inventory_aws_ec2.yaml \
                               deploy-application.yml \
                               --limit '$TARGET_HOST' \
                               --vault-password-file /dev/stdin"

                        echo "Application deployment completed."
                    '''
}
}
}

stage('Verify Application HTTP') {
    sshagent(credentials: ['ansible-server-key']) {
    sh '''
                    set -eu

                    REVISION="$(git rev-parse --verify HEAD)"
                    RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                    ssh \
                      -T \
                      -o BatchMode=yes \
                      -o StrictHostKeyChecking=yes \
                      "$ANSIBLE_USER@$ANSIBLE_HOST" \
                      "set -eu;
                       cd '$RELEASE_DIR/ansible';

                       export ANSIBLE_HOST_KEY_CHECKING=True;
                       export ANSIBLE_PRIVATE_KEY_FILE='$EC2_PRIVATE_KEY';
                       export ANSIBLE_REMOTE_USER='$EC2_SSH_USER';
                       export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                       ansible ec2_docker_server \
                         -i inventory_aws_ec2.yaml \
                         --limit '$TARGET_HOST' \
                         -m ansible.builtin.uri \
                         -a 'url=http://127.0.0.1:8080/ method=GET status_code=200 return_content=false timeout=10'"

                    echo "Application HTTP 200 verified."
                '''
}
}
}
}

return this
