
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Capstone 05 - Ansible Deployment') {
            steps {
                script {
                    def capstone = load 'jenkins/capstone05.groovy'
                    capstone.runPipeline()
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Jenkins-Ansible deployment verified.'
        }

        failure {
            echo 'FAILURE: Review the failed pipeline stage.'
        }

        always {
            echo "Build: ${env.BUILD_NUMBER}"
            echo "Job: ${env.JOB_NAME}"
        }
    }
}
