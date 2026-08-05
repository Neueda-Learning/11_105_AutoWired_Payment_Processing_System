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

        stage('Build Spring Boot') {
            steps {
                dir('server') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Client') {
            steps {
                dir('client') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                // Pulls the real .env (DB_NAME/DB_USERNAME/DB_PASSWORD) from a Jenkins
                // "Secret file" credential so it never lives in source control or the pipeline log.
                withCredentials([file(credentialsId: 'payment-processing-env', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env'
                    sh 'docker-compose up -d'
                }
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }
}
