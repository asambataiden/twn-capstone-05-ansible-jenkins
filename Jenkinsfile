
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    environment {
        // DigitalOcean Ansible controller
        ANSIBLE_HOST = '46.101.229.169'
        ANSIBLE_USER = 'root'

        // Versioned release configuration
        RELEASE_ROOT = '/root/twn-capstone-05/releases'
        ANSIBLE_PLAYBOOK = 'deploy-application.yml'

        // One AWS EC2 target for the first deployment
        TARGET_HOST = 'ec2-16-171-181-122.eu-north-1.compute.amazonaws.com'

        // Existing credentials and verified SSH host keys
        EC2_SSH_USER = 'ec2-user'
        EC2_PRIVATE_KEY = '/root/ansible-jenkins-key-pair.pem'
        EC2_KNOWN_HOSTS = '/root/.ssh/known_hosts_capstone05'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm

                sh '''
                    set -eu

                    echo "=== Checked-out Git revision ==="
                    git rev-parse HEAD

                    echo "=== Latest commit ==="
                    git log -1 --format='%s'
                '''
            }
        }

        stage('Validate Source') {
            steps {
                sh '''
                    set -eu

                    echo "=== Validate Git-controlled source ==="

                    test -f Jenkinsfile
                    test -f ansible/deploy-docker-new-user.yaml
                    test -f ansible/deploy-application.yml
                    test -f ansible/inventory_aws_ec2.yaml
                    test -f ansible/ec2-docker-vars.yaml

                    for role in \
                        linux_baseline \
                        docker_storage \
                        docker_engine \
                        linux_users \
                        docker_compose \
                        docker_registry \
                        application_deploy \
                        application_verify
                    do
                        test -f "ansible/roles/$role/tasks/main.yml"
                    done

                    # The Vault file stays on the Ansible controller.
                    test ! -e ansible/secrets.vault.yml

                    echo "Source validation passed."
                '''
            }
        }

        stage('Verify Ansible Controller') {
            steps {
                sshagent(credentials: ['ansible-server-key']) {
                    sh '''
                        set -eu

                        echo "=== Verify Jenkins to Ansible SSH ==="

                        ssh \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=yes \
                          -o ConnectTimeout=10 \
                          "$ANSIBLE_USER@$ANSIBLE_HOST" \
                          'hostname && ansible --version | head -n 1'
                    '''
                }
            }
        }

        stage('Transfer Versioned Ansible Release') {
            steps {
                sshagent(credentials: ['ansible-server-key']) {
                    sh '''
                        set -eu

                        REVISION="$(git rev-parse --verify HEAD)"
                        RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                        ARCHIVE="$(mktemp)"
                        trap 'rm -f "$ARCHIVE"' EXIT

                        echo "=== Package Git revision ==="
                        echo "Revision: $REVISION"

                        git archive --format=tar HEAD \
                          ansible/deploy-docker-new-user.yaml \
                          ansible/deploy-application.yml \
                          ansible/inventory_aws_ec2.yaml \
                          ansible/ec2-docker-vars.yaml \
                          ansible/roles \
                          > "$ARCHIVE"

                        echo "=== Transfer versioned release ==="

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
                             > '$RELEASE_DIR/REVISION';

                           test -f \
                             '$RELEASE_DIR/ansible/deploy-application.yml';

                           echo 'Versioned release transferred.'" \
                          < "$ARCHIVE"

                        echo "Release transfer passed."
                    '''
                }
            }
        }

        stage('Validate Versioned Ansible Release') {
            steps {
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

                            echo "=== Ansible syntax check ==="

                            printf '%s\\n' "$VAULT_PASSWORD" |
                              ssh \
                                -T \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=yes \
                                -o ConnectTimeout=10 \
                                "$ANSIBLE_USER@$ANSIBLE_HOST" \
                                "set -eu;
                                 cd '$RELEASE_DIR/ansible';

                                 export ANSIBLE_HOST_KEY_CHECKING=True;
                                 export ANSIBLE_PRIVATE_KEY_FILE='$EC2_PRIVATE_KEY';
                                 export ANSIBLE_REMOTE_USER='$EC2_SSH_USER';
                                 export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                                 ansible-playbook \
                                   -i inventory_aws_ec2.yaml \
                                   '$ANSIBLE_PLAYBOOK' \
                                   --syntax-check \
                                   --vault-password-file /dev/stdin"

                            echo "Ansible syntax validation passed."
                        '''
                    }
                }
            }
        }

        stage('Verify EC2 Connectivity') {
            steps {
                sshagent(credentials: ['ansible-server-key']) {
                    sh '''
                        set -eu

                        REVISION="$(git rev-parse --verify HEAD)"
                        RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                        echo "=== Verify AWS EC2 authentication ==="

                        ssh \
                          -T \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=yes \
                          -o ConnectTimeout=10 \
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

                        echo "AWS EC2 connectivity passed."
                    '''
                }
            }
        }

        stage('Approve EC2 Deployment') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input(
                        message: 'Deploy TWN Java application to EC2 instance i-0e83d63c88f0811bf?',
                        ok: 'Approve Deployment'
                    )
                }
            }
        }

        stage('Deploy Application to EC2') {
            steps {
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

                            echo "=== Approved deployment ==="
                            echo "Revision: $REVISION"
                            echo "Target: $TARGET_HOST"

                            printf '%s\\n' "$VAULT_PASSWORD" |
                              ssh \
                                -T \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=yes \
                                -o ConnectTimeout=10 \
                                "$ANSIBLE_USER@$ANSIBLE_HOST" \
                                "set -eu;
                                 cd '$RELEASE_DIR/ansible';

                                 export ANSIBLE_HOST_KEY_CHECKING=True;
                                 export ANSIBLE_PRIVATE_KEY_FILE='$EC2_PRIVATE_KEY';
                                 export ANSIBLE_REMOTE_USER='$EC2_SSH_USER';
                                 export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                                 ansible-playbook \
                                   -i inventory_aws_ec2.yaml \
                                   '$ANSIBLE_PLAYBOOK' \
                                   --limit '$TARGET_HOST' \
                                   --vault-password-file /dev/stdin"

                            echo "Ansible deployment finished."
                        '''
                    }
                }
            }
        }

        stage('Verify Application HTTP') {
            steps {
                sshagent(credentials: ['ansible-server-key']) {
                    sh '''
                        set -eu

                        REVISION="$(git rev-parse --verify HEAD)"
                        RELEASE_DIR="$RELEASE_ROOT/${REVISION}-${BUILD_NUMBER}"

                        echo "=== Check Java application HTTP status ==="

                        ssh \
                          -T \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=yes \
                          -o ConnectTimeout=10 \
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

                        echo "Application HTTP verification passed."
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Capstone 05 Jenkins-Ansible deployment verified.'
        }

        failure {
            echo 'FAILURE: Review the failed Jenkins stage.'
        }

        aborted {
            echo 'ABORTED: Pipeline stopped or deployment not approved.'
        }

        always {
            echo "Build: ${env.BUILD_NUMBER}"
            echo "Job: ${env.JOB_NAME}"
            echo 'TWN Capstone 05 pipeline finished.'
        }
    }
}
