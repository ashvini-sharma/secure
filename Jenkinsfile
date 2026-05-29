pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'my-springboot-artifacts'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.9.11-eclipse-temurin-21'
                }
            }
            steps {
                sh 'mvn clean package'
            }
        }

/*
        stage('Upload Jar To S3') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-creds']
                ]) {

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
*/
    post {
        success {
            echo 'Build completed and JAR uploaded.'
        }

        failure {
            echo 'Pipeline failed.'
        }
    }
}