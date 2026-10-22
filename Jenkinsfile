
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

        // Git-controlled Ansible releases
        RELEASE_ROOT = '/root/twn-capstone-05/releases'
        ANSIBLE_PLAYBOOK = 'deploy-application.yml'

        // Restrict deployment to one AWS EC2 instance
        TARGET_HOST = 'ec2-16-171-181-122.eu-north-1.compute.amazonaws.com'

        // Verified EC2 SSH host keys
        EC2_KNOWN_HOSTS = '/root/.ssh/known_hosts_capstone05'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm

                sh '''
                    set -eu

                    echo "=== Git Revision ==="
                    git rev-parse HEAD

                    echo "=== Git Branch ==="
                    git log -1 --format="%s"
                '''
            }
        }

        stage('Validate Source') {
            steps {
                sh '''
                    set -eu

                    echo "=== Validating Ansible project ==="

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

                    # Vault credentials must not be stored in Git
                    test ! -e ansible/secrets.vault.yml

                    echo "Ansible project structure verified."
                '''
            }
        }

        stage('Verify Ansible Controller') {
            steps {
                sshagent(credentials: ['ansible-server-key']) {
                    sh '''
                        set -eu

                        echo "=== Testing Ansible controller connectivity ==="

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

                        echo "=== Packaging Git-controlled release ==="
                        echo "Revision: $REVISION"

                        git archive --format=tar HEAD \
                          ansible/deploy-docker-new-user.yaml \
                          ansible/deploy-application.yml \
                          ansible/inventory_aws_ec2.yaml \
                          ansible/ec2-docker-vars.yaml \
                          ansible/roles \
                          > "$ARCHIVE"

                        echo "=== Transferring release to Ansible controller ==="

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
                             > '$RELEASE_DIR/REVISION';

                           test -f \
                             '$RELEASE_DIR/ansible/deploy-application.yml';

                           echo 'Versioned Ansible release transferred.'" \
                          < "$ARCHIVE"

                        echo "Git revision transferred: $REVISION"
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

                            echo "=== Running Ansible syntax validation ==="

                            printf '%s\\n' "$VAULT_PASSWORD" |
                              ssh \
                                -T \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=yes \
                                -o ConnectTimeout=10 \
                                "$ANSIBLE_USER@$ANSIBLE_HOST" \
                                "set -eu;
                                 cd '$RELEASE_DIR/ansible';

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

                        echo "=== Testing strict SSH connectivity to AWS EC2 ==="

                        ssh \
                          -T \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=yes \
                          -o ConnectTimeout=10 \
                          "$ANSIBLE_USER@$ANSIBLE_HOST" \
                          "set -eu;
                           cd '$RELEASE_DIR/ansible';

                           test -f '$EC2_KNOWN_HOSTS';

                           export ANSIBLE_HOST_KEY_CHECKING=True;

                           export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                           ansible ec2_docker_server \
                             -i inventory_aws_ec2.yaml \
                             --limit '$TARGET_HOST' \
                             -m ansible.builtin.ping"

                        echo "EC2 SSH connectivity verified."
                    '''
                }
            }
        }

        stage('Approve EC2 Deployment') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input(
                        message: 'Deploy the TWN Java application to the selected AWS EC2 instance?',
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

                            echo "=== Starting approved Ansible deployment ==="
                            echo "Git revision: $REVISION"
                            echo "EC2 target: $TARGET_HOST"

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

                                 export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                                 ansible-playbook \
                                   -i inventory_aws_ec2.yaml \
                                   '$ANSIBLE_PLAYBOOK' \
                                   --limit '$TARGET_HOST' \
                                   --vault-password-file /dev/stdin"

                            echo "Ansible deployment completed."
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

                        echo "=== Checking Java application HTTP status ==="

                        ssh \
                          -T \
                          -o BatchMode=yes \
                          -o StrictHostKeyChecking=yes \
                          -o ConnectTimeout=10 \
                          "$ANSIBLE_USER@$ANSIBLE_HOST" \
                          "set -eu;
                           cd '$RELEASE_DIR/ansible';

                           export ANSIBLE_HOST_KEY_CHECKING=True;

                           export ANSIBLE_SSH_ARGS='-o StrictHostKeyChecking=yes -o UserKnownHostsFile=$EC2_KNOWN_HOSTS';

                           ansible ec2_docker_server \
                             -i inventory_aws_ec2.yaml \
                             --limit '$TARGET_HOST' \
                             -m ansible.builtin.uri \
                             -a 'url=http://127.0.0.1:8080/ method=GET status_code=200 return_content=false timeout=10'"

                        echo "Java application HTTP verification passed."
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Capstone 05 Jenkins-Ansible pipeline completed.'
        }

        failure {
            echo 'FAILURE: Review the Jenkins stage logs.'
        }

        aborted {
            echo 'ABORTED: Deployment was not approved or the pipeline was stopped.'
        }

        always {
            echo "Build: ${env.BUILD_NUMBER}"
            echo "Job: ${env.JOB_NAME}"
            echo 'TWN Capstone 05 pipeline finished.'
        }
    }
}
