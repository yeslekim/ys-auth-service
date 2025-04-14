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

        // 🔧 GitParameter에서 들어온 브랜치명에서 origin/ 제거
        CLEAN_BRANCH = "${params.BRANCH_TO_DEPLOY}".replaceFirst("^origin/", "")
    }

    stages {
        stage('Print Branch') {
            steps {
                echo "👉 받은 브랜치: ${params.BRANCH_TO_DEPLOY}"
                echo "✅ 정제된 브랜치: ${CLEAN_BRANCH}"
            }
        }

        stage('Checkout') {
            steps {
                git branch: "${env.CLEAN_BRANCH}",
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
                    def tag = "${REGISTRY}/${IMAGE_NAME}:${env.CLEAN_BRANCH}"
                    sh "./gradlew jib --image=${tag}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: "${KUBECONFIG_ID}", variable: 'KUBECONFIG')]) {
                    echo "🚀 Deploying: auth-service on branch ${env.CLEAN_BRANCH}"

                    sh "kubectl apply -f k8s/pv.yaml || true"
                    sh "kubectl apply -f k8s/pvc.yaml || true"

                    sh "kubectl apply -f k8s/deployment.yaml"
                    sh "kubectl apply -f k8s/service.yaml"
                }
            }
        }
    }
}
