properties([
    parameters([
        gitParameter(
            name: 'BRANCH_TO_DEPLOY',
            type: 'PT_BRANCH',
            branchFilter: '.*',
            defaultValue: 'main',
            sortMode: 'DESCENDING_SMART',
            selectedValue: 'DEFAULT',
            description: '배포할 브랜치를 선택하세요',
            useRepository: 'http://10.1.218.84/achiv/auth-service.git'
        )
    ])
])

pipeline {
    agent any

    environment {
        REGISTRY = '10.1.223.21:5000'
        IMAGE_NAME = 'auth-service'
        KUBECONFIG_ID = 'kubeconfig'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: "${params.BRANCH_TO_DEPLOY}",
                    url: 'http://10.1.218.84/achiv/auth-service.git',
                    credentialsId: 'gitlab-token'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Build & Push Image with Jib') {
            steps {
                script {
                    def tag = "${REGISTRY}/${IMAGE_NAME}:${params.BRANCH_TO_DEPLOY}"
                    sh "./gradlew jib --image=${tag}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: "${KUBECONFIG_ID}", variable: 'KUBECONFIG')]) {
                    echo " Deploying: auth-service on branch ${env.BRANCH_NAME}"

                    sh "kubectl apply -f k8s/pv.yaml || true"
                    sh "kubectl apply -f k8s/pvc.yaml || true"

                    sh "kubectl apply -f k8s/deployment.yaml"
                    sh "kubectl apply -f k8s/service.yaml"
                }
            }
        }
    }
}
