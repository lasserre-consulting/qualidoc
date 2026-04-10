pipeline {
    agent any

    environment {
        PROJECT_DIR = '/home/ubuntu/projects/qualidoc'
        WWW_DIR     = '/var/www/qualidoc'
    }

    stages {

        stage('Checkout') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'git pull origin main'
                }
            }
        }

        stage('Frontend - Build') {
            steps {
                dir("${PROJECT_DIR}/qualidoc-frontend") {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        stage('Frontend - Deploy') {
            steps {
                sh '''
                    if [ -z "${WWW_DIR}" ] || [ ! -d "${WWW_DIR}" ]; then
                        echo "WWW_DIR invalide ou absent : ${WWW_DIR}"
                        exit 1
                    fi
                    rm -rf "${WWW_DIR:?}/"*
                '''
                sh "cp -r ${PROJECT_DIR}/qualidoc-frontend/dist/qualidoc-frontend/browser/* ${WWW_DIR}/"
            }
        }

        stage('Backend - Build & Deploy') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'docker compose --env-file /etc/qualidoc/secrets.env -f docker-compose.prod.yml up -d --build backend'
                }
            }
        }

    }

    post {
        success {
            echo 'qualidoc déployé avec succès.'
        }
        failure {
            echo 'Echec du pipeline qualidoc.'
        }
    }
}
