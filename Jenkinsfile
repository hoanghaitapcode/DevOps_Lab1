// =============================================================================
// YAS Monorepo CI Pipeline - Jenkinsfile
// =============================================================================
// Pipeline tự động phát hiện service nào thay đổi và chỉ build/test service đó.
// Yêu cầu plugin: Pipeline, Git, JUnit, JaCoCo, SonarQube Scanner, Snyk Security
// =============================================================================

pipeline {
    agent any

    tools {
        maven 'Maven'       // Tên Maven tool đã cấu hình trong Jenkins → Global Tool Configuration
        jdk   'JDK25'       // Tên JDK tool đã cấu hình trong Jenkins → Global Tool Configuration
    }

    environment {
        // ── SonarCloud ──────────────────────────────────────────────────────
        SONAR_TOKEN       = credentials('sonarcloud-token')      // ID credential trong Jenkins
        SONAR_ORG         = 'hoanghaitapcode'                // ← ĐỔI thành org SonarCloud của nhóm
        SONAR_HOST_URL    = 'https://sonarcloud.io'

        // ── Snyk ────────────────────────────────────────────────────────────
        SNYK_TOKEN        = credentials('snyk-token')            // ID credential trong Jenkins

        // ── Danh sách Java microservices (các thư mục gốc chứa pom.xml) ──
        // Nếu có service mới, thêm vào đây
        JAVA_SERVICES = 'cart,customer,inventory,location,media,order,payment,payment-paypal,product,promotion,rating,search,storefront-bff,backoffice-bff,tax,webhook,sampledata,recommendation,delivery'
    }

    options {
        // Giới hạn thời gian chạy pipeline tối đa 60 phút
        timeout(time: 60, unit: 'MINUTES')
        // Không cho chạy đồng thời cùng branch
        disableConcurrentBuilds()
        // Hiển thị timestamp trong console log
        timestamps()
        // Giữ lại 10 bản build gần nhất
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        // =====================================================================
        // STAGE 1: Phát hiện service thay đổi (Path Filtering)
        // =====================================================================
        stage('Detect Changed Services') {
            steps {
                script {
                    // Lấy danh sách file thay đổi so với main branch
                    def changedFiles = []

                    if (env.CHANGE_ID) {
                        // Đây là Pull Request → so sánh với target branch
                        sh "git fetch origin ${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET} || true"
                        changedFiles = sh(
                            script: "git diff --name-only origin/${env.CHANGE_TARGET}...HEAD || echo ''",
                            returnStdout: true
                        ).trim().split('\n').findAll { it }
                    } else {
                        // Đây là branch build thông thường → so sánh với commit trước
                        changedFiles = sh(
                            script: "git diff --name-only HEAD~1 HEAD || echo ''",
                            returnStdout: true
                        ).trim().split('\n').findAll { it }
                    }

                    echo "📂 Changed files:\n${changedFiles.join('\n')}"

                    // Xác định service nào bị thay đổi
                    def allServices = env.JAVA_SERVICES.split(',')
                    def changedServices = []

                    // Nếu pom.xml gốc hoặc common-library thay đổi → build tất cả
                    def buildAll = changedFiles.any { file ->
                        file == 'pom.xml' || file.startsWith('common-library/')
                    }

                    if (buildAll) {
                        changedServices = allServices.toList()
                        echo "⚠️  Root pom.xml hoặc common-library thay đổi → Build TẤT CẢ services"
                    } else {
                        for (service in allServices) {
                            if (changedFiles.any { it.startsWith("${service}/") }) {
                                changedServices.add(service)
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "✅ Không có Java service nào thay đổi. Skip build."
                        env.CHANGED_SERVICES = ''
                    } else {
                        env.CHANGED_SERVICES = changedServices.join(',')
                        echo "🔨 Services cần build: ${env.CHANGED_SERVICES}"
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 2: Build common-library trước (dependency chung)
        // =====================================================================
        stage('Build Common Library') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                echo "📦 Building common-library (dependency dùng chung)..."
                sh 'mvn clean install -pl common-library -am -DskipTests -q'
            }
        }

        // =====================================================================
        // STAGE 3: TEST - Chạy Unit Test + JaCoCo Coverage
        // =====================================================================
        stage('Test') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🧪 Testing: ${svc}"
                        // Chạy test + generate JaCoCo report (phase verify)
                        sh "mvn clean verify -pl ${svc} -am -Dmaven.test.failure.ignore=false"
                    }
                }
            }
            post {
                always {
                    // ── Upload JUnit Test Results ──
                    junit(
                        testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
                        allowEmptyResults: true
                    )

                    // ── Upload JaCoCo Coverage Report ──
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**',
                        minimumLineCoverage: '70',
                        maximumLineCoverage: '100'
                    )
                }
            }
        }

        // =====================================================================
        // STAGE 4: BUILD - Đóng gói JAR (không chạy test lại)
        // =====================================================================
        stage('Build') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🏗️  Building: ${svc}"
                        sh "mvn package -pl ${svc} -am -DskipTests -q"
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 5: SonarQube Analysis (Yêu cầu nâng cao 7c)
        // =====================================================================
        stage('SonarQube Analysis') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🔍 SonarQube scanning: ${svc}"
                        withSonarQubeEnv('SonarCloud') {  // Tên SonarQube server đã cấu hình trong Jenkins
                            sh """
                                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                    -pl ${svc} -am \
                                    -Dsonar.organization=${SONAR_ORG} \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.token=${SONAR_TOKEN} \
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
        // STAGE 6: SonarQube Quality Gate (chặn nếu coverage < 70%)
        // =====================================================================
        stage('Quality Gate') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                echo "⏳ Waiting for SonarQube Quality Gate..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // =====================================================================
        // STAGE 7: Snyk Security Scan (Yêu cầu nâng cao 7c)
        // =====================================================================
        stage('Snyk Security Scan') {
            when {
                expression { env.CHANGED_SERVICES?.trim() }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🛡️  Snyk scanning: ${svc}"
                        // Snyk test - báo cáo lỗ hổng nhưng không fail pipeline
                        sh """
                            snyk test --file=${svc}/pom.xml \
                                      --org=\${SNYK_ORG:-''} \
                                      --severity-threshold=high \
                                      || true
                        """
                        // Snyk monitor - gửi kết quả lên dashboard
                        sh """
                            snyk monitor --file=${svc}/pom.xml \
                                         --org=\${SNYK_ORG:-''} \
                                         || true
                        """
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
            // Dọn dẹp workspace sau mỗi build
            cleanWs()
        }
    }
}
