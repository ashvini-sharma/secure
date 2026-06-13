

————————————————————————— JENKINS —————————————————————————

Agenda:
1. DinD and Dood
2. Why Jenkins
3. I have a SpringBoot application that uses mysql DB. The app is in GitHub and DB is in local machine.
   I want to host this application to AWS and want to let the application use AWS RDS mysql DB. 

—————————————————————————— DooD and DinD ———————————————————————————————————

The docker-compose.yaml creates a complete Jenkins + Docker setup for CI/CD.
The overall idea is:

Jenkins container
      ↓
talks securely to
      ↓
separate Docker Engine container

This approach is called:

Docker-in-Docker (DinD)

and is commonly used for Jenkins pipelines that build Docker images.

High-level architecture

+----------------------+
|   my-jenkins         |
| Jenkins + Docker CLI |
+----------------------+
           |
           | TLS connection
           v
+----------------------+
|   jenkins-docker     |
| Docker Engine (DinD) |
+----------------------+


Why 2 containers?
1. my-jenkins
Runs:
	•	Jenkins
	•	Java
	•	Docker CLI
BUT no Docker daemon.

2. jenkins-docker
Runs actual:
	•	Docker daemon
	•	Docker engine
Jenkins sends Docker commands to this container.

Key Difference
Feature
DinD
DooD
Docker daemon location
Inside container
Host machine
Isolation
High
Low
Performance
Slower
Faster
Requires privileged mode
Yes
Usually no
Security
Better isolation but privileged
Host daemon exposure
Storage
Separate
Shared with host
Best for
CI isolation/testing
Simple builds


Real-World Recommendation
For Jenkins:
	•	Small/local setup → DooD is common
	•	Enterprise/secure CI → Kubernetes agents or isolated DinD runners
Modern CI systems increasingly avoid privileged DinD because of security concerns.

—————————————————————————————————————— Configure Jenkin to run the pipeline ———————————————————————————————————
 
   First of all we need Jenkins controller/server. We will use jenkins container. 
   Either use DooD : Jenkins container that has (Jenkins + AWS CLI + Docker CLI) with Host Docker Daemon
   Or     use DinD : Jenkins container that has (Jenkins + AWS CLI + Docker CLI) with a Docker container

   In both the cases we need to add Docker pipeline plugin as docker agents are being user through out the pipeline.
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


	In both cases, we will need to configure Global Credentials that will be used to connect AWS.
    Manage jenkins -> security -> Credentials -> System -> Global -> Username with password -> 
    Fill-in the AWS credentials.. 
			id 'aws-creds-user-S3-jenkins-project-springboot-artifacts' and AWS S3 User’s username and password.
			usernameVariable: 'AWS_ACCESS_KEY_ID'
			passwordVariable: 'AWS_SECRET_ACCESS_KEY'



	Jenkins is now ready to create the pipeline.

—————————————————————————————————————— The pipeline ———————————————————————————————————

Git Commit
    ↓
Jenkins Build
    ↓
Docker Build
    ↓
Push learnjenkinsrepo:${BUILD_ID}
    ↓
Generate task-definition.json
    ↓
Register new ECS Task Definition revision
    ↓
Update ECS Service
    ↓
New Fargate Task starts with new image



That's a solid production-style deployment workflow. 🚀


    Since the pipeline uses .. 
		- S3 bucket to store jar image as archive
		- ECR repo to store docker image
		- ECS (Cluster, Task-definition-family with initial task-definition, ECS service)
		- IAM user that can access S3, ECR and ECS
		- Mysql DB with a schema coupondb and table coupon in it 

	Cook these items before creating a pipeline.


—————————————————————————————————————— Create IAM user with permissions and map it to Jenkins ———————————————————————————————————

1.  Let’s prepare on AWS side first. 
	Create a IAM User ‘user-jenkins-project’ and assign policies and credentials to it.
    
	POLICIES:
    Create a custom policy for S3 access, say DUMMY-POLICY-jenkins-project by using JSON
	{
    		"Version": "2012-10-17",
    		"Statement": [
        		{
            		"Effect": "Allow",
            		"Action": [
                		"s3:PutObject",
                		"s3:GetObject",
                		"s3:ListBucket"
            		],
            		"Resource": [
                		"arn:aws:s3:::jenkins-project-springboot-artifacts",
                		"arn:aws:s3:::jenkins-project-springboot-artifacts/*"
            		]
        		}
    		]
	}

	Along with DUMMY-POLICY-jenkins-project, attach these roles as well AmazonECS_FullAccess and  AmazonEC2ContainerRegistryFullAccess, AmazonEKSClusterPolicy, AmazonEKSServicePolicy
     
	CREDENTIALS:
    Create Access keys for CLI purpose. Save the keys somewhere. You may download these as .csv file as well.
    AKIAYF47B4AZDFKIBSLI
	Kdp/8BYi9QUK4R/j31aFVMuMG4dli8shV1yd5Jsv

2.  Now that AWS user has been created, configure these AWS credentials as Global Credentials at Jenkins side. This will be done 2 parts.
	
	PART-1 :
	Manage jenkins -> security -> Credentials -> System -> Global -> Username with password -> 
    Fillin the AWS credentials.. 
    I have created these with id Jenkins-user AKIAYF47B4AZDFKIBSLI   Kdp/8BYi9QUK4R/j31aFVMuMG4dli8shV1yd5Jsv

	PART-2 :
	Bind the credentials to fixed variable names (as per the aws doc) so that in this pipeline these credentials may be available.
	
	Jenkins pipeline -> pipeline syntax -> withCredentials: Bind credentials to variables
	usernameVariable: 'AWS_ACCESS_KEY_ID'
	passwordVariable: 'AWS_SECRET_ACCESS_KEY'

	
    This will be used in pipeline stages to connect AWS.
	        stage('Upload Image to ECR') {
            steps {
                script {
                    env.IMAGE_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:${BUILD_ID}"
                    echo "IMAGE_URI=${env.IMAGE_URI}"
                }
        
                withCredentials([
                    usernamePassword(
                        credentialsId: ‘Jenkins-user’,
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

                        docker tag $APP_NAME:$BUILD_ID $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$BUILD_ID

                        docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$BUILD_ID
                    '''
                }
            }
        }


	
—————————————————————————————————————— Create S3, ECR, ECS, RDS ———————————————————————————————————


7.  S3 : Create a general purpose S3 bucket with no blockage for public access (uncheck Block all for public access).

8.  ECR : Create a mutable ECR repo.
    Create Cluster, Task-definition-family and ECS service which you will use in Jenkins pipeline.
	
	pipeline {
    	agent any

    	environment {
        APP_NAME = 'couponservice'
        AWS_ACCOUNT_ID = '562437414962' 
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'jenkins-project-springboot-artifacts'
        ECR_REPO = 'couponservicerepo'
        AWS_ECS_CLUSTER = 'couponservice-cluster-prod'
        AWS_ECS_SERVICE = 'couponservice-service-prod'
        AWS_ECS_TD_FAMILY = 'couponservice-taskDefinition'
    	}


9.  ECS Cluster : Create a FARGATE ECS Cluster. 

10. Task-definition-family : Create a task-definition-family with initial task-definition.

11. Create a service with a task-definition-family.
	Service1 -> Tasks -> task1 -> Security Group sg-0701d5871a129b0dd (default) -> Edit Inbound Rules 

￼
		 


    
12. Now let’s use DB and create a schema coupondb, then create a table coupon in it.
    Create mysql database with name coupon-db. Check that it has the same security group sg-0701d5871a129b0dd (default) and same VPC vpc-0fa17dd420e65a9d2.

	Since it does not has the schema coupondb and the table coupon inside, lets create it. 
	But Since the coupon-db has no public visibility, let’s create the schema and table by the logic below.
	Create an EC2 instance and install a mysql client on it .. all making sure that it uses the same Security group and VPC as the RDS mysql db coupon-db uses.

	Create an EC2 instance with keypair. This generate a pem key. Download it. This pemkey will let us connect EC2 via SSH.
    Transfer this pesky to its correct place i.e. ~/.ssh and connect to EC2.
	
	Find the pemkey -> find ~/Downloads ~/Documents ~/.ssh -name "*.pem" 2>/dev/null
	Got to pemkey location and transfer it to ~/.ssh -> mv ~/Downloads/first-dummy-instance-keypair.pem ~/.ssh/
	cd ~/.ssh
	make the pemkey private -> chmod 400 ~/.ssh/first-dummy-instance-keypair.pem
	connect to EC2 -> ssh -i first-dummy-instance-keypair.pem ec2-user@13.235.132.28      (ec2-user is default username and 13.235.132.28 is public IP of EC2)

	

The file RDS-new-db-creation has the complete process with console output. 


—————————————————————————————————————— Configure the pipeline ———————————————————————————————————


- Docker agent will create their own different workspace. To prevent this use reuseNode true for the stages which involve usage of docker agent. 

- checkout stage will download the code in pipeline’s workspace folder. 
  In the Build stage, Maven agent will build to the jar. This will use same workspace because reuseNode true.

- Since Jenkins container is capable of docker commands, we can see usage of docker build -t image-name .  .
  Create the image by using downloaded application’s  Dockerfile. 
  Tag the image -> docker build -t secure-app:$BUILD_ID . 
 
- Push this docker image on ECR repo. Note that this ECR image has a BUILD_ID associated with its filename.

- We will create a task-definition-family with an initial task-definition/node-definition.
  When the pipeline runs, it creates a new image with a new BUILD_ID (e.g.image name:3) and pushes it to ECR.
  In the next stage, a new task-definition with this new image having new name is created and registered to task-family with a new revision number. 
  A rolling-update/Blue-Green update is carried out, so that either new version is deployed or rollback to last revision happens. 
  This type of deployment ensures Zero downtime.. as old revision node only become deregistered when the new revision node gets fully stable.

- Notice that each task <——> task-definition is a node. (Task definition has image info, CPU and memory info, port info)

- Notice that pom.xml has java21.
  In the build stage of the pipeline, version 21 in mentioned for maven as version 25 image is not available.

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


——————————————————————————————————————————————————— task-definition-template.json ——————————————————————————————————————————————————————————————————————————
{
  "family": "couponservice-taskDefinition",
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "networkMode": "awsvpc",
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::562437414962:role/ecsTaskExecutionRole",

  "containerDefinitions": [
    {
      "name": "couponservice",
      "image": "IMAGE_URI_PLACEHOLDER",
      "essential": true,

      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp",
          "appProtocol": "http"
        }
      ],

      "environment": [
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:mysql://coupon-db.c1umeey4y3ip.ap-south-1.rds.amazonaws.com:3306/coupondb"
        },
        {
          "name": "SPRING_DATASOURCE_USERNAME",
          "value": "admin"
        },
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "value": "secret123"
        }
      ],

      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/couponservice",
          "awslogs-region": "ap-south-1",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "true"
        }
      }
    }
  ]
}

——————————————————————————————————————————————————— Jenkinsfile ——————————————————————————————————————————————————————————————————————————
pipeline {
    agent any

    environment {
        APP_NAME = 'couponservice'
        AWS_ACCOUNT_ID = '562437414962' 
        AWS_REGION = 'ap-south-1'
        S3_BUCKET = 'jenkins-project-springboot-artifacts'
        ECR_REPO = 'couponservicerepo'
        AWS_ECS_CLUSTER = 'couponservice-cluster-prod'
        AWS_ECS_SERVICE = 'couponservice-service-prod'
        AWS_ECS_TD_FAMILY = 'couponservice-taskDefinition'
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

        stage('Debug Env before ECR Push') {
            steps {
                sh '''
                    echo AWS_REGION=$AWS_REGION
                    echo AWS_ACCOUNT_ID=$AWS_ACCOUNT_ID
                '''
            }
        }

        stage('Upload Image to ECR') {
            steps {
                script {
                    env.IMAGE_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:${BUILD_ID}"
                    echo "IMAGE_URI=${env.IMAGE_URI}"
                }
        
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-creds-user-S3-jenkins-project-springboot-artifacts',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

                        docker tag $APP_NAME:$BUILD_ID $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$BUILD_ID

                        docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$BUILD_ID
                    '''
                }
            }
        }

        stage('Prepare Task Definition') {
                steps {
                    sh '''
                        sed \
                        "s|IMAGE_URI_PLACEHOLDER|${IMAGE_URI}|g" task-definition-template.json > task-definition-prod.json
                    '''
                }
        }

        stage('Register ECS Task Definition and Update ECS Service') {
            steps {
               withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-creds-user-S3-jenkins-project-springboot-artifacts',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        LATEST_TD_REVISION=$(aws ecs register-task-definition --cli-input-json file://task-definition-prod.json | jq -r '.taskDefinition.revision')
                        if [ -z "$LATEST_TD_REVISION" ]; then
                            echo "FAILED - Task definition registration with ECS"
                            exit 1
                            fi
                        aws ecs update-service --cluster $AWS_ECS_CLUSTER --service $AWS_ECS_SERVICE --task-definition $AWS_ECS_TD_FAMILY:$LATEST_TD_REVISION
                        aws ecs wait services-stable --cluster $AWS_ECS_CLUSTER --services $AWS_ECS_SERVICE
                    '''
                }
            }
        }

        stage('Upload Jar To S3') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-creds-user-S3-jenkins-project-springboot-artifacts', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {

                    sh '''
                    JAR_FILE=$(ls target/*.jar | head -1)

                    aws s3 cp $JAR_FILE s3://$S3_BUCKET/
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

——————————————————————————————————————————————————— Dockerfile ——————————————————————————————————————————————————————————————————————————

FROM eclipse-temurin:25-jdk
COPY target/couponservice-0.0.1-SNAPSHOT.jar couponservice.jar
ENTRYPOINT [ "java","-jar","/couponservice.jar" ]


