# Ngày 5 - Runbook chốt hạ cho cả 4 thành viên

Mục tiêu ngày 5: không mở thêm việc lớn. Hôm nay là ngày khóa demo, gom evidence, sửa lỗi nhỏ, và viết báo cáo theo đúng những gì đã làm được.

Trạng thái kỳ vọng đầu ngày 5:

- Jenkins cũ Project 1 vẫn dùng làm trung tâm CI/CD.
- Jenkins CI đã build/push image `yas-tax` với tag commit SHA và branch alias.
- Jenkins job `developer_build` deploy được preview `tax`.
- Jenkins job `developer_cleanup` xóa được namespace preview.
- K3s có 1 master, 1 worker.
- NodePort `30080` đã tạo được.
- Postgres runtime tối thiểu có thể chạy ở namespace `postgres`.
- Argo CD pod đã `1/1 Running` trong namespace `argocd`.

Không làm thêm những việc này trong ngày 5 nếu phần bắt buộc chưa đủ evidence:

- Không cố deploy full 14 service.
- Không đổi Jenkins cũ sang GitHub Actions.
- Không cài Jenkins mới.
- Không cài full observability Grafana/Prometheus/Tempo/Loki nếu đề không bắt buộc.
- Không cố sửa ApplicationSet nếu Argo CD Application thường đã đủ demo.
- Không làm Istio/Kiali nếu cluster bắt đầu thiếu tài nguyên.

## Lịch làm đề xuất trong ngày

| Khung giờ | Cả nhóm cần đạt |
|---|---|
| 0h-0h30 | Sync tình trạng thật, chốt scope demo cuối |
| 0h30-1h30 | TV1 kiểm tra K3s/NodePort/Argo CD; TV2 kiểm tra Jenkins CI Docker tag |
| 1h30-2h30 | TV3 chạy lại `developer_build` và `developer_cleanup`; TV4 chụp evidence |
| 2h30-3h30 | Nếu ổn: tạo Argo CD Application thường cho `dev`; nếu không ổn: bỏ Argo, khóa phần bắt buộc |
| 3h30-4h30 | Viết report, README, demo script |
| 4h30-5h00 | Tổng duyệt demo, backup screenshot/log |

## Quyết định đầu ngày 5

Cả nhóm chọn đúng 1 scope:

```text
Scope A - Chắc điểm bắt buộc:
Jenkins CI/CD + Docker Hub + K3s + developer_build + NodePort + cleanup.

Scope B - Bắt buộc + Argo CD:
Làm thêm Argo CD Application thường cho namespace dev.

Scope C - Bắt buộc + Argo CD + Istio:
Chỉ chọn nếu cluster còn khỏe và nhóm đã có đủ evidence bắt buộc.
```

Khuyến nghị: chọn Scope B. Argo CD hiện đã Running, nên làm Application thường là vừa sức. Istio chỉ làm nếu còn thời gian thật.

---

# Thành viên 1 - Infrastructure Lead

Nhiệm vụ ngày 5: giữ cluster ổn định, kiểm tra K3s, NodePort, firewall, Argo CD, và không để nhóm mất demo vì lỗi hạ tầng.

## Bước 1. Kiểm tra K3s 2 node

Chạy:

```bash
kubectl get nodes -o wide
kubectl get pods -A
```

Kỳ vọng:

```text
yas-master   Ready
yas-worker   Ready
```

Nếu worker `NotReady`, dừng Argo/Istio, fix worker trước.

## Bước 2. Kiểm tra namespace chính

```bash
kubectl get ns
kubectl get pods -n kube-system
kubectl get pods -n argocd
kubectl get pods -n postgres || true
kubectl get pods -n preview-demo || true
```

Kỳ vọng Argo CD:

```text
argocd-application-controller        1/1 Running
argocd-applicationset-controller     1/1 Running
argocd-dex-server                    1/1 Running
argocd-redis                         1/1 Running
argocd-repo-server                   1/1 Running
argocd-server                        1/1 Running
```

Nếu `argocd-applicationset-controller` lỗi lại nhưng các pod khác vẫn Running, không sa lầy. Demo Argo CD Application thường không cần ApplicationSet.

## Bước 3. Kiểm tra Postgres runtime

```bash
kubectl get svc -n postgres
kubectl get pods -n postgres
```

Kỳ vọng:

```text
service/postgresql
pod/postgresql-... Running
```

Nếu chưa có, báo TV3 chạy lại `developer_build` bản mới vì job đã có đoạn bootstrap Postgres. Nếu cần tạo tay để cứu demo, dùng lại YAML Postgres trong Day4.

## Bước 4. Kiểm tra NodePort và firewall

Sau khi TV3 chạy `developer_build`, kiểm tra:

```bash
kubectl get svc -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo || true
kubectl get pods -n preview-demo -o wide
```

Kỳ vọng có:

```text
yas-preview-nodeport   NodePort   ...   80:30080/TCP
```

Kiểm tra firewall GCP:

```bash
gcloud compute firewall-rules list --filter="name~yas"
```

Cần có rule mở `tcp:30080` cho IP demo. Nếu gấp:

```bash
gcloud compute firewall-rules update yas-allow-preview-nodeport \
  --source-ranges 0.0.0.0/0
```

Ghi vào report: mở rộng chỉ dùng khi demo, production phải giới hạn IP.

## Bước 5. Mở Argo CD UI

Chạy:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Mở:

```text
https://localhost:8080
```

Lấy password admin nếu cần:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath='{.data.password}' | base64 -d && echo
```

Nếu secret không tồn tại, báo nhóm. Không reset password khi chưa cần.

## Evidence TV1 phải có

Chụp hoặc lưu log:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl get pods -n argocd
kubectl get svc -n preview-demo
kubectl get svc -n postgres
```

TV1 xong khi cluster Ready, Argo CD Running, NodePort/firewall rõ ràng.

---

# Thành viên 2 - Jenkins CI & Container Lead

Nhiệm vụ ngày 5: chứng minh CI Project 1 được kế thừa và đã mở rộng thành build/push Docker image tag commit SHA.

## Bước 1. Kiểm tra Jenkinsfile trên main

Chạy local:

```bash
git checkout main
git pull
rg -n "Docker Build and Push|DOCKERHUB_USER|dockerhub-credentials|CHANGED_SERVICES|junit|jacoco|gitleaks|sonar|snyk" Jenkinsfile
```

Cần chứng minh:

- Jenkinsfile vẫn là Jenkins, không phải GitHub Actions mới.
- Có monorepo change detection.
- Có test/build.
- Có JUnit result.
- Có coverage.
- Có Docker build/push.
- Docker image tag gồm commit SHA và branch alias.
- Branch `main` push tag `main`.

Nếu `rg` chưa có, dùng:

```bash
grep -n "Docker Build and Push\\|DOCKERHUB_USER\\|dockerhub-credentials\\|CHANGED_SERVICES" Jenkinsfile
```

## Bước 2. Kiểm tra Docker Hub tag

Từ local:

```bash
git fetch origin dev_tax_service
TAX_SHA="$(git rev-parse --short=12 origin/dev_tax_service)"
echo "$TAX_SHA"
```

Vì máy Mac có thể là ARM, pull image linux/amd64 bằng:

```bash
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:$TAX_SHA
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:dev_tax_service
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:main
```

Nếu local Docker chưa login hoặc pull bị unauthorized, chụp Docker Hub UI thay thế. Cần thấy tag:

```text
af2dbde4f1a2 hoặc commit SHA hiện tại
dev_tax_service
main
```

## Bước 3. Chạy Jenkins CI branch demo nếu cần

Nếu cần evidence mới:

1. Vào Jenkins Multibranch job Project 1.
2. Chọn branch `dev_tax_service`.
3. Build Now hoặc Scan Multibranch Pipeline.
4. Chụp console các đoạn:

```text
Changed services: tax
Docker Build and Push
docker.io/doubleho/yas-tax:<commit-sha>
docker.io/doubleho/yas-tax:dev_tax_service
```

Không chạy build full service song song. Jenkins cũ chỉ khoảng 8GB RAM, build tuần tự và chỉ service thay đổi.

## Bước 4. Chuẩn bị câu giải thích SHA

Khi giảng viên hỏi “SHA là gì”, trả lời:

```text
SHA là Git commit id của commit cuối branch. Project 2 yêu cầu image của branch developer phải tag bằng commit id cuối cùng. Jenkins resolve branch bằng git rev-parse --short=12 origin/<branch>, rồi push Docker image tag đó lên Docker Hub.
```

## Evidence TV2 phải có

- Screenshot Jenkins CI branch `dev_tax_service`.
- Screenshot Docker Hub `yas-tax` có tag commit SHA.
- Screenshot Docker Hub `yas-tax:main`.
- Screenshot Jenkinsfile đoạn Docker build/push.
- Log test/build/coverage từ Project 1 nếu chưa đủ evidence.

TV2 xong khi có thể chứng minh CI cũ đã được mở rộng thành CI/CD image pipeline.

---

# Thành viên 3 - Jenkins CD & GitOps Lead

Nhiệm vụ ngày 5: chạy lại `developer_build`, chạy lại `developer_cleanup`, và nếu đủ thời gian tạo Argo CD Application thường cho `dev`.

## Bước 1. Kiểm tra `developer_build` script

Trong Jenkins job `developer_build`, script phải có các ý chính:

```text
git branch: 'main', url: 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
set -eu
resolve_tag()
kubectl create namespace "$NAMESPACE"
bootstrap Postgres ở namespace postgres
helm upgrade --install yas-configuration
helm upgrade --install tax
--set backend.serviceMonitor.enabled=false
kubectl apply NodePort yas-preview-nodeport
```

Không được có:

```text
checkout scm
set -euo pipefail
YAML NodePort nằm ngoài sh block
kubectl delete namespace postgres trong developer_cleanup
```

## Bước 2. Chạy `developer_build`

Vào Jenkins:

```text
developer_build
Build with Parameters
```

Nhập:

```text
ENV_NAME=demo
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=main
CART_BRANCH=main
ORDER_BRANCH=main
CUSTOMER_BRANCH=main
INVENTORY_BRANCH=main
MEDIA_BRANCH=main
SEARCH_BRANCH=main
STOREFRONT_BFF_BRANCH=main
STOREFRONT_UI_BRANCH=main
BACKOFFICE_BFF_BRANCH=main
BACKOFFICE_UI_BRANCH=main
```

Kỳ vọng console:

```text
Namespace: preview-demo
TAX_TAG=<commit-sha>
Ensuring shared Postgres runtime exists at postgresql.postgres
Ensuring yas-configuration exists in preview-demo
Deploying tax image=docker.io/doubleho/yas-tax:<commit-sha>
```

## Bước 3. Kiểm tra sau deploy

Chạy local:

```bash
kubectl get ns preview-demo
kubectl get pods -n preview-demo
kubectl get svc -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo || true
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng image:

```text
docker.io/doubleho/yas-tax:<commit-sha>
```

Nếu pod chưa Ready, lấy log:

```bash
kubectl describe pod -n preview-demo -l app.kubernetes.io/name=tax
kubectl logs -n preview-demo deploy/tax --tail=100 || true
```

Nếu lỗi là app dependency/runtime, vẫn giữ evidence image tag đúng. Nhưng nếu đã có Postgres/config thì cố chờ:

```bash
kubectl rollout status deployment/tax -n preview-demo --timeout=180s || true
kubectl get pods -n preview-demo
```

## Bước 4. Test NodePort

Nếu pod Ready và endpoint có IP:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080
curl -I http://<WORKER_EXTERNAL_IP>:30080/tax/actuator/health || true
```

Nếu curl fail, kiểm tra 3 điều:

```bash
kubectl get endpoints yas-preview-nodeport -n preview-demo
kubectl get pods -n preview-demo
gcloud compute firewall-rules list --filter="name~yas"
```

Ghi nhớ: NodePort tạo thành công là evidence CD expose. Curl chỉ thành công khi app route đúng và pod Ready.

## Bước 5. Chạy `developer_cleanup`

Vào Jenkins job:

```text
developer_cleanup
Build with Parameters
```

Nhập:

```text
ENV_NAME=demo
CONFIRM_DELETE=DELETE
```

Kỳ vọng console:

```text
kubectl delete namespace preview-demo --ignore-not-found=true
namespace "preview-demo" deleted
Finished: SUCCESS
```

Kiểm tra:

```bash
kubectl get ns preview-demo
kubectl get ns postgres
```

Kỳ vọng:

```text
preview-demo không còn
postgres vẫn còn
```

## Bước 6. Tạo Argo CD Application thường cho `dev`

Chỉ làm nếu phần Jenkins CD đã có evidence.

Tạo namespace:

```bash
kubectl create namespace dev --dry-run=client -o yaml | kubectl apply -f -
```

Tạo file tạm `argocd-tax-dev.yaml` ngoài repo hoặc trong thư mục evidence:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: yas-tax-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/hoanghaitapcode/DevOps_Lab1.git
    targetRevision: main
    path: k8s/charts/tax
    helm:
      parameters:
        - name: backend.image.repository
          value: docker.io/doubleho/yas-tax
        - name: backend.image.tag
          value: main
        - name: backend.serviceMonitor.enabled
          value: "false"
  destination:
    server: https://kubernetes.default.svc
    namespace: dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

Apply:

```bash
kubectl apply -f argocd-tax-dev.yaml
kubectl get application -n argocd
kubectl describe application yas-tax-dev -n argocd
```

Nếu Argo CD báo thiếu `yas-configuration` hoặc Postgres trong namespace `dev`, đừng sa lầy. Ghi rõ:

```text
Argo CD Application đã quản lý Helm chart tax ở namespace dev. Runtime dependency giống preview cần được bootstrap riêng.
```

## Evidence TV3 phải có

- Screenshot Jenkins `developer_build` SUCCESS.
- Console có `TAX_TAG=<commit-sha>`.
- Console có `Deploying tax image=...:<commit-sha>`.
- Screenshot `kubectl get deployment tax ... image`.
- Screenshot `kubectl get svc -n preview-demo` có NodePort.
- Screenshot Jenkins `developer_cleanup` SUCCESS.
- Nếu làm Argo: screenshot Argo CD UI hoặc `kubectl get application -n argocd`.

TV3 xong khi demo CD bắt buộc có thể chạy lại được từ đầu.

---

# Thành viên 4 - Service Mesh & QA/Report Lead

Nhiệm vụ ngày 5: gom bằng chứng, viết report, chuẩn bị demo script, và quyết định có làm Istio/Kiali hay không.

## Bước 1. Tạo checklist evidence cuối

Tạo thư mục:

```text
evidence/final/
  01-project1-ci/
  02-k3s/
  03-dockerhub/
  04-developer-build/
  05-nodeport/
  06-cleanup/
  07-argocd/
  08-istio-if-any/
```

## Bước 2. Checklist bắt buộc phải có

Project 1 kế thừa:

```text
GitHub repo fork YAS
Branch protection main
2 reviewer approve
CI pass required
Jenkins Multibranch
Jenkins test/build
Test result
Coverage report
Monorepo change detection
Gitleaks/Sonar/Snyk nếu nhóm đã làm
```

Project 2 bắt buộc:

```text
K3s master/worker Ready
Docker Hub image tag main
Docker Hub image tag commit SHA
Jenkins developer_build parameters
developer_build SUCCESS
Deployment image dùng commit SHA
NodePort service 30080
hosts file/domain strategy
developer_cleanup SUCCESS
```

Nâng cao:

```text
Argo CD pods Running
Argo CD Application dev nếu có
Istio/Kiali nếu có
```

## Bước 3. Demo script cho giảng viên

Đọc theo thứ tự này khi demo:

```text
1. Đây là Jenkins cũ từ Project 1, nhóm không cài Jenkins mới.
2. Project 2 mở rộng Jenkins cũ thành CI/CD.
3. Khi developer push branch dev_tax_service, Jenkins chỉ build service tax bị thay đổi.
4. Jenkins push Docker image docker.io/doubleho/yas-tax:<commit-sha>.
5. Job developer_build cho developer nhập TAX_BRANCH=dev_tax_service, service khác để main.
6. Job resolve branch thành commit SHA và deploy image đó lên K3s.
7. K3s chạy trên Google Cloud gồm yas-master và yas-worker.
8. Preview expose bằng NodePort 30080.
9. Developer tự thêm yas-preview.local vào hosts trỏ về Worker external IP.
10. Job developer_cleanup xóa preview namespace sau khi test.
11. Argo CD được cài ở namespace argocd để quản lý dev/staging nâng cao.
```

## Bước 4. Câu trả lời sẵn cho các câu hỏi khó

Nếu hỏi “Sao không dùng GitHub Actions?”:

```text
Đề yêu cầu Project 2 kế thừa Project 1. Project 1 đã dùng Jenkins Multibranch CI, nên Project 2 mở rộng Jenkins đó thành CI/CD thay vì thay hệ thống CI.
```

Nếu hỏi “Sao Jenkins không chạy workload YAS?”:

```text
Jenkins cũ chỉ khoảng 8GB RAM, nên Jenkins chỉ điều phối CI/CD, build service thay đổi, push image, và gọi kubectl/helm. Workload runtime chạy trên K3s ở Google Cloud.
```

Nếu hỏi “Sao có 2 tag Docker?”:

```text
Commit SHA tag là tag chính theo yêu cầu đề. Branch alias như dev_tax_service chỉ là tag phụ để developer dễ nhìn trên Docker Hub.
```

Nếu hỏi “Sao NodePort có nhưng curl fail?”:

```text
NodePort chỉ tạo đường vào cluster. Curl chỉ thành công khi pod phía sau service Ready và endpoint tồn tại. Lúc tax thiếu PostgreSQL runtime, image deploy vẫn đúng nhưng app chưa Ready.
```

Nếu hỏi “Sao vẫn cần yas-postgresql-credentials-secret dù dùng Postgres tự dựng?”:

```text
Vì Helm chart tax/backend khai báo envFrom Secret tên yas-postgresql-credentials-secret với Optional false. Dù database thật là Postgres tự dựng, pod vẫn cần Secret đúng tên để lấy username/password.
```

Nếu hỏi “Argo CD ApplicationSet dùng không?”:

```text
Nhóm dùng Argo CD Application thường cho dev/staging. ApplicationSet không bắt buộc trong scope demo.
```

## Bước 5. README/report phần kết luận

Viết đoạn này vào report:

```text
Project 2 kế thừa Jenkins CI từ Project 1. Jenkins cũ tiếp tục quét branch, chạy test/build/coverage/security scan và được mở rộng thêm Docker build/push image. Với mỗi branch developer, Jenkins build image cho service thay đổi và tag bằng commit SHA cuối branch. Job developer_build nhận parameter branch cho từng service, resolve branch thành image tag, sau đó deploy preview vào K3s bằng Helm/kubectl. Preview được expose qua NodePort và developer có thể trỏ domain local trong hosts file về Worker node. Job developer_cleanup xóa namespace preview sau khi test.
```

## Bước 6. Istio/Kiali chỉ làm nếu còn thời gian

Nếu phần bắt buộc và Argo đã đủ evidence, có thể làm Istio tối thiểu:

```bash
kubectl get ns istio-system
kubectl get pods -n istio-system
kubectl get svc -n istio-system
```

Nếu chưa cài Istio, cân nhắc dừng. Cài Istio ngày cuối có rủi ro cao vì cluster chỉ 1 master/1 worker.

Nếu đã cài, evidence tối thiểu:

```text
PeerAuthentication STRICT mTLS
AuthorizationPolicy allow/deny
VirtualService retry
Kiali topology screenshot
curl allow/deny logs
```

Không có Istio vẫn nộp được phần bắt buộc + Argo.

## Evidence TV4 phải có

- Folder evidence đầy đủ.
- Demo script.
- README hoặc report cập nhật.
- Screenshot không lộ secret.
- Bảng phân công 4 người.
- Bảng scope đã hoàn thành/chưa hoàn thành.

TV4 xong khi nhóm có thể mở report và demo không cần nghĩ thêm.

---

# Tổng duyệt cuối ngày

Chạy các lệnh này trước khi nộp:

```bash
kubectl get nodes -o wide
kubectl get pods -n argocd
kubectl get pods -n postgres || true
kubectl get pods -n preview-demo || true
kubectl get svc -n preview-demo || true
kubectl get application -n argocd || true
```

Kiểm tra Jenkins:

```text
Project 1 Multibranch CI còn chạy được
developer_build SUCCESS
developer_cleanup SUCCESS
```

Kiểm tra Docker Hub:

```text
doubleho/yas-tax:main
doubleho/yas-tax:<commit-sha>
doubleho/yas-tax:dev_tax_service
```

## Scope cắt giảm nếu trễ

Thứ tự cắt:

1. Cắt Istio/Kiali.
2. Cắt ApplicationSet, chỉ dùng Argo CD Application thường.
3. Cắt deploy full storefront flow, chỉ demo `tax`.
4. Cắt curl NodePort nếu app runtime chưa Ready, nhưng giữ evidence NodePort + deployment image tag đúng.

Tuyệt đối không cắt:

- Jenkins cũ kế thừa Project 1.
- Docker image commit SHA.
- `developer_build`.
- `developer_cleanup`.
- K3s master/worker.

## Checklist nộp bài

| Hạng mục | Trạng thái |
|---|---|
| Jenkins cũ Project 1 được tái sử dụng | yes/no |
| K3s 1 master 1 worker | yes/no |
| Docker Hub có tag `main` | yes/no |
| Docker Hub có tag commit SHA | yes/no |
| `developer_build` có parameter branch | yes/no |
| `developer_build` deploy đúng image SHA | yes/no |
| NodePort `30080` có evidence | yes/no |
| Hosts/domain strategy có ghi trong report | yes/no |
| `developer_cleanup` SUCCESS | yes/no |
| Argo CD pods Running | yes/no |
| Argo CD Application dev nếu có | yes/no |
| Istio/Kiali nếu có | yes/no |
| Screenshot/log đã gom | yes/no |
| Report/README hoàn tất | yes/no |

## Câu chốt demo

```text
Nhóm hoàn thành phần bắt buộc bằng cách kế thừa Jenkins CI từ Project 1 và mở rộng thành CI/CD. Jenkins build image theo service thay đổi, tag bằng commit SHA, push Docker Hub, sau đó developer_build deploy preview lên K3s ở Google Cloud. Preview expose bằng NodePort và cleanup bằng Jenkins job riêng. Phần nâng cao nhóm cài Argo CD để chuẩn bị GitOps cho dev/staging.
```
