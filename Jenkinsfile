// =============================================================================
// YAS Monorepo CI Pipeline - Jenkinsfile
// =============================================================================
// Pipeline tự động phát hiện service nào thay đổi và chỉ build/test service đó.
// Yêu cầu plugin: Pipeline, Git, JUnit, SonarQube Scanner, Snyk Security
// =============================================================================

pipeline {
    agent any

    tools {
        maven 'Maven'       // Tên Maven tool đã cấu hình trong Jenkins
        jdk   'JDK25'       // Tên JDK tool đã cấu hình trong Jenkins
    }

    environment {
        // ── SonarCloud ──────────────────────────────────────────────────────
        SONAR_ORG         = 'hoanghaitapcode'                // Org SonarCloud của nhóm

        // ── Snyk ────────────────────────────────────────────────────────────
        SNYK_TOKEN        = credentials('snyk-token')        // ID credential trong Jenkins

        // ── Danh sách Java microservices (các thư mục gốc chứa pom.xml) ──
        JAVA_SERVICES = 'cart,customer,inventory,location,media,order,payment,payment-paypal,product,promotion,rating,search,storefront-bff,backoffice-bff,tax,webhook,sampledata,recommendation,delivery'
        UI_SERVICES = 'storefront,backoffice'
        // Dockerhub username
        DOCKERHUB_USER = 'doubleho'
        GITOPS_BRANCH = 'main'
        GITOPS_REPO_URL = 'https://github.com/hoanghaitapcode/yas-deployment.git'
        GITOPS_REPO_PUSH_PATH = 'github.com/hoanghaitapcode/yas-deployment.git'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        // =====================================================================
        // STAGE 1: Phát hiện service thay đổi (Path Filtering)
        // =====================================================================
        stage('Detect Changed Services') {
            steps {
                script {
                    def changedFiles = []

                    if (env.CHANGE_ID) {
                        sh "git fetch origin ${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET} || true"
                        changedFiles = sh(
                            script: "git diff --name-only origin/${env.CHANGE_TARGET}...HEAD || echo ''",
                            returnStdout: true
                        ).trim().split('\n').findAll { it }
                    } else {
                        changedFiles = sh(
                            script: "git diff --name-only HEAD~1 HEAD || echo ''",
                            returnStdout: true
                        ).trim().split('\n').findAll { it }
                    }

                    echo "📂 Changed files:\n${changedFiles.join('\n')}"

                    def javaServices = env.JAVA_SERVICES.split(',')
                    def uiServices = env.UI_SERVICES.split(',')
                    def changedJavaServices = []
                    def changedUiServices = []

                    def buildAll = changedFiles.any { file ->
                        file == 'pom.xml' || file.startsWith('common-library/')
                    }

                    if (buildAll) {
                        changedJavaServices = javaServices.toList()
                        echo "⚠️  Root pom.xml hoặc common-library thay đổi → Build TẤT CẢ Java services"
                    } else {
                        for (service in javaServices) {
                            if (changedFiles.any { it.startsWith("${service}/") }) {
                                changedJavaServices.add(service)
                            }
                        }

                        for (service in uiServices) {
                            if (changedFiles.any { it.startsWith("${service}/") }) {
                                changedUiServices.add(service)
                            }
                        }
                    }

                    def changedServices = changedJavaServices + changedUiServices
                    env.CHANGED_JAVA_SERVICES = changedJavaServices.join(',')
                    env.CHANGED_UI_SERVICES = changedUiServices.join(',')

                    if (changedServices.isEmpty()) {
                        echo "✅ Không có Java/UI service nào thay đổi. Skip build."
                        env.CHANGED_SERVICES = ''
                    } else {
                        env.CHANGED_SERVICES = changedServices.join(',')
                        echo "🔨 Services cần build: ${env.CHANGED_SERVICES}"
                        echo "☕ Java services: ${env.CHANGED_JAVA_SERVICES ?: 'none'}"
                        echo "🖥️ UI services: ${env.CHANGED_UI_SERVICES ?: 'none'}"
                    }
                }
            }
        }

        // =====================================================================
        // STAGE: Gitleaks Scan
        // =====================================================================
        stage('Gitleaks Scan') {
            steps {
                echo "🔍 Scanning for secrets with Gitleaks..."
                sh "docker run --rm -v ${env.WORKSPACE}:/repo zricethezav/gitleaks:v8.18.4 detect --source=/repo --config=/repo/gitleaks.toml --verbose"
            }
        }

        // =====================================================================
        // STAGE 2: Build common-library trước (dependency chung)
        // =====================================================================
        stage('Build Common Library') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                echo "📦 Building common-library (dependency dùng chung)..."
                sh 'mvn clean install -pl common-library -am -DskipTests -q'
            }
        }

        // =====================================================================
        // STAGE 3: TEST - Chạy Unit Test
        // =====================================================================
        stage('Test') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_JAVA_SERVICES.split(',')
                    for (svc in services) {
                        echo "🧪 Testing: ${svc}"
                        // Chạy Unit Test (bỏ qua Integration Test để tránh lỗi DB)
                        sh "mvn clean verify -pl ${svc} -am -DskipITs"
                    }
                }
            }
            post {
                always {
                    // ── Upload JUnit Test Results ──
                    junit(
                        testResults: '**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml',
                        allowEmptyResults: true
                    )
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        qualityGates: [
                            [threshold: 70.0, metric: 'LINE', baseline: 'PROJECT', criticality: 'FAILURE']
                        ]
                    )
                }
            }
        }

        // =====================================================================
        // STAGE 4: BUILD - Đóng gói JAR (không chạy test lại)
        // =====================================================================
        stage('Build') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_JAVA_SERVICES.split(',')
                    for (svc in services) {
                        echo "🏗️  Building: ${svc}"
                        sh "mvn package -pl ${svc} -am -DskipTests -q"
                    }
                }
            }
        }

        stage('Docker Build and Push') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def commitSha = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                    def branchAlias = env.BRANCH_NAME.replaceAll('[^A-Za-z0-9_.-]', '-')
                    def services = env.CHANGED_SERVICES.split(',')
                    def imageName = { svc ->
                        if (svc == 'storefront') {
                            return 'yas-storefront'
                        }
                        if (svc == 'backoffice') {
                            return 'yas-backoffice'
                        }
                        return "yas-${svc}"
                    }

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'

                        for (svc in services) {
                            def image = "docker.io/${env.DOCKERHUB_USER}/${imageName(svc)}"
                            echo "Building ${image}:${commitSha}"
                            sh """
                                docker build -t ${image}:${commitSha} -t ${image}:${branchAlias} ./${svc}
                                docker push ${image}:${commitSha}
                                docker push ${image}:${branchAlias}
                            """

                            if (env.BRANCH_NAME == 'main') {
                                sh """
                                    docker tag ${image}:${commitSha} ${image}:main
                                    docker push ${image}:main
                                """
                            }

                            if (env.TAG_NAME?.trim()) {
                                sh """
                                    docker tag ${image}:${commitSha} ${image}:${env.TAG_NAME}
                                    docker push ${image}:${env.TAG_NAME}
                                """
                            }
                        }

                        sh 'docker logout'
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 5: SonarQube Analysis
        // =====================================================================
        stage('SonarQube Analysis') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_JAVA_SERVICES.split(',')
                    for (svc in services) {
                        echo "🔍 SonarQube scanning: ${svc}"
                        withSonarQubeEnv('SonarCloud') {  
                            // Đã tối ưu: withSonarQubeEnv tự động xử lý URL và Token
                            sh """
                                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                    -pl ${svc} -am \
                                    -Dsonar.organization=${SONAR_ORG} \
                                    -Dsonar.projectKey=hoanghaitapcode_DevOps_Lab1 \
                                    -Dsonar.java.coveragePlugin=jacoco \
                                    -Dsonar.coverage.jacoco.xmlReportPaths=${svc}/target/site/jacoco/jacoco.xml
                            """
                        }
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 6: SonarQube Quality Gate
        // =====================================================================
        stage('Quality Gate') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                echo "⏳ Waiting for SonarQube Quality Gate..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // =====================================================================
        // STAGE 7: Snyk Security Scan
        // =====================================================================
        stage('Snyk Security Scan') {
            when {
                expression { env.CHANGED_JAVA_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_JAVA_SERVICES.split(',')
                    for (svc in services) {
                        echo "🛡️  Snyk scanning: ${svc}"
                        sh """
                            snyk test --file=${svc}/pom.xml \
                                      --org=\${SNYK_ORG:-''} \
                                      --severity-threshold=high \
                                      || true
                        """
                        sh """
                            snyk monitor --file=${svc}/pom.xml \
                                         --org=\${SNYK_ORG:-''} \
                                         || true
                        """
                    }
                }
            }
        }

        stage('Update GitOps Manifests') {
            when {
                expression {
                    env.CHANGED_SERVICES?.trim() &&
                    (env.BRANCH_NAME == 'main' || env.TAG_NAME?.trim())
                }
            }
            steps {
                script {
                    def commitSha = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                    def targetEnv = env.TAG_NAME?.trim() ? 'staging' : 'dev'
                    def imageTag = env.TAG_NAME?.trim() ? env.TAG_NAME.trim() : commitSha
                    def services = env.CHANGED_SERVICES.split(',')

                    def valuesFile = { svc ->
                        if (svc == 'storefront') {
                            return 'storefront-ui-values.yaml'
                        }
                        if (svc == 'backoffice') {
                            return 'backoffice-ui-values.yaml'
                        }
                        def supported = [
                            'product', 'cart', 'order', 'customer', 'inventory', 'tax',
                            'media', 'search', 'storefront-bff', 'backoffice-bff',
                            'sampledata'
                        ]
                        if (supported.contains(svc)) {
                            return "${svc}-values.yaml"
                        }
                        return ''
                    }

                    dir('yas-deployment') {
                        deleteDir()
                        sh """
                            git clone --branch ${GITOPS_BRANCH} ${GITOPS_REPO_URL} .
                            git config user.email "jenkins@yas.local"
                            git config user.name "jenkins"
                        """

                        for (svc in services) {
                            def fileName = valuesFile(svc)
                            if (!fileName) {
                                echo "Skip GitOps update for unsupported service: ${svc}"
                                continue
                            }

                            def filePath = "envs/${targetEnv}/${fileName}"
                            echo "Updating ${filePath} -> tag ${imageTag}"
                            sh "sed -i 's/^    tag:.*/    tag: ${imageTag}/' ${filePath}"
                            sh "git add ${filePath}"
                        }

                        def hasChanges = sh(
                            script: 'git diff --cached --quiet; echo $?',
                            returnStdout: true
                        ).trim()

                        if (hasChanges == '0') {
                            echo "No GitOps manifest changes to commit."
                        } else {
                            sh "git commit -m 'chore(gitops): update ${targetEnv} image tags to ${imageTag} [skip ci]'"
                            withCredentials([usernamePassword(
                                credentialsId: 'github-push-token',
                                usernameVariable: 'GIT_USER',
                                passwordVariable: 'GIT_TOKEN'
                            )]) {
                                sh """
                                    set +x
                                    git remote set-url origin https://\$GIT_USER:\$GIT_TOKEN@${GITOPS_REPO_PUSH_PATH}
                                    set -x
                                    git push origin HEAD:${GITOPS_BRANCH}
                                    git remote set-url origin ${GITOPS_REPO_URL}
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // POST ACTIONS
    // =========================================================================
    post {
        success {
            echo """
            ╔══════════════════════════════════════════╗
            ║   ✅ PIPELINE PASSED SUCCESSFULLY!       ║
            ║   Branch: ${env.BRANCH_NAME}             ║
            ║   Services: ${env.CHANGED_SERVICES ?: 'none'}
            ╚══════════════════════════════════════════╝
            """
        }
        failure {
            echo """
            ╔══════════════════════════════════════════╗
            ║   ❌ PIPELINE FAILED!                    ║
            ║   Branch: ${env.BRANCH_NAME}             ║
            ║   Check console output for details.      ║
            ╚══════════════════════════════════════════╝
            """
        }
        always {
            cleanWs()
        }
    }
}
