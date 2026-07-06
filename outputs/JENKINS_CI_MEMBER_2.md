# Thành viên 2 - Jenkins CI & Container Lead

File này là checklist làm việc chính của bạn. Mục tiêu của bạn là nâng Jenkins CI Project 1 để build/push Docker image cho service thay đổi, đúng yêu cầu Project 2.

## Kết quả phải bàn giao

Cuối đồ án, bạn cần chứng minh:

- Jenkins cũ Project 1 vẫn là CI chính.
- Jenkins detect đúng service thay đổi trong monorepo.
- Jenkins chạy test/build/coverage/security scan cho service thay đổi.
- Jenkins không build full 14 service song song.
- Jenkins push Docker Hub image với tag commit SHA.
- Branch `main` có tag `main`.
- Branch developer có tag commit SHA, tùy chọn thêm branch alias.

Ví dụ cần có trên Docker Hub:

```text
docker.io/<dockerhub-user>/yas-tax:<commit-sha>
docker.io/<dockerhub-user>/yas-tax:main
docker.io/<dockerhub-user>/yas-tax:dev_tax_service
```

Tag bắt buộc để báo cáo là commit SHA. Branch alias chỉ là tiện ích.

## Nguyên tắc làm

- Không chuyển CI/CD mới sang GitHub Actions.
- Không chạy full YAS bằng Docker Compose trên Jenkins.
- Không build nhiều service song song trên Jenkins 8GB.
- Không hardcode Docker Hub password/token trong Jenkinsfile.
- Workload nặng chạy trên K3s/GCP, Jenkins chỉ build/push/deploy command.

## Ngày 1 - Làm Jenkinsfile sạch hơn

### 1. Kiểm tra tool Jenkins và giữ JDK25

Jenkinsfile hiện có:

```groovy
tools {
    maven 'Maven'
    jdk   'JDK25'
}
```

Nhóm đã thống nhất dùng JDK25. Vì vậy không đổi xuống JDK21.

Cần làm:

- Vào Jenkins `Manage Jenkins` -> `Tools`.
- Kiểm tra có JDK tool tên đúng `JDK25`.
- Nếu chưa có, cấu hình thêm JDK25 trong Jenkins Tools.
- Giữ nguyên Jenkinsfile:

```groovy
jdk 'JDK25'
```

### 2. Sửa JUnit glob

Hiện Jenkinsfile có pattern dễ sai:

```groovy
'*/target/surefire-reports/TEST-.xml, */target/failsafe-reports/TEST-.xml'
```

Nên sửa thành:

```groovy
junit(
    testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
    allowEmptyResults: true
)
```

### 3. Giữ change detection hiện có

Stage `Detect Changed Services` đang là tài sản quan trọng của Project 1. Không bỏ.

Cần kiểm tra danh sách service ưu tiên:

```groovy
JAVA_SERVICES = 'cart,customer,inventory,media,order,product,search,storefront-bff,backoffice-bff,tax,sampledata'
```

Có thể giữ thêm service khác, nhưng demo nên ưu tiên:

- product
- cart
- order
- customer
- inventory
- tax
- media
- search
- storefront-bff
- backoffice-bff
- sampledata

UI `storefront` và `backoffice` là Next.js, cần xử lý riêng nếu muốn build image UI.

Done ngày 1 khi:

- Jenkinsfile không làm mất pipeline Project 1.
- Test result glob đúng.
- Jenkins có tool `JDK25` hoặc đã ghi rõ blocker nếu thiếu.

## Ngày 2 - Thêm Docker build/push

### 1. Tạo Docker Hub credential trong Jenkins

Credential gợi ý:

```text
ID: dockerhub-credentials
Kind: Username with password
Username: <dockerhub-user>
Password: <dockerhub-token-or-password>
```

Không commit credential vào repo.

### 2. Image naming thống nhất

| Repo folder | Docker Hub image |
|---|---|
| `product` | `yas-product` |
| `cart` | `yas-cart` |
| `order` | `yas-order` |
| `customer` | `yas-customer` |
| `inventory` | `yas-inventory` |
| `tax` | `yas-tax` |
| `media` | `yas-media` |
| `search` | `yas-search` |
| `storefront-bff` | `yas-storefront-bff` |
| `backoffice-bff` | `yas-backoffice-bff` |
| `storefront` | `yas-storefront` |
| `backoffice` | `yas-backoffice` |
| `sampledata` | `yas-sampledata` |

### 3. Docker build/push skeleton

Thêm stage sau stage `Build`, sau khi test/scan đã qua. Có thể bắt đầu với backend Java trước:

```groovy
stage('Docker Build and Push') {
    when {
        expression { env.CHANGED_SERVICES?.trim() }
    }
    steps {
        script {
            def dockerUser = '<dockerhub-user>'
            def commitSha = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
            def branchAlias = env.BRANCH_NAME.replaceAll('[^A-Za-z0-9_.-]', '-')
            def services = env.CHANGED_SERVICES.split(',')

            withCredentials([usernamePassword(
                credentialsId: 'dockerhub-credentials',
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
            )]) {
                sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'

                for (svc in services) {
                    def image = "docker.io/${dockerUser}/yas-${svc}"
                    echo "Building image ${image}:${commitSha}"
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
                }
            }
        }
    }
}
```

Nếu `storefront`/`backoffice` cần image name khác, dùng map riêng thay vì `yas-${svc}`.

### 4. Test với một service

Tạo branch demo:

```bash
git checkout -b dev_tax_service
# sửa nhỏ trong tax, ví dụ README/comment/test nhỏ
git add tax
git commit -m "demo: update tax service"
git push origin dev_tax_service
```

Kỳ vọng Jenkins:

- Detect `tax`.
- Test/build `tax`.
- Push `yas-tax:<commit-sha>`.

Done ngày 2 khi:

- Docker Hub có ít nhất 1 image tag commit SHA.
- Có screenshot Jenkins console build/push.
- Có screenshot Docker Hub tag.

## Ngày 3 - Ổn định CI cho CD dùng được

### 1. Đảm bảo tag của branch khớp với CD

Thành viên 3 sẽ lấy tag bằng:

```bash
git rev-parse --short=12 origin/<branch>
```

Vì vậy CI cũng phải push đúng short length `12`. Nếu dùng full SHA thì cả 2 bên cùng dùng full SHA. Không lẫn lộn.

### 2. Main image mặc định

Đảm bảo branch `main` push được:

```text
docker.io/<dockerhub-user>/yas-tax:main
docker.io/<dockerhub-user>/yas-product:main
...
```

Nếu chưa đủ thời gian build all service `main`, ưu tiên build các service demo tối thiểu:

- storefront
- storefront-bff
- product
- cart
- order
- inventory
- tax
- customer
- search nếu kịp

Build tuần tự, không song song.

### 3. Fix Snyk/Sonar nếu gây block demo

Snyk trong Jenkinsfile hiện có `|| true`, tức không block. Sonar nếu fail vì config, cần quyết định:

- Nếu Project 1 đã có screenshot Sonar, không để Sonar phá Docker build Project 2 trong ngày cuối.
- Nếu muốn quality gate, làm sau khi preview đã chạy.

Done ngày 3 khi:

- `developer_build` của thành viên 3 có image để deploy.
- CI ổn định với branch demo.

## Ngày 4 - Release tag cho nâng cao

Nếu bắt buộc đã xong, hỗ trợ Argo CD staging:

```bash
git tag v1.2.3
git push origin v1.2.3
```

Pipeline release nên build/push:

```text
docker.io/<dockerhub-user>/yas-tax:v1.2.3
```

Nếu Jenkins Multibranch không scan tag, làm job riêng hoặc build thủ công qua parameter. Đây là nâng cao, không được làm hỏng preview bắt buộc.

## Ngày 5 - Evidence của bạn

Chụp:

- Jenkins Multibranch Pipeline.
- Stage `Detect Changed Services`.
- Stage `Test`.
- Test result.
- Coverage report 70%.
- Gitleaks.
- SonarCloud/SonarQube.
- Snyk.
- Stage Docker build/push.
- Docker Hub tag commit SHA.
- Docker Hub tag `main`.

## Debug nhanh

Docker permission denied:

```bash
docker ps
groups jenkins
```

Nếu Jenkins agent không chạy Docker được, cần admin thêm Jenkins user vào docker group hoặc dùng node có Docker.

Docker login fail:

```bash
echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
```

Maven hết RAM/chậm:

- Build service tuần tự.
- Không dùng parallel.
- Giữ executor Jenkins 1 hoặc 2.

Tag không khớp:

```bash
git rev-parse --short=12 HEAD
git rev-parse --short=12 origin/dev_tax_service
```

## Câu nói trong demo

> Jenkins cũ từ Project 1 được giữ lại. Pipeline không build toàn bộ monorepo; nó detect service thay đổi, test/build/scan service đó, rồi push Docker image với tag commit SHA để job CD deploy đúng phiên bản branch developer.
