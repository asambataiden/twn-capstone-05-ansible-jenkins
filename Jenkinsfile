
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 10, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    environment {
        ANSIBLE_HOST = '46.101.229.169'
        ANSIBLE_USER = 'root'
        ANSIBLE_PLAYBOOK = 'deploy-ansible-roles-docker-new-user.yaml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Project') {
            steps {
                sh '''
                    set -eu

                    test -f Jenkinsfile

                    echo "Jenkins pipeline source verified."
                '''
            }
        }

        stage('Verify Ansible SSH Connection') {
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

        stage('Validate Remote Ansible Playbook') {
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

                            printf '%s\\n' "$VAULT_PASSWORD" |
                              ssh \
                                -T \
                                -o BatchMode=yes \
                                -o StrictHostKeyChecking=yes \
                                -o ConnectTimeout=10 \
                                "$ANSIBLE_USER@$ANSIBLE_HOST" \
                                "cd /root && ansible-playbook \
                                  --syntax-check \
                                  --vault-password-file /dev/stdin \
                                  $ANSIBLE_PLAYBOOK"
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Jenkins-to-Ansible validation completed successfully.'
        }

        failure {
            echo 'Validation failed. Review the stage logs.'
        }

        always {
            echo 'Capstone 05 pipeline execution finished.'
        }
    }
}
