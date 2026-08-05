pipeline {
    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/11_105_AutoWired_Payment_Processing_System.git'
        BRANCH  = 'main'
    }

    stages {
        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }

        stage('Prepare Environment File') {
            steps {
                // Keep secrets out of source and avoid workspace write-permission issues.
                withCredentials([file(credentialsId: 'payment-processing-env', variable: 'ENV_FILE')]) {
                    script {
                        env.CI_ENV_FILE = sh(script: 'mktemp /tmp/payment-processing-env.XXXXXX', returnStdout: true).trim()
                        sh 'cp "$ENV_FILE" "$CI_ENV_FILE"'
                        sh 'chmod 600 "$CI_ENV_FILE"'
                    }
                }
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose --env-file "$CI_ENV_FILE" down || true'
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker-compose --env-file "$CI_ENV_FILE" build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose --env-file "$CI_ENV_FILE" up -d'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }

    post {
        always {
            sh 'if [ -n "$CI_ENV_FILE" ] && [ -f "$CI_ENV_FILE" ]; then rm -f "$CI_ENV_FILE"; fi'
        }
    }
}
