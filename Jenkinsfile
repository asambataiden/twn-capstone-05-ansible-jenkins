
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    environment {
        ANSIBLE_HOST = '46.101.229.169'
        ANSIBLE_USER = 'root'
        RELEASE_ROOT = '/root/twn-capstone-05/releases'
        ANSIBLE_PLAYBOOK = 'deploy-docker-new-user.yaml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                sh '''
                    set -eu
                    echo "Checked-out Git revision:"
                    git rev-parse HEAD
                '''
            }
        }

        stage('Validate Source') {
            steps {
                sh '''
                    set -eu

                    test -f ansible/deploy-docker-new-user.yaml
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

                    test ! -e ansible/secrets.vault.yml

                    echo "Required deployment source files verified."
                '''
            }
        }

        stage('Verify Ansible Controller') {
            steps {
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

                        git archive --format=tar HEAD \
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
                           printf '%s\\\\n' '$REVISION' \
                             > '$RELEASE_DIR/REVISION'" \
                          < "$ARCHIVE"

                        echo "Transferred Git revision: $REVISION"
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

                            printf '%s\\n' "$VAULT_PASSWORD" |
                              ssh \
                                -T \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=yes \
                                -o ConnectTimeout=10 \
                                "$ANSIBLE_USER@$ANSIBLE_HOST" \
                                "cd '$RELEASE_DIR/ansible' &&
                                 ansible-playbook \
                                   -i inventory_aws_ec2.yaml \
                                   '$ANSIBLE_PLAYBOOK' \
                                   --syntax-check \
                                   --vault-password-file /dev/stdin"
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Versioned Jenkins-to-Ansible validation passed.'
        }

        failure {
            echo 'Pipeline failed; EC2 deployment was not attempted.'
        }

        always {
            echo 'Capstone 05 validation pipeline finished.'
        }
    }
}
