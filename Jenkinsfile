// =============================================================================
// YAS Monorepo CI Pipeline - Jenkinsfile
// =============================================================================
// Pipeline tự động phát hiện service nào thay đổi và chỉ build/test service đó.
// Yêu cầu plugin: Pipeline, Git, JUnit, SonarQube Scanner, Snyk Security, Jacoco
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

                    def allServices = env.JAVA_SERVICES.split(',')
                    def changedServices = []

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
        // STAGE 3: TEST - Chạy Unit Test
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
                        // 🛠️ FIX 1: Thêm 'jacoco:report' để ép Maven tạo ra file jacoco.xml cho SonarQube đọc
                        sh "mvn clean verify jacoco:report -pl ${svc} -am -DskipITs"
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
                    
                    // ── Upload Coverage Results ──
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']]
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
        // STAGE 5: SonarQube Analysis
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
                        
                        // 🛠️ FIX 2: Phân loại rạch ròi giữa Pull Request và Branch thông thường cho SonarCloud
                        def sonarParams = ""
                        if (env.CHANGE_ID) {
                            sonarParams = "-Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} -Dsonar.pullrequest.base=${env.CHANGE_TARGET}"
                        } else {
                            sonarParams = "-Dsonar.branch.name=${env.BRANCH_NAME}"
                        }

                        withSonarQubeEnv('SonarCloud') {  
                            // 🛠️ FIX 3: Truyền tham số ${sonarParams} vào lệnh Maven
                            sh """
                                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                    -pl ${svc} -am \
                                    -Dsonar.organization=${SONAR_ORG} \
                                    -Dsonar.projectKey=hoanghaitapcode_DevOps_Lab1 \
                                    ${sonarParams} \
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
        // STAGE 7: Snyk Security Scan
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