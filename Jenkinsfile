pipeline {
    agent any

    environment {
        APP_NAME='Secure-app'
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'jenkins-project-springboot-artifacts'
    }

    stages {

        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.9.11-eclipse-temurin-21'
                    reuseNode true
                }
            }
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Image') {
            steps {
                sh '''
                    docker build -t $APP_NAME:$BUILD_ID .
                sh '''
            }
        }

        stage('Upload Jar To S3') {
            agent {
                docker {
                    image 'amazon/aws-cli'
                    args "--entrypoint=''"
                    reuseNode true
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-creds-user-S3-jenkins-project-springboot-artifacts', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {

                    sh '''
                    JAR_FILE=$(ls target/*.jar | head -1)

                    aws s3 cp \
                        $JAR_FILE \
                        s3://${S3_BUCKET}/
                    '''
                }
            }
        }
    }
    post {
        success {
            echo 'Build completed and JAR uploaded.'
        }

        failure {
            echo 'Pipeline failed.'
        }
    }
}