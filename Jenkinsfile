pipeline {
    agent any

    environment {
        registry = "mimi019/docker-spring-boot"
        awsCredentialsId = 'aws_credentials'
        dockerImage = ''
        AWS_REGION = 'us-east-1' // or your desired region
        CLUSTER_NAME = 'spring-cluster' // your EKS cluster name
        region = 'us-east-1'
    }

    stages {
        stage('Maven Clean') {
            steps {
                script {
                    echo 'Cleaning Maven project...'
                    sh 'mvn clean'
                }
            }
        }
        stage('Artifact Construction') {
            steps {
                script {
                    echo 'Constructing artifacts...'
                    sh 'mvn package -Dmaven.test.skip=true -P test-coverage'
                }
            }
        }
        stage('Unit Tests') {
            steps {
                script {
                    echo 'Launching Unit Tests...'
                    sh 'mvn test'
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                script {
                    echo 'Running SonarQube analysis...'
                    withCredentials([usernamePassword(credentialsId: 'SonarQube_credentials', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASSWORD')]) {
                        sh 'mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.login=$SONAR_USER -Dsonar.password=$SONAR_PASSWORD'
                    }
                }
            }
        }
        stage('Publish to Nexus') {
            steps {
                script {
                    echo 'Publishing artifacts to Nexus...'
                    withCredentials([usernamePassword(credentialsId: 'Nexus_credentials', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD')]) {
                        sh 'mvn deploy -s /var/jenkins_home/.m2/settings.xml'
                    }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    echo 'Building Docker image...'
                    dockerImage = docker.build("${registry}:latest")
                }
            }
        }
        stage('Deploy Docker Image') {
            steps {
                script {
                    echo 'Deploying Docker image to Docker Hub...'
                    docker.withRegistry('https://registry.hub.docker.com', 'docker_hub') {
                        dockerImage.push()
                    }
                }
            }
        }
        stage('Test AWS Credentials') {
            steps {
                withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')]) {
                    script {
                        def awsCredentials = readFile(AWS_CREDENTIALS_FILE).trim().split("\n")
                        env.AWS_ACCESS_KEY_ID = awsCredentials.find { it.startsWith("aws_access_key_id") }.split("=")[1].trim()
                        env.AWS_SECRET_ACCESS_KEY = awsCredentials.find { it.startsWith("aws_secret_access_key") }.split("=")[1].trim()
                        env.AWS_SESSION_TOKEN = awsCredentials.find { it.startsWith("aws_session_token") }?.split("=")[1]?.trim()

                        echo "AWS Access Key ID: ${env.AWS_ACCESS_KEY_ID}"
                        // Optional: echo "AWS Session Token: ${env.AWS_SESSION_TOKEN}"

                        echo "AWS Credentials File Loaded"

                        // Test AWS Credentials
                        sh '''
                        aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
                        aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
                        aws configure set aws_session_token $AWS_SESSION_TOKEN
                        aws sts get-caller-identity
                        ''' // Ensure AWS CLI can access the credentials
                    }
                }
            }
        }
        stage('Retrieve AWS Resources') {
            steps {
                withCredentials([file(credentialsId: awsCredentialsId, variable: 'AWS_CREDENTIALS_FILE')]) {
                    script {
                        echo "AWS Access Key ID: ${env.AWS_ACCESS_KEY_ID}"
                        echo "AWS Credentials File Loaded"

                        // Retrieve role_arn
                        env.ROLE_ARN = sh(script: "aws iam list-roles --query 'Roles[?RoleName==`LabRole`].Arn' --output text", returnStdout: true).trim()
                        echo "Retrieved Role ARN: ${env.ROLE_ARN}"
//                        env.VPC_ID = sh(script: "aws ec2 describe-vpcs --region ${region} --query 'Vpcs[1].VpcId' --output text", returnStdout: true).trim()
//                        echo "Retrieved VPC ID: ${env.VPC_ID}"
//
//                        // Retrieve Internet Gateway ID
//                        env.IGW_ID = sh(script: "aws ec2 describe-internet-gateways --region ${region} --query 'InternetGateways[0].InternetGatewayId' --output text", returnStdout: true).trim()
//                        echo "Retrieved Internet Gateway ID: ${env.IGW_ID}"
//
//                        // Retrieve Public Subnet ID
//                        env.PUBLIC_SUBNET_ID = sh(script: "aws ec2 describe-subnets --region ${region} --filters 'Name=vpc-id,Values=${env.VPC_ID}' 'Name=tag:Name,Values=public-subnet' --query 'Subnets[0].SubnetId' --output text", returnStdout: true).trim()
//                        echo "Retrieved Public Subnet ID: ${env.PUBLIC_SUBNET_ID}"
//
//                        // Retrieve Private Subnet ID
//                        env.PRIVATE_SUBNET_ID = sh(script: "aws ec2 describe-subnets --region ${region} --filters 'Name=vpc-id,Values=${env.VPC_ID}' 'Name=tag:Name,Values=private-subnet' --query 'Subnets[1].SubnetId' --output text", returnStdout: true).trim()
//                        echo "Retrieved Private Subnet ID: ${env.PRIVATE_SUBNET_ID}"

                    }
                }
            }
        }
        stage('Initialize Terraform') {
            steps {
                dir('Terraform') {
                    sh 'terraform init'
                }
            }
        }
        stage('Validate Terraform') {
            steps {
                dir('Terraform') {
                    sh 'terraform validate'
                }
            }
        }
        stage('Apply Terraform') {
            steps {
                dir('Terraform') {
                    sh 'terraform apply -auto-approve -var cluster_name=${CLUSTER_NAME}'
                }
            }
        }
        stage('Deploy to AWS Kubernetes (EKS)') {
            steps {
                script {
                    // Update kubeconfig to interact with the EKS cluster
                    sh """
                    aws eks update-kubeconfig --region ${region} --name ${CLUSTER_NAME}
                    kubectl apply -f mysql-secrets.yaml
                    kubectl apply -f mysql-configMap.yaml
                    """

                    sh """
                    export cluster_name=${CLUSTER_NAME}
                    envsubst < deployment-mysql.yaml > rendered-deployment-mysql.yaml
                    kubectl apply -f rendered-deployment-mysql.yaml
                    """

                    // Substitute the cluster name in app-deployment.yaml using envsubst
                    sh """
                    export cluster_name=${CLUSTER_NAME}
                    envsubst < deployment.yaml > rendered-deployment.yaml
                    kubectl apply -f rendered-deployment.yaml
                    """
                }
            }
        }
    }

    post {
        always {
            emailext attachLog: true,
                    subject: "'${currentBuild.result}' - Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: "Project: ${env.JOB_NAME}<br/>" +
                            "Build Number: ${env.BUILD_NUMBER}<br/>" +
                            "URL: ${env.BUILD_URL}<br/>" +
                            "Result: ${currentBuild.result}<br/>",
                    to: 'moula.meriame@gmail.com',
                    mimeType: 'text/html'
        }
        success {
            emailext attachLog: true,
                    subject: "SUCCESS - Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: "Project: ${env.JOB_NAME}<br/>" +
                            "Build Number: ${env.BUILD_NUMBER}<br/>" +
                            "URL: ${env.BUILD_URL}<br/>" +
                            "Result: SUCCESS<br/>",
                    to: 'moula.meriame@gmail.com',
                    mimeType: 'text/html'
        }
        failure {
            emailext attachLog: true,
                    subject: "FAILURE - Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: "Project: ${env.JOB_NAME}<br/>" +
                            "Build Number: ${env.BUILD_NUMBER}<br/>" +
                            "URL: ${env.BUILD_URL}<br/>" +
                            "Result: FAILURE<br/>",
                    to: 'moula.meriame@gmail.com',
                    mimeType: 'text/html'
        }
    }
}
