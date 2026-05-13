pipeline {
    agent none
    options {
        checkoutToSubdirectory('quarkus-auth')
        newContainerPerStage()
    }
    environment {
        PROJECT_DIR = 'quarkus-auth'
        GH_USER = 'newgrnetci'
        GH_EMAIL = '<argo@grnet.gr>'
    }
    stages {
        stage('Build & Test') {
            agent {
                docker {
                    image 'argo.registry:5000/rocky9-java17-mvn3.9.9:latest'
                    args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -u root:root'
                }
            }
            steps {
                echo 'Build & Test'
                sh """
                cd ${WORKSPACE}/${PROJECT_DIR}
                mvn -B clean verify
                """
                archiveArtifacts artifacts: '**/runtime/target/*.jar, **/deployment/target/*.jar', allowEmptyArchive: true
                step([ $class: 'JacocoPublisher' ])
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
        stage('Publish to GitHub Packages') {
            when {
                anyOf {
                    branch 'main'
                    branch 'devel'
                }
            }
            agent {
                docker {
                    image 'argo.registry:5000/rocky9-java17-mvn3.9.9:latest'
                    args '-v $HOME/.m2:/root/.m2 -u root:root'
                }
            }
            steps {
                echo "Publishing quarkus-auth artifact to GitHub Packages (branch: ${env.BRANCH_NAME})"
                withCredentials([usernamePassword(
                    credentialsId: 'newgrnetci-publish-maven-packages',
                    usernameVariable: 'GHPKG_USERNAME',
                    passwordVariable: 'GHPKG_KEY'
                )]) {
                    sh """
                    cd ${WORKSPACE}/${PROJECT_DIR}
                    mkdir -p ~/.m2
                    cat > ~/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>\${GHPKG_USERNAME}</username>
      <password>\${GHPKG_KEY}</password>
    </server>
  </servers>
</settings>
EOF
                    if [ "\${BRANCH_NAME}" = "main" ]; then
                        mvn -B versions:set -DremoveSnapshot=true -DgenerateBackupPoms=false
                    fi
                    mvn -B clean deploy -DskipTests
                    """
                }
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
    }
    post {
        success {
            script {
                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'devel') {
                    slackSend(message: ":rocket: New version for <$BUILD_URL|$PROJECT_DIR>:$BRANCH_NAME Job: $JOB_NAME !")
                }
            }
        }
        failure {
            script {
                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'devel') {
                    slackSend(message: ":rain_cloud: Build Failed for <$BUILD_URL|$PROJECT_DIR>:$BRANCH_NAME Job: $JOB_NAME")
                }
            }
        }
    }
}