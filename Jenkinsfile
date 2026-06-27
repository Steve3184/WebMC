pipeline {
    agent any

    environment {
        JAVA_HOME = tool 'jdk-21'
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        string(name: 'MINECRAFT_VERSION', value: '1.21.8', description: 'Minecraft version')
        booleanParam(name: 'SKIP_PATCHES', value: false, description: 'Skip patch application')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup') {
            steps {
                sh 'chmod +x scripts/apply-patches.sh'
                sh 'scripts/apply-patches.sh'
            }
        }

        stage('TeaVM Compilation') {
            steps {
                sh './gradlew clean build'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'web/**', fingerprint: true
                }
            }
        }

        stage('Artifact Assembly') {
            steps {
                // Remove source maps to prevent Chromium crashes (belt and suspenders)
                sh 'find work/build/generated/teavm/ -name "*.js.map" -delete'
                sh 'find web/ -name "*.js.map" -delete'
            }
        }

        stage('Publish') {
            steps {
                archiveArtifacts artifacts: 'web/**', fingerprint: true
                publishHTML([
                    reportDir: 'web',
                    reportFiles: 'index.html',
                    reportName: 'WebMC Preview',
                    allowMissing: false
                ])
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            mail to: env.MAIL_RECIPIENTS,
                 subject: "WebMC Build Success (#${env.BUILD_NUMBER})",
                 body: "Build completed successfully.\nArtifact: ${env.BUILD_URL}artifact/"
        }
        failure {
            mail to: env.MAIL_RECIPIENTS,
                 subject: "WebMC Build Failed (#${env.BUILD_NUMBER})",
                 body: "Build failed. Check console output: ${env.BUILD_URL}"
        }
    }
}
