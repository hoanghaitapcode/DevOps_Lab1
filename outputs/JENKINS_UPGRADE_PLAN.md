# Jenkins Upgrade Plan - Mở rộng CI Project 1 thành CI/CD

## Mục tiêu

Giữ Jenkins cũ Project 1 làm trung tâm. Chỉ nâng pipeline và thêm job CD, không thay bằng GitHub Actions.

## Việc cần sửa trong Jenkinsfile hiện tại

1. Giữ JDK25 theo thống nhất nhóm.
   - Hiện `Jenkinsfile` dùng `jdk 'JDK25'`.
   - Kiểm tra Jenkins cũ có tool đúng tên `JDK25`.
   - Nếu Jenkins thiếu tool này, cấu hình thêm JDK25 trong Jenkins Tools, không đổi pipeline xuống JDK21.

2. Sửa JUnit glob.
   - Hiện thấy `*/target/surefire-reports/TEST-.xml`.
   - Nên dùng:

```groovy
junit(
  testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
  allowEmptyResults: true
)
```

3. Thêm UI service vào change detection.
   - `storefront` -> image `yas-storefront-ui` hoặc thống nhất `yas-storefront`.
   - `backoffice` -> image `yas-backoffice-ui` hoặc thống nhất `yas-backoffice`.
   - Backend Java giữ như hiện tại.

4. Thêm Docker build/push stage.
   - Chỉ chạy khi `CHANGED_SERVICES` có service.
   - Build tuần tự.
   - Push commit SHA tag.
   - Nếu branch `main`, push thêm `main`.
   - Push branch alias nếu muốn dễ demo.

5. Không chạy workload YAS trên Jenkins.
   - Jenkins chỉ build/push/deploy command.
   - Runtime chạy trên K3s/GCP.

## Credential cần có trong Jenkins

| Credential ID | Loại | Mục đích |
|---|---|---|
| `dockerhub-credentials` | username/password hoặc token | `docker login` và push image |
| `kubeconfig-yas-k3s` | secret file | Jenkins deploy K3s |
| `snyk-token` | secret text | Snyk scan |
| `SonarCloud` | Jenkins Sonar config | SonarCloud analysis |

Không commit các giá trị này vào repo.

## Service image naming đề xuất

| Service folder | Docker Hub image |
|---|---|
| `product` | `docker.io/<dockerhub-user>/yas-product` |
| `cart` | `docker.io/<dockerhub-user>/yas-cart` |
| `order` | `docker.io/<dockerhub-user>/yas-order` |
| `customer` | `docker.io/<dockerhub-user>/yas-customer` |
| `inventory` | `docker.io/<dockerhub-user>/yas-inventory` |
| `tax` | `docker.io/<dockerhub-user>/yas-tax` |
| `media` | `docker.io/<dockerhub-user>/yas-media` |
| `search` | `docker.io/<dockerhub-user>/yas-search` |
| `storefront-bff` | `docker.io/<dockerhub-user>/yas-storefront-bff` |
| `storefront` | `docker.io/<dockerhub-user>/yas-storefront` |
| `backoffice-bff` | `docker.io/<dockerhub-user>/yas-backoffice-bff` |
| `backoffice` | `docker.io/<dockerhub-user>/yas-backoffice` |
| `sampledata` | `docker.io/<dockerhub-user>/yas-sampledata` |

## Groovy sketch cho Docker stage

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

      withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
        for (svc in services) {
          def image = "docker.io/${dockerUser}/yas-${svc}"
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

Lưu ý: với `storefront`/`backoffice`, image name có thể không trùng folder. Nếu nhóm chọn `yas-storefront-ui`, cần map riêng thay vì `yas-${svc}`.

## Jenkins resource guard

- `disableConcurrentBuilds()` giữ nguyên.
- Jenkins executor: 1 hoặc 2.
- Không dùng `parallel` cho build nhiều service.
- Maven dùng `-DskipITs` nếu integration test quá nặng; báo cáo rõ test scope.
- Docker build từng service.
- Cleanup workspace cuối pipeline giữ nguyên.
