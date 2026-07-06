# Ngày 2 - Runbook chi tiết cho cả 4 thành viên

Mục tiêu ngày 2: nối Jenkins cũ với Docker Hub và K3s. Hết ngày 2 nhóm phải có:

- Jenkins cũ vẫn chạy CI Project 1.
- Jenkins có Docker Hub credential.
- Jenkins build/push được ít nhất 1 image với tag commit SHA, ưu tiên `tax`.
- Jenkins hoặc máy deploy kết nối được K3s bằng kubeconfig.
- Helm deploy thử được ít nhất 1 service vào K3s bằng image tag `main` hoặc commit SHA.
- TV4 có evidence rõ cho Jenkins CI, Docker Hub tag, K3s access.

Nguyên tắc chung:

- Không chuyển sang GitHub Actions.
- Không cài Jenkins mới nếu Jenkins cũ còn dùng được.
- Không hardcode Docker Hub password, kubeconfig, token vào repo.
- Không build toàn bộ 14 service song song.
- Nếu kẹt thời gian, chỉ demo `tax` trước.

## Lịch làm đề xuất trong ngày

| Khung giờ | Cả nhóm cần đạt |
|---|---|
| 0h-0h30 | Sync kết quả ngày 1, xác nhận Jenkins/K3s/Docker Hub |
| 0h30-2h00 | TV1 tạo kubeconfig Jenkins, TV2 thêm Docker build/push, TV3 chuẩn bị deploy thử, TV4 chuẩn bị evidence |
| 2h00-3h30 | TV2 chạy branch demo `dev_tax_service`, TV3 deploy image lên K3s |
| 3h30-4h30 | Fix lỗi Docker/Jenkins/K3s/Helm |
| 4h30-5h00 | Chốt evidence và blocker ngày 3 |

## Thông tin chung cần có trước khi bắt đầu

Điền vào note nhóm:

```text
GitHub repo:
Jenkins URL:
Jenkins job name:
Docker Hub username:
Docker Hub credential ID in Jenkins: dockerhub-credentials
Kubeconfig credential ID in Jenkins: kubeconfig-yas-k3s
GCP master external/private IP:
GCP worker external/private IP:
Demo branch: dev_tax_service
Demo service: tax
Commit SHA length: 12
Preview namespace test: preview-manual
Runtime namespace dùng chung: postgres
Preview NodePort: 30080
```

---

# Thành viên 1 - Infrastructure Lead

Nhiệm vụ ngày 2: tạo kubeconfig/service account cho Jenkins và đảm bảo Jenkins/K3s kết nối được.

## Bước 1. Kiểm tra lại K3s

SSH vào `yas-master`:

```bash
gcloud compute ssh yas-master
```

Kiểm tra:

```bash
kubectl get nodes -o wide
kubectl get pods -A
```

Kỳ vọng:

- `yas-master` Ready.
- `yas-worker` Ready.
- Pod hệ thống K3s không CrashLoopBackOff.

Nếu worker chưa Ready, ưu tiên fix trước khi làm Jenkins kubeconfig.

## Bước 2. Kiểm tra firewall K3s API

Từ máy cá nhân hoặc nơi Jenkins có thể truy cập, kiểm tra port `6443`.

Nếu dùng macOS/Linux:

```bash
nc -vz <MASTER_EXTERNAL_IP> 6443
```

Nếu không có `nc`:

```bash
curl -k https://<MASTER_EXTERNAL_IP>:6443/version
```

Kỳ vọng:

- `nc` báo succeeded, hoặc
- `curl` trả JSON version hoặc thông báo Unauthorized/Forbidden. Unauthorized vẫn tốt, nghĩa là kết nối được.

Nếu timeout:

- Firewall `tcp:6443` chưa mở cho IP Jenkins/máy bạn.
- Master external IP sai.
- K3s chưa chạy.

Mở firewall tạm cho IP của Jenkins hoặc máy test:

```bash
gcloud compute firewall-rules create yas-allow-k3s-api \
  --allow tcp:6443 \
  --source-ranges <JENKINS_PUBLIC_IP_OR_YOUR_PUBLIC_IP>/32 \
  --target-tags yas-k3s
```

Nếu rule đã tồn tại, dùng Console sửa source range.

## Bước 3. Tạo kubeconfig dùng cho Jenkins

Trên `yas-master`:

```bash
sudo cat /etc/rancher/k3s/k3s.yaml
```

Copy nội dung ra file trên máy cá nhân, ví dụ:

```bash
mkdir -p /tmp/yas-kube
nano /tmp/yas-kube/kubeconfig-yas-k3s.yaml
```

Dán kubeconfig vào.

Tìm dòng:

```yaml
server: https://127.0.0.1:6443
```

Đổi thành:

```yaml
server: https://<MASTER_EXTERNAL_IP>:6443
```

Lưu file.

Không copy file này vào repo.

## Bước 4. Test kubeconfig từ máy cá nhân

Trên máy đang có file kubeconfig:

```bash
export KUBECONFIG=/tmp/yas-kube/kubeconfig-yas-k3s.yaml
kubectl get nodes -o wide
kubectl auth can-i get pods --all-namespaces
```

Kỳ vọng:

- Thấy 2 node.
- `kubectl auth can-i` trả `yes`.

Nếu lỗi certificate/IP:

- Kiểm tra `server:` đã đổi đúng IP.
- Kiểm tra firewall.
- Nếu vẫn lỗi TLS SAN, dùng private IP nếu Jenkins cùng VPC, hoặc cài lại K3s với `--tls-san <MASTER_EXTERNAL_IP>`. Nếu đã cài rồi và gấp, thử dùng SSH tunnel hoặc chạy Jenkins agent trong cùng network. Ghi blocker rõ cho nhóm.

## Bước 5. Upload kubeconfig vào Jenkins

Trong Jenkins:

1. Manage Jenkins.
2. Credentials.
3. Chọn domain phù hợp.
4. Add Credentials.
5. Kind: Secret file.
6. File: chọn `/tmp/yas-kube/kubeconfig-yas-k3s.yaml`.
7. ID:

```text
kubeconfig-yas-k3s
```

8. Description:

```text
Kubeconfig for YAS K3s cluster on GCP
```

Không chụp nội dung kubeconfig. Chỉ chụp màn hình credential ID nếu cần, che thông tin nhạy cảm.

## Bước 6. Tạo Jenkins job test Kubernetes access

Nếu có quyền tạo job, tạo Pipeline job tên:

```text
k8s_access_test
```

Pipeline:

```groovy
pipeline {
    agent any
    stages {
        stage('Test K8s Access') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-yas-k3s', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        kubectl get nodes -o wide
                        kubectl auth can-i get pods --all-namespaces
                    '''
                }
            }
        }
    }
}
```

Kỳ vọng console:

```text
yas-master Ready
yas-worker Ready
yes
```

Nếu Jenkins agent thiếu `kubectl`, cài `kubectl` trên Jenkins agent hoặc báo TV2/TV3.

## Bước 7. Cài kubectl/helm trên Jenkins agent nếu thiếu

Nếu Jenkins chạy trên Ubuntu và bạn có quyền shell:

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.30/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.30/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubectl
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
kubectl version --client
helm version
```

Nếu không có quyền sudo:

- Ghi blocker.
- Dùng Jenkins tool installer nếu có.
- Tạm deploy bằng máy cá nhân ngày 2, nhưng ngày 3 phải đưa vào Jenkins job.

## Done của TV1 trong ngày 2

TV1 xong khi có:

- `kubectl get nodes -o wide` vẫn thấy 2 node Ready.
- Jenkins credential `kubeconfig-yas-k3s` được tạo.
- Jenkins job hoặc Jenkins console chạy được `kubectl get nodes`.
- Screenshot/log không lộ kubeconfig.

---

# Thành viên 2 - Jenkins CI & Container Lead

Nhiệm vụ ngày 2: thêm Docker build/push vào Jenkinsfile và push được image tag commit SHA.

## Bước 1. Kiểm tra Jenkins agent có Docker

Trong Jenkins job test hoặc SSH vào Jenkins agent:

```bash
docker version
docker ps
```

Kỳ vọng:

- Docker client/server trả version.
- `docker ps` không permission denied.

Nếu permission denied:

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

Nếu Jenkins chạy bằng container, cần kiểm tra Docker socket mount. Báo nhóm nếu không có quyền sửa.

## Bước 2. Tạo Docker Hub access token

Trên Docker Hub:

1. Account Settings.
2. Personal access tokens.
3. Generate new token.
4. Tên token:

```text
jenkins-yas-project2
```

5. Quyền: Read/Write/Delete nếu cần cleanup tag, Read/Write là đủ.
6. Copy token một lần.

Không gửi token vào chat nhóm công khai. Không commit token.

## Bước 3. Lưu Docker Hub credential vào Jenkins

Trong Jenkins:

1. Manage Jenkins.
2. Credentials.
3. Add Credentials.
4. Kind: Username with password.
5. Username: `<dockerhub-user>`.
6. Password: Docker Hub token.
7. ID:

```text
dockerhub-credentials
```

8. Description:

```text
Docker Hub token for YAS Project 2
```

## Bước 4. Test Docker login từ Jenkins

Tạo Pipeline job tạm hoặc stage test:

```groovy
pipeline {
    agent any
    stages {
        stage('Docker Login Test') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker logout
                    '''
                }
            }
        }
    }
}
```

Kỳ vọng:

```text
Login Succeeded
```

## Bước 5. Thêm Docker build/push stage cho backend Java

Mở `Jenkinsfile`.

Thêm environment:

```groovy
DOCKERHUB_USER = '<dockerhub-user>'
```

Thêm stage sau stage `Build`, trước hoặc sau scan tùy nhóm. Khuyến nghị sau test/build và sau security scan nếu scan đang ổn:

```groovy
stage('Docker Build and Push') {
    when {
        expression { env.CHANGED_SERVICES?.trim() }
    }
    steps {
        script {
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
                    def image = "docker.io/${env.DOCKERHUB_USER}/yas-${svc}"
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
                }

                sh 'docker logout'
            }
        }
    }
}
```

Lưu ý:

- Đổi `<dockerhub-user>` thành username thật.
- Stage này xử lý backend/BFF Java trước.
- UI `storefront`, `backoffice` xử lý riêng sau.

## Bước 6. Kiểm tra service có Dockerfile

Chạy local:

```bash
find . -maxdepth 2 -name Dockerfile | sort
```

Đảm bảo service demo `tax` có:

```text
./tax/Dockerfile
```

## Bước 7. Tạo branch demo `dev_tax_service`

Nếu chưa có branch:

```bash
git checkout main
git pull
git checkout -b dev_tax_service
```

Sửa một file trong `tax`, ví dụ thêm comment nhỏ trong README nếu có, hoặc sửa test nhỏ không phá logic. Nếu không muốn sửa code nghiệp vụ, tạo file note:

```bash
mkdir -p tax/docs
printf "Demo change for Project 2 CI/CD\n" > tax/docs/project2-demo.txt
git add tax/docs/project2-demo.txt
git commit -m "demo: update tax service for project 2"
git push origin dev_tax_service
```

Nếu branch đã có, chỉ cần push commit mới.

## Bước 8. Chạy Jenkins branch build

Trong Jenkins Multibranch:

1. Scan Multibranch Pipeline Now.
2. Mở branch `dev_tax_service`.
3. Build Now nếu chưa tự chạy.

Theo dõi console log. Cần thấy:

```text
Services cần build: tax
Docker Build and Push
docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

## Bước 9. Kiểm tra Docker Hub tag

Lấy SHA:

```bash
git rev-parse --short=12 origin/dev_tax_service
```

Kiểm tra bằng Docker:

```bash
docker pull docker.io/<dockerhub-user>/yas-tax:<commit-sha>
docker image inspect docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

Hoặc mở Docker Hub UI và chụp tag.

## Bước 10. Nếu Docker build fail

Lỗi thường gặp:

1. Jenkins không có Docker permission.
2. Dockerfile cần jar nhưng Maven build output chưa đúng.
3. Image name sai.
4. Docker Hub credential sai.
5. Network Jenkins không push được Docker Hub.

Debug:

```bash
ls -la tax
ls -la tax/target
docker build -t docker.io/<dockerhub-user>/yas-tax:test ./tax
docker login
docker push docker.io/<dockerhub-user>/yas-tax:test
```

Nếu Dockerfile cần build context root thay vì service folder, kiểm tra Dockerfile:

```bash
sed -n '1,160p' tax/Dockerfile
```

Điều chỉnh build command theo Dockerfile thực tế.

## Done của TV2 trong ngày 2

TV2 xong khi có:

- Jenkins credential `dockerhub-credentials`.
- Jenkins Docker login test pass.
- Jenkins branch `dev_tax_service` detect `tax`.
- Docker Hub có `yas-tax:<commit-sha>`.
- Có screenshot Jenkins console và Docker Hub tag.

---

# Thành viên 3 - Jenkins CD & GitOps Lead

Nhiệm vụ ngày 2: deploy thử image bằng Helm lên K3s và chuẩn bị job `developer_build` cho ngày 3.

## Bước 1. Lấy kubeconfig để deploy thử

Nhận kubeconfig từ TV1 qua cách an toàn, không commit vào repo.

Lưu local:

```bash
mkdir -p /tmp/yas-kube
nano /tmp/yas-kube/kubeconfig-yas-k3s.yaml
export KUBECONFIG=/tmp/yas-kube/kubeconfig-yas-k3s.yaml
```

Test:

```bash
kubectl get nodes -o wide
```

Nếu không muốn dùng kubeconfig local, làm trực tiếp trên `yas-master`.

## Bước 2. Kiểm tra Helm local

```bash
helm version
kubectl version --client
```

Nếu thiếu Helm, cài:

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

## Bước 3. Xác định image tag từ TV2

Khi TV2 push xong branch `dev_tax_service`, lấy SHA:

```bash
git fetch origin dev_tax_service
TAX_TAG="$(git rev-parse --short=12 origin/dev_tax_service)"
echo "$TAX_TAG"
```

Hoặc lấy trực tiếp từ TV2.

Ghi:

```text
TAX_TAG=
IMAGE=docker.io/<dockerhub-user>/yas-tax:<TAX_TAG>
```

## Bước 4. Deploy thử namespace `preview-manual`

Tạo namespace:

```bash
kubectl create namespace preview-manual --dry-run=client -o yaml | kubectl apply -f -
```

Build dependency chart:

```bash
helm dependency build k8s/charts/tax
```

Deploy tax:

```bash
helm upgrade --install tax k8s/charts/tax \
  -n preview-manual --create-namespace \
  --set backend.image.repository=docker.io/<dockerhub-user>/yas-tax \
  --set backend.image.tag="$TAX_TAG" \
  --set backend.serviceMonitor.enabled=false
```

Kiểm tra:

```bash
helm list -n preview-manual
kubectl get all -n preview-manual
kubectl get deployment tax -n preview-manual \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng image đúng:

```text
docker.io/<dockerhub-user>/yas-tax:<TAX_TAG>
```

## Bước 5. Nếu pod không chạy vì thiếu dependency

Ngày 2 mục tiêu chính là chứng minh Helm deploy đúng image, pod có thể chưa Ready nếu thiếu Postgres/ConfigMap/Secret.

Kiểm tra lỗi:

```bash
kubectl describe pod -n preview-manual -l app.kubernetes.io/name=tax
kubectl logs -n preview-manual deploy/tax --tail=100
```

Lỗi có thể gặp:

- Thiếu `yas-configuration-configmap`.
- Thiếu `yas-postgresql-credentials-secret`.
- Không kết nối Postgres.
- ImagePullBackOff.

Nếu ImagePullBackOff:

```bash
docker pull docker.io/<dockerhub-user>/yas-tax:$TAX_TAG
kubectl describe pod <pod> -n preview-manual
```

Nếu thiếu config/secret, ghi blocker cho ngày 3. Không mất mục tiêu ngày 2 nếu image override đúng.

Ghi rõ vào evidence:

```text
Ngày 2 đã chứng minh Helm deploy đúng image tag.
Pod chưa Ready là do thiếu runtime dependency của YAS, không phải lỗi Docker build/push.
Ngày 3 developer_build sẽ tự tạo NodePort.
Ngày 4 hoặc bản developer_build hoàn chỉnh sẽ tự bootstrap Postgres + yas-configuration.
```

## Bước 6. Tạo skeleton Jenkins job `developer_build`

Tạo file nháp hoặc Jenkins job draft. Parameters:

```text
ENV_NAME=demo
PRODUCT_BRANCH=main
CART_BRANCH=main
ORDER_BRANCH=main
CUSTOMER_BRANCH=main
INVENTORY_BRANCH=main
TAX_BRANCH=main
MEDIA_BRANCH=main
SEARCH_BRANCH=main
STOREFRONT_BFF_BRANCH=main
STOREFRONT_UI_BRANCH=main
BACKOFFICE_BFF_BRANCH=main
BACKOFFICE_UI_BRANCH=main
```

Pipeline skeleton:

```groovy
pipeline {
    agent any
    parameters {
        string(name: 'ENV_NAME', defaultValue: 'demo')
        string(name: 'TAX_BRANCH', defaultValue: 'main')
        string(name: 'PRODUCT_BRANCH', defaultValue: 'main')
        string(name: 'CART_BRANCH', defaultValue: 'main')
        string(name: 'ORDER_BRANCH', defaultValue: 'main')
        string(name: 'CUSTOMER_BRANCH', defaultValue: 'main')
        string(name: 'INVENTORY_BRANCH', defaultValue: 'main')
        string(name: 'MEDIA_BRANCH', defaultValue: 'main')
        string(name: 'SEARCH_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_BFF_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_UI_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_BFF_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_UI_BRANCH', defaultValue: 'main')
    }
    stages {
        stage('Deploy Preview') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-yas-k3s', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        kubectl get nodes -o wide
                    '''
                }
            }
        }
    }
}
```

Ngày 3 sẽ thêm resolve tag, bootstrap Postgres tối thiểu, cài `yas-configuration`, Helm deploy tax, và tạo NodePort tự động.

Lưu ý: job này dùng `Pipeline Script`, nên sau này không dùng `checkout scm`. Dùng:

```groovy
git branch: 'main', url: 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
```

Nếu Jenkins báo `set: Illegal option -o pipefail`, đổi mọi block shell từ `set -euo pipefail` thành:

```bash
set -eu
```

## Bước 7. Chuẩn bị cleanup command

Test cleanup namespace manual nếu cần:

```bash
kubectl delete namespace preview-manual --ignore-not-found=true
```

Nếu muốn giữ lại để TV4 chụp evidence, chưa xóa ngay. Hỏi TV4 trước.

## Done của TV3 trong ngày 2

TV3 xong khi có:

- Kết nối được K3s bằng kubeconfig.
- Helm deploy thử `tax` vào `preview-manual`.
- Deployment image tag đúng commit SHA.
- Skeleton `developer_build` có thể chạy `kubectl get nodes`.
- Ghi rõ blocker nếu pod chưa Ready vì thiếu dependency.

---

# Thành viên 4 - Service Mesh & QA/Report Lead

Nhiệm vụ ngày 2: kiểm chứng và chụp bằng chứng Jenkins CI, Docker Hub image tag, K3s access, Helm deploy thử.

## Bước 1. Chuẩn bị evidence folder ngày 2

Tạo hoặc dùng thư mục:

```text
evidence/
  03-jenkins-ci-docker/
  04-k3s-access/
  05-helm-manual-deploy/
```

## Bước 2. Chụp Jenkins Docker credential nhưng không lộ secret

Ảnh cần:

- Jenkins credentials list có ID `dockerhub-credentials`.
- Không mở màn hình hiện password/token.

Nếu ảnh có thông tin nhạy cảm, che trước khi đưa vào report.

## Bước 3. Chụp Jenkins kubeconfig credential nhưng không lộ secret

Ảnh cần:

- Jenkins credentials list có ID `kubeconfig-yas-k3s`.
- Không mở nội dung file.

## Bước 4. Chụp Jenkins branch demo

Khi TV2 chạy `dev_tax_service`, chụp console có các đoạn:

```text
Detect Changed Services
Services cần build: tax
Test
Build
Docker Build and Push
docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

Nếu pipeline fail, vẫn chụp lỗi và ghi nguyên nhân.

## Bước 5. Xác minh Docker Hub tag

Nhờ TV2 đưa:

```text
Commit SHA:
Docker image:
```

Tự kiểm tra:

```bash
docker pull docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

Hoặc mở Docker Hub UI.

Chụp:

- Docker Hub tag list.
- Tag commit SHA.
- Tag branch alias nếu có.

## Bước 6. Xác minh K3s access

Chụp TV1 hoặc Jenkins log:

```bash
kubectl get nodes -o wide
kubectl auth can-i get pods --all-namespaces
```

Kỳ vọng:

```text
yas-master Ready
yas-worker Ready
yes
```

## Bước 7. Xác minh Helm deploy manual

Khi TV3 deploy:

```bash
kubectl get all -n preview-manual
helm list -n preview-manual
kubectl get deployment tax -n preview-manual \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Chụp output image. Đây là bằng chứng quan trọng: Kubernetes deployment dùng đúng image tag commit SHA.

Nếu pod chưa Ready, ghi rõ:

```text
Pod chưa Ready do thiếu dependency/config, nhưng Helm deployment đã override đúng image tag commit SHA. Ngày 3 sẽ hoàn thiện dependency/preview.
```

## Bước 8. Cập nhật test plan

Điền kết quả:

```text
Test case: Jenkins Docker build/push
Branch: dev_tax_service
Service: tax
Expected: Docker Hub has yas-tax:<commit-sha>
Actual:
Status: PASS/FAIL
Evidence:

Test case: Jenkins access K3s
Expected: kubectl get nodes works from Jenkins
Actual:
Status: PASS/FAIL
Evidence:

Test case: Helm manual deploy
Expected: deployment tax image uses commit SHA
Actual:
Status: PASS/FAIL
Evidence:
```

## Bước 9. Chuẩn bị câu giải thích ngày 2

Copy vào report:

```text
Trong ngày 2, nhóm mở rộng Jenkins Project 1 để push Docker image lên Docker Hub. Với branch demo dev_tax_service, Jenkins phát hiện chỉ service tax thay đổi, chạy pipeline CI và push image docker.io/<dockerhub-user>/yas-tax:<commit-sha>. Đồng thời nhóm cấu hình kubeconfig cho Jenkins để truy cập K3s trên Google Cloud. Helm được dùng để deploy thử service tax vào namespace preview-manual với image tag commit SHA.
```

## Done của TV4 trong ngày 2

TV4 xong khi có:

- Evidence Jenkins Docker build/push.
- Evidence Docker Hub tag commit SHA.
- Evidence Jenkins/K3s access.
- Evidence Helm deploy image tag.
- Test plan ngày 2 đã điền PASS/FAIL.

---

# Sync cuối ngày 2

Cả nhóm họp 15 phút và điền bảng:

| Câu hỏi | Trả lời |
|---|---|
| Jenkins Docker login pass chưa? | yes/no |
| Branch demo là gì? | dev_tax_service |
| Service demo là gì? | tax |
| Commit SHA 12 ký tự là gì? | |
| Docker Hub image đã push là gì? | |
| Jenkins có kubeconfig credential chưa? | yes/no |
| Jenkins chạy được `kubectl get nodes` chưa? | yes/no |
| Helm deploy manual namespace nào? | preview-manual |
| Deployment image tag đúng chưa? | yes/no |
| Blocker lớn nhất cho ngày 3? | |

## Quyết định cuối ngày

Nếu Docker build/push chưa xong:

```text
Ngày 3 buổi đầu chỉ tập trung fix Docker build/push. Không làm Argo/Istio.
```

Nếu Jenkins chưa kết nối K3s:

```text
Ngày 3 TV1 và TV3 ưu tiên kubeconfig/kubectl/helm trên Jenkins agent.
```

Nếu Helm deploy được nhưng pod chưa Ready:

```text
Ngày 3 tập trung dependency/config/secret tối thiểu và developer_build.
```

Nếu cả 3 đều xong:

```text
Ngày 3 làm developer_build hoàn chỉnh, NodePort preview và cleanup.
```

## Checklist nộp cuối ngày 2 cho trưởng nhóm

TV1 gửi:

```text
[TV1 Day2]
K3s nodes:
Kubeconfig credential ID:
Jenkins kubectl access: yes/no
Blocker:
Screenshot/log:
```

TV2 gửi:

```text
[TV2 Day2]
Docker credential ID:
Branch built:
Changed service:
Commit SHA:
Docker image pushed:
Jenkins build status:
Blocker:
Screenshot/log:
```

TV3 gửi:

```text
[TV3 Day2]
Manual namespace:
Helm release:
Deployment image:
Pod status:
developer_build skeleton: yes/no
Blocker:
Screenshot/log:
```

TV4 gửi:

```text
[TV4 Day2]
Evidence collected:
Missing screenshots:
Test plan updated: yes/no
PASS cases:
FAIL/blocker cases:
```
