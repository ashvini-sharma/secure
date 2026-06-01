pipeline {
    agent any

    environment {
        APP_NAME = 'secure-app'
        AWS_ACCOUNT_ID = '562437414962'
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'jenkins-project-springboot-artifacts'
        AWS_DOCKER_REGISTRY = '${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com'
        ECR_REPO = 'learnjenkinsrepo'
    }

    stages {

        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Build Jar') {
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

        stage('Build Project Image') {
            steps {
                sh 'docker build --platform linux/amd64 -t $APP_NAME:$BUILD_ID .'
            }
        }

        /*
        stage('Build Custom AWS-CLI Image') {
            steps {
                sh 'docker build -f Dockerfile-aws-cli -t my-aws-cli .'
            }
        }
        */

        stage('Upload Image to ECR') {
            steps {
               withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-creds-user-S3-jenkins-project-springboot-artifacts',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_DOCKER_REGISTRY}

                        docker tag ${APP_NAME}:${BUILD_ID} ${AWS_DOCKER_REGISTRY}/${ECR_REPO}:${BUILD_ID}

                        docker push ${AWS_DOCKER_REGISTRY}/${ECR_REPO}:${BUILD_ID}
                    '''
                }
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