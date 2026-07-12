# End-to-End Demo Test Flows cho Project 1 + Project 2

File này dùng để chạy demo thực tế trước giảng viên. Mục tiêu là đi từ Project 1 CI đến Project 2 CD, GitOps, web, observability và service mesh. Khi demo, ưu tiên chạy các luồng ít rủi ro trước, sau đó mới chạy các luồng live như push branch, merge main hoặc release tag.

## 0. Chuẩn bị trước khi thầy xem

### Tab nên mở sẵn

Mở sẵn các tab này để không mất thời gian:

1. GitHub source repo: `https://github.com/hoanghaitapcode/DevOps_Lab1`
2. GitHub GitOps repo: `https://github.com/DoubleHo05/yas-deployment`
3. Jenkins job/folder: `YAS`
4. Jenkins job: `developer_build`
5. Jenkins cleanup job preview
6. Docker Hub user/repo: `doubleho/yas-tax`, `doubleho/yas-product`, hoặc service đang demo
7. Argo CD UI
8. Grafana UI
9. Kiali UI nếu demo service mesh
10. Terminal có `kubectl`, `git`, `helm`, `istioctl`

### Thông tin cần nhớ

```text
Source repo:  https://github.com/hoanghaitapcode/DevOps_Lab1
GitOps repo:  https://github.com/DoubleHo05/yas-deployment
Docker Hub:   docker.io/doubleho
Cluster:      K3s, 1 master + 1 worker
Worker IP:    35.198.213.72
Dev ns:       dev
Staging ns:   staging
Argo ns:      argocd
Observability ns: observability
Service mesh ns:  mesh-demo
```

### Demo order đề xuất

```text
1. Kiểm tra cluster và dependency.
2. Project 1: GitHub branch protection + PR + Jenkins multibranch CI.
3. Project 1: Service change detection, test/build, report, security scan.
4. Project 2: Branch commit -> Jenkins build image -> Docker Hub tag SHA/branch.
5. Project 2: Merge main -> Jenkins update GitOps dev -> Argo CD sync dev.
6. Project 2: Mở web storefront bằng domain local + NodePort.
7. Project 2: developer_build preview + cleanup.
8. Project 2 nâng cao: release tag -> staging.
9. Observability: Prometheus, Loki, Tempo trong Grafana.
10. Service mesh: Istio mTLS, retry, AuthorizationPolicy, Kiali topology.
```

## 1. Luồng Sanity Check Cluster

### Mục tiêu

Chứng minh cluster K3s, namespace, dependency nền và workload chính đang tồn tại.

### Lệnh chạy

```bash
kubectl get nodes -o wide
```

Kỳ vọng:

```text
yas-master Ready
yas-worker Ready
```

```bash
kubectl get ns
```

Kỳ vọng có:

```text
argocd
dev
staging
postgres
kafka
elasticsearch
redis
keycloak
observability
mesh-demo
```

```bash
kubectl get pods -n postgres
```

```bash
kubectl get pods -n kafka
```

```bash
kubectl get elasticsearch -n elasticsearch
```

```bash
kubectl get pods -n keycloak
```

```bash
kubectl get pods -n dev
```

```bash
kubectl get applications -n argocd
```

### Cách giải thích

Nói ngắn gọn:

```text
Nhóm dùng K3s trên Google Cloud với 1 master và 1 worker. Các dependency nền như PostgreSQL, Kafka, Elasticsearch, Redis, Keycloak được deploy trước, sau đó các microservice YAS trong namespace dev/staging kết nối tới dependency bằng Kubernetes DNS nội bộ.
```

### Screenshot cần chụp

- `kubectl get nodes -o wide`
- `kubectl get applications -n argocd`
- `kubectl get pods -n dev`
- `kubectl get pods -A | grep postgres|kafka|elastic|keycloak|redis`

## 2. Project 1 - GitHub Branch Protection và PR

### Mục tiêu

Chứng minh `main` không được push trực tiếp, PR cần 2 approval và Jenkins check phải pass.

### Demo trên GitHub UI

Vào:

```text
GitHub repo -> Settings -> Branches -> Branch protection rules -> main
```

Chỉ cho thầy các setting:

```text
Require a pull request before merging
Require approvals: 2
Require status checks to pass before merging
Không cho push trực tiếp vào main
```

Sau đó mở một PR đang có hoặc tạo PR demo mới.

### Tạo PR demo live

Nếu cần tạo PR live, chạy từ máy local:

```bash
git fetch origin main
```

```bash
git switch -c viva-tax-demo-0711 origin/main
```

```bash
printf "demo viva 2026-07-11\n" >> tax/demo-viva.txt
```

```bash
git add tax/demo-viva.txt
```

```bash
git commit -m "test: viva tax ci flow"
```

```bash
git push -u origin viva-tax-demo-0711
```

Sau đó mở GitHub:

```text
Compare & pull request -> base main -> compare viva-tax-demo-0711
```

### Kỳ vọng trên PR

PR phải hiện:

```text
Review required
2 approving reviews required
Jenkins check đang chạy hoặc đã pass
Không thể merge nếu thiếu approval/check fail
```

### Cách giải thích

```text
Project 1 yêu cầu bảo vệ main. Nhóm cấu hình GitHub branch protection để mọi thay đổi phải đi qua PR, cần ít nhất 2 reviewer approve và Jenkins CI pass thì mới merge được.
```

## 3. Project 1 - Jenkins Multibranch Pipeline

### Mục tiêu

Chứng minh Jenkins tự scan branch/PR/tag và tạo job tương ứng.

### Demo trên Jenkins UI

Vào:

```text
Jenkins -> YAS -> Configure
```

Chỉ cho thầy:

```text
Branch Sources: GitHub
Repository: hoanghaitapcode/DevOps_Lab1
Credentials: GitHub credential
Behaviours:
  Discover branches
  Discover pull requests from origin
  Discover pull requests from forks nếu có
  Discover tags
Build strategies:
  Regular branches
  Change requests / PR
  Tags
Script Path: Jenkinsfile
```

### Nếu thầy hỏi build strategy hoạt động thế nào

Trả lời:

```text
Build strategy quyết định SCM head nào được build. Branch strategy cho phép Jenkins build các branch developer. PR strategy cho phép Jenkins build pull request để status check hiện trên GitHub. Tag strategy cho phép Jenkins build release tag như v1.0.0, dùng cho staging.
```

Lưu ý quan trọng:

```text
Lúc trước trường "Ignore tags older than" để 0 nên Jenkins scan thấy tag nhưng không build. Sau khi bỏ 0, Jenkins build release tag bình thường.
```

### Demo scan

Trong Jenkins:

```text
YAS -> Scan Repository Now
```

Kỳ vọng scan log có branch/PR/tag mới.

### Screenshot cần chụp

- Jenkins Configure phần Branch Sources
- Jenkins Configure phần Build Strategies
- Jenkins scan log
- Jenkins branch/PR job được tạo

## 4. Project 1 - Detect Service Changed và CI Chỉ Build Service Đổi

### Mục tiêu

Chứng minh monorepo change detection hoạt động. Khi đổi `tax/`, Jenkins chỉ build/test/push `tax`, không build toàn bộ YAS.

### Vị trí code cần mở cho thầy

Mở `Jenkinsfile`, chỉ các đoạn:

```groovy
JAVA_SERVICES = 'cart,customer,inventory,...,tax,...'
UI_SERVICES = 'storefront,backoffice'
```

Đoạn detect changed files:

```groovy
git diff --name-only origin/${env.CHANGE_TARGET}...HEAD
git diff --name-only HEAD~1 HEAD
```

Đoạn map path sang service:

```groovy
if (changedFiles.any { it.startsWith("${service}/") }) {
    changedJavaServices.add(service)
}
```

### Cách nói khi bị hỏi có dùng changeset không

```text
Nhóm không dùng Jenkins Declarative changeset. Nhóm tự dùng git diff --name-only để lấy danh sách file thay đổi, sau đó map prefix thư mục sang service trong monorepo. Cách này chủ động hơn vì YAS là monorepo nhiều service.
```

### Demo trên Jenkins Console

Mở build của branch/PR demo, tìm stage:

```text
Detect Changed Services
```

Kỳ vọng thấy:

```text
Changed files:
tax/demo-viva.txt

Services cần build: tax
Java services: tax
UI services: none
```

Sau đó chỉ các stage:

```text
Build Common Library
Test
Build
Docker Build and Push
SonarQube Analysis
Quality Gate
Snyk Security Scan
```

### Test result và coverage

Trong Jenkins build:

```text
Build page -> Test Result
Build page -> Coverage/Jacoco hoặc SonarCloud coverage
```

Nếu thầy hỏi coverage 70%:

```text
Coverage được publish qua JaCoCo/SonarCloud. Ngưỡng 70% được enforce bằng SonarCloud Quality Gate. Jenkins stage Quality Gate sẽ fail nếu SonarCloud trả về FAILED.
```

### Screenshot cần chụp

- Jenkins console đoạn `Changed files`
- Jenkins console đoạn `Services cần build: tax`
- Stage View có Test/Build/Docker/Sonar/Snyk
- Test Result
- SonarCloud coverage/Quality Gate

## 5. Project 1 - Security Scan: Gitleaks, SonarCloud, Snyk

### Mục tiêu

Chứng minh nhóm có security scan và biết xử lý khi phát hiện leak.

### Gitleaks

Mở trong repo:

```text
gitleaks.toml
.github/workflows/gitleaks.yml
.github/workflows/gitleaks-check.yaml
```

Nói rõ:

```text
Phần Gitleaks là bằng chứng/lab Project 1 cũ, dùng để scan secret leak. Với Project 2, nhóm không thêm GitHub Actions mới cho CD, Jenkins vẫn là CI/CD chính.
```

Nếu thầy hỏi leak credentials thì làm gì:

```text
Nếu Gitleaks phát hiện secret thật, nhóm phải revoke/rotate secret ngay, xóa secret khỏi code/history nếu cần, chuyển sang Jenkins Credentials hoặc Kubernetes Secret, rồi chạy lại scan. Không được chỉ allowlist secret thật. Chỉ allowlist false positive có lý do rõ ràng.
```

### SonarCloud

Mở:

```text
SonarCloud project -> Project Overview -> Quality Gate -> Coverage
```

Mở trong Jenkinsfile:

```groovy
withSonarQubeEnv('SonarCloud')
waitForQualityGate abortPipeline: true
```

Nếu thầy hỏi “Sonar có cài cái gateway gì không?”:

```text
Ý đó là Quality Gate. Nhóm dùng SonarCloud/SonarQube Quality Gate để đặt điều kiện pass/fail, ví dụ coverage, bugs, vulnerabilities, code smells. Jenkins dùng waitForQualityGate để chờ kết quả và fail pipeline nếu Quality Gate fail.
```

### Snyk

Mở Jenkinsfile:

```groovy
snyk test --file=${svc}/pom.xml --severity-threshold=high
snyk monitor --file=${svc}/pom.xml
```

Mở Snyk UI:

```text
Snyk -> Project -> Issues/Dependencies/Monitor
```

Nói rõ:

```text
Snyk dùng để scan dependency vulnerability. Jenkins dùng token từ Jenkins Credentials ID snyk-token, không hardcode token trong Jenkinsfile.
```

## 6. Project 2 - Branch Commit -> Jenkins Build/Push DockerHub

### Mục tiêu

Chứng minh yêu cầu: mỗi branch developer commit code thì Jenkins build image service đổi, tag bằng commit SHA cuối branch, push Docker Hub.

### Demo live bằng branch tax

Nếu đã tạo branch ở phần PR thì dùng luôn `viva-tax-demo-0711`. Nếu chưa có:

```bash
git fetch origin main
```

```bash
git switch -c viva-tax-demo-0711 origin/main
```

```bash
printf "demo docker push 2026-07-11\n" >> tax/demo-docker-push.txt
```

```bash
git add tax/demo-docker-push.txt
```

```bash
git commit -m "test: trigger tax docker push"
```

```bash
git push -u origin viva-tax-demo-0711
```

### Jenkins kỳ vọng

Mở Jenkins branch job:

```text
YAS -> viva-tax-demo-0711
```

Trong console phải thấy:

```text
Docker Build and Push
docker build -t docker.io/doubleho/yas-tax:<commitSha> -t docker.io/doubleho/yas-tax:viva-tax-demo-0711 ./tax
docker push docker.io/doubleho/yas-tax:<commitSha>
docker push docker.io/doubleho/yas-tax:viva-tax-demo-0711
```

### Kiểm tra commit SHA

Trên local branch:

```bash
git rev-parse --short=12 HEAD
```

Mở Docker Hub:

```text
doubleho/yas-tax -> Tags
```

Kỳ vọng có 2 tag:

```text
<commit-sha-12>
viva-tax-demo-0711
```

### Giải thích digest khác SHA

Nếu thầy hỏi DockerHub digest khác commit SHA:

```text
Commit SHA là Git commit id dùng làm image tag. Docker digest là sha256 của nội dung image sau khi build. Hai giá trị này khác nhau là đúng. Nhóm push image với tag là commit SHA, còn DockerHub tự sinh digest cho image content.
```

## 7. Project 2 - Merge Main -> GitOps Dev -> Argo CD Sync

### Mục tiêu

Chứng minh luồng dev:

```text
Merge PR vào main
-> Jenkins main build/push image
-> Jenkins update envs/dev/<service>-values.yaml trong GitOps repo
-> Argo CD auto sync namespace dev
```

### Demo live

Trên GitHub PR:

```text
Đợi Jenkins check pass
Đủ 2 approvals
Merge PR vào main
```

Mở Jenkins:

```text
YAS -> main -> build mới nhất
```

Trong console tìm:

```text
Docker Build and Push
Update GitOps Manifests
Updating envs/dev/tax-values.yaml -> tag <commitSha>
git push GitOps repo
```

Nếu lúc demo không thấy stage `Update GitOps Manifests`, không đoán. Mở đúng Jenkinsfile trên branch `main` hoặc dùng Jenkins build console đã chạy thành công để chứng minh.

### Kiểm tra GitOps repo

```bash
cd /tmp/yas-deployment
```

```bash
git pull origin main
```

```bash
git log --oneline -5
```

```bash
sed -n '1,35p' envs/dev/tax-values.yaml
```

Kỳ vọng:

```yaml
backend:
  image:
    repository: docker.io/doubleho/yas-tax
    tag: <commit-sha-12>
```

### Kiểm tra Argo CD/Kubernetes

```bash
kubectl get app yas-tax-dev -n argocd
```

```bash
kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
docker.io/doubleho/yas-tax:<commit-sha-12>
```

### Cách giải thích

```text
Jenkins vẫn là CI/CD chính. Jenkins build và push image, sau đó cập nhật GitOps repo. Argo CD không build image, Argo CD chỉ theo dõi GitOps repo và sync state mong muốn vào Kubernetes. GitOps repo là source of truth cho dev/staging.
```

## 8. Project 2 - Mở Web Storefront bằng Domain Local + NodePort

### Mục tiêu

Chứng minh ứng dụng chạy được và developer truy cập bằng domain name:port theo yêu cầu đồ án.

### Kiểm tra hosts file

Trên máy local cần có:

```text
35.198.213.72 storefront.yas.local.com
35.198.213.72 backoffice.yas.local.com
35.198.213.72 identity.yas.local.com
```

Trên macOS sửa bằng:

```bash
sudo nano /etc/hosts
```

### Kiểm tra ingress-nginx NodePort

```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

Kỳ vọng thấy port kiểu:

```text
80:30303/TCP
```

### Test API

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

Kỳ vọng:

```text
HTTP/1.1 200 OK
productList có iPhone 15, iPhone 15 Pro, Dell XPS, iPad...
```

### Mở browser

Mở:

```text
http://storefront.yas.local.com:30303
```

Search thử:

```text
iPhone
```

### Nếu thầy hỏi vì sao chung một IP

Trả lời:

```text
Vì không có DNS thật nên cả storefront, backoffice, identity đều trỏ về cùng IP worker. Ingress-nginx nghe NodePort 30303 và route dựa trên Host header. Cùng IP nhưng Host khác nhau sẽ đi tới service khác nhau.
```

### Nếu hỏi dùng IP trực tiếp được không

```text
Không nên. Nếu dùng thẳng IP thì browser không gửi Host header storefront.yas.local.com, Ingress không biết route đúng host nào. Vì vậy yêu cầu đồ án mới nói developer thêm domain vào hosts file.
```

## 9. Project 2 - developer_build Preview

### Mục tiêu

Chứng minh Jenkins job `developer_build` cho developer nhập branch/tag từng service để deploy preview. Service không nhập dùng default `main/latest`.

### Demo trên Jenkins UI

Mở:

```text
Jenkins -> developer_build -> Build with Parameters
```

Chỉ cho thầy parameter:

```text
ENV_NAME
TAX_TAG hoặc tax-service parameter
PRODUCT_TAG
ORDER_TAG
...
```

Ví dụ nhập:

```text
ENV_NAME=demo
TAX_TAG=viva-tax-demo-0711 hoặc <commit-sha-12>
Các service khác để trống hoặc main
```

### Khi job chạy xong

```bash
kubectl get ns preview-demo
```

```bash
kubectl get pods -n preview-demo -o wide
```

```bash
kubectl get deploy tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
docker.io/doubleho/yas-tax:viva-tax-demo-0711
```

Kiểm tra service NodePort:

```bash
kubectl get svc -n preview-demo -o wide
```

Nếu job in domain/port trong console, chụp lại dòng đó.

### Cách giải thích nếu preview dependency chưa hoàn hảo

```text
developer_build dùng để chứng minh cơ chế preview theo parameter: chọn tag cho service đang sửa, các service còn lại dùng main/latest, deploy vào namespace preview riêng và expose bằng NodePort/domain local. Môi trường full web ổn định chính của nhóm là dev/staging do Argo CD quản lý.
```

## 10. Project 2 - Cleanup Preview Job

### Mục tiêu

Chứng minh có Jenkins job xóa preview sau khi developer test xong.

### Demo Jenkins UI

Mở cleanup job:

```text
Jenkins -> cleanup preview job -> Build with Parameters
```

Nhập:

```text
ENV_NAME=demo
```

Job kỳ vọng chạy:

```text
kubectl delete namespace preview-demo
```

### Kiểm tra

```bash
kubectl get ns preview-demo
```

Kỳ vọng:

```text
NotFound
```

Hoặc namespace đang `Terminating` là vẫn chứng minh cleanup đã chạy.

## 11. Project 2 - Release Tag -> Staging

### Mục tiêu

Chứng minh nâng cao Argo CD staging:

```text
Push release tag vX.Y.Z
-> Jenkins build/push image tag vX.Y.Z
-> Jenkins update envs/staging/<service>-values.yaml
-> Argo CD sync namespace staging
```

### Chọn tag mới

Không dùng lại tag đã tồn tại. Ví dụ nếu đã có `v1.0.0`, dùng `v1.0.1` hoặc `v1.0.2`.

Kiểm tra tag tồn tại chưa:

```bash
git ls-remote --tags origin v1.0.2
```

Nếu không in gì là chưa có.

### Tạo và push tag

```bash
git fetch origin main --tags
```

```bash
git tag -a v1.0.2 origin/main -m "release: v1.0.2 staging demo"
```

```bash
git push origin v1.0.2
```

### Jenkins kiểm tra

Mở:

```text
Jenkins -> YAS -> tag v1.0.2
```

Nếu chưa tự chạy:

```text
YAS -> Scan Repository Now
```

Console kỳ vọng:

```text
Docker push docker.io/doubleho/yas-tax:v1.0.2
Update GitOps Manifests
envs/staging/tax-values.yaml -> tag v1.0.2
```

### Kiểm tra GitOps staging

```bash
cd /tmp/yas-deployment
```

```bash
git pull origin main
```

```bash
sed -n '1,35p' envs/staging/tax-values.yaml
```

Kỳ vọng:

```yaml
tag: v1.0.2
```

### Kiểm tra Kubernetes staging

```bash
kubectl get app yas-tax-staging -n argocd
```

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
docker.io/doubleho/yas-tax:v1.0.2
```

## 12. Argo CD UI Demo

### Mục tiêu

Chứng minh Argo CD đang quản lý dev/staging bằng Application CRD.

### Mở Argo UI

Nếu chưa mở:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Mở browser:

```text
https://localhost:8080
```

Login admin/password từ secret nếu cần:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d
```

### App cần mở

```text
yas-root-dev
yas-root-staging
yas-tax-dev
yas-tax-staging
yas-product-dev
yas-storefront-ui-dev
```

### Chỉ cho thầy

Trong app detail:

```text
Health: Healthy
Sync: Synced
Auto sync enabled
Repo/path/target revision
Deployment -> ReplicaSet -> Pod
Service
Ingress nếu có
```

### Nếu thầy hỏi tag xem ở đâu

Trong Argo UI:

```text
Application -> Deployment tax -> mở manifest/live manifest -> image
```

Hoặc terminal:

```bash
kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

### Cách giải thích Argo CD

```text
Argo CD không build image. Jenkins build/push image và update GitOps repo. Argo CD đọc Application CRD, render Helm chart/values rồi sync resource thật như Deployment, Service, Ingress vào namespace dev/staging.
```

## 13. Helm Chart Code Demo

### Mục tiêu

Chứng minh nhóm hiểu chart/template trong code.

### File cần mở

```text
k8s/charts/backend/templates/deployment.yaml
k8s/charts/backend/templates/service.yaml
k8s/charts/backend/templates/ingress.yaml
k8s/charts/backend/templates/serviceaccount.yaml
k8s/charts/tax/Chart.yaml
k8s/charts/tax/values.yaml
k8s/charts/ui/
```

### Giải thích nhanh

```text
k8s/charts/tax là application chart cho service tax.
Chart tax phụ thuộc chart backend để tái sử dụng template chung cho Java backend service.
deployment.yaml tạo Deployment và pod chạy container.
service.yaml tạo Service ổn định để các pod/service khác gọi.
ingress.yaml tạo rule route HTTP theo host/path.
serviceaccount.yaml tạo identity cho pod.
```

Nếu thầy hỏi `type: application`:

```text
Application chart là Helm chart có thể deploy ra resource Kubernetes thật. Ví dụ chart tax render ra Deployment, Service, Ingress, ServiceAccount cho tax.
```

Nếu thầy hỏi Service trong K8s:

```text
Service tạo địa chỉ ổn định và load balancing tới pod. Pod IP thay đổi được, còn Service DNS ổn định. Các type phổ biến là ClusterIP, NodePort, LoadBalancer, ExternalName.
```

## 14. Observability Demo - Prometheus, Loki, Tempo, Grafana

### Mục tiêu

Chứng minh nhóm deploy được observability stack và xem được metrics/logs/traces.

### Kiểm tra pod

```bash
kubectl get pods -n observability
```

Kỳ vọng:

```text
prometheus
grafana
loki
promtail
tempo
opentelemetry-collector
Running
```

Nếu Grafana bị CrashLoop do Postgres, kiểm tra Postgres trước. Khi ổn thì Grafana phải Running.

### Mở Grafana

```bash
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Mở:

```text
http://localhost:3000
```

Lấy password nếu cần:

```bash
kubectl --namespace observability get secrets prometheus-grafana -o jsonpath="{.data.admin-password}" | base64 -d
```

### Demo metrics với Prometheus

Grafana:

```text
Explore -> chọn Prometheus -> query: up -> Run query
```

Kỳ vọng nhiều series value `1`.

Giải thích:

```text
Metric là số đo theo thời gian. Prometheus query up trả về 1 nghĩa là target scrape được và đang up, 0 nghĩa là target down hoặc không scrape được.
```

Tạo dashboard đơn giản:

```text
Dashboards -> New dashboard -> Add visualization hoặc nút + màu xanh
Data source: Prometheus
Query: sum(up)
Visualization: Stat
Title: Prometheus Targets Up
Save
```

### Demo logs với Loki

Grafana:

```text
Explore -> chọn Loki -> query: {namespace="staging"} -> Run query
```

Nếu muốn ít log hơn:

```text
{namespace="staging", container="product"}
```

Giải thích:

```text
Log là dòng sự kiện/runtime text từ container stdout/stderr. Promtail thu log từ node, gửi sang Loki, Grafana dùng Loki để search/filter log.
```

### Demo traces với Tempo

Tạo trace test nếu cần:

```bash
kubectl delete pod telemetrygen -n observability --ignore-not-found
```

```bash
kubectl run telemetrygen -n observability --rm -i --restart=Never --image=ghcr.io/open-telemetry/opentelemetry-collector-contrib/telemetrygen:latest --command -- /telemetrygen traces --otlp-endpoint opentelemetry-collector:4317 --otlp-insecure --traces 20
```

Kiểm tra Tempo API:

```bash
kubectl run tempo-test -n observability --rm -i --restart=Never --image=curlimages/curl:8.8.0 --command -- curl -s 'http://tempo:3200/api/search'
```

Grafana:

```text
Explore -> chọn Tempo -> query: {resource.service.name="telemetrygen"} -> Run query
```

Giải thích:

```text
Trace là dấu vết một request đi qua nhiều service. Trace gồm nhiều span, mỗi span có thời gian bắt đầu/kết thúc, service name, latency. OpenTelemetry Collector nhận trace OTLP rồi gửi sang Tempo, Grafana dùng Tempo để visualize trace.
```

### Screenshot cần chụp

- `kubectl get pods -n observability`
- Grafana data sources: Prometheus, Loki, Tempo
- Prometheus query `up`
- Loki query `{namespace="staging"}`
- Tempo query `telemetrygen`
- Dashboard tự tạo `Prometheus Targets Up`

## 15. Service Mesh Demo - Istio, mTLS, Retry, AuthorizationPolicy, Kiali

### Mục tiêu

Chứng minh đủ 2 điểm nâng cao service mesh:

```text
mTLS giữa service
Kiali topology
Retry policy
AuthorizationPolicy allow/deny
curl test evidence
```

### Kiểm tra Istio/Kiali

```bash
kubectl get pods -n istio-system
```

Kỳ vọng:

```text
istiod Running
kiali Running nếu đã cài
```

Nếu cần mở Kiali:

```bash
/Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2/bin/istioctl dashboard kiali
```

Hoặc:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

### Apply manifest service mesh

```bash
kubectl apply -f outputs/service-mesh/01-mesh-demo-namespace.yaml
```

```bash
kubectl apply -f outputs/service-mesh/02-mtls-strict.yaml
```

```bash
kubectl apply -f outputs/service-mesh/03-tax-retry-virtualservice.yaml
```

```bash
kubectl apply -f outputs/service-mesh/04-authz-default-deny.yaml
```

```bash
kubectl apply -f outputs/service-mesh/05-authz-allow-order-to-tax.yaml
```

### Kiểm tra policy

```bash
kubectl get ns mesh-demo --show-labels
```

Kỳ vọng namespace có:

```text
istio-injection=enabled
```

```bash
kubectl get peerauthentication -n mesh-demo
```

```bash
kubectl get virtualservice -n mesh-demo
```

```bash
kubectl get authorizationpolicy -n mesh-demo
```

### Kiểm tra sidecar

```bash
kubectl get pods -n mesh-demo
```

```bash
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

Kỳ vọng pod có:

```text
istio-proxy
```

### Test allow/deny

Nếu chưa có curl pod:

```bash
kubectl run curl-order -n mesh-demo --image=curlimages/curl:8.8.0 --restart=Never --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl-order","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

```bash
kubectl run curl-deny -n mesh-demo --image=curlimages/curl:8.8.0 --restart=Never --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl-deny","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

Allow test:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- curl -i http://tax:8090/actuator/health
```

Deny test:

```bash
kubectl exec -n mesh-demo curl-deny -c curl-deny -- curl -i http://tax:8090/actuator/health
```

Kỳ vọng:

```text
curl-order: request được phép hoặc tới được tax.
curl-deny: HTTP 403/RBAC access denied.
```

### Test retry

Mở file:

```text
outputs/service-mesh/03-tax-retry-virtualservice.yaml
```

Chỉ đoạn:

```yaml
retries:
  attempts: 3
  perTryTimeout: 2s
  retryOn: 5xx,connect-failure,refused-stream
```

Nói:

```text
VirtualService này cấu hình Istio retry tối đa 3 lần khi upstream trả 5xx hoặc lỗi kết nối. Đây là retry ở tầng service mesh, không cần sửa code service.
```

### Kiali topology

Tạo traffic:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- curl -s http://tax:8090/actuator/health
```

Lặp lại vài lần nếu cần. Sau đó mở Kiali:

```text
Kiali -> Graph -> Namespace mesh-demo
```

Chỉ cho thầy:

```text
service graph/topology
traffic từ client/order sang tax
mTLS lock icon nếu có
request rate/response code
```

## 16. Luồng Tổng Hợp Nói Trong 30 Giây

Nếu thầy hỏi “tổng thể hệ thống chạy sao?”, nói:

```text
Nhóm dùng Jenkins làm CI/CD chính. Với branch developer, Jenkins Multibranch detect service thay đổi bằng git diff, chỉ test/build service đó, scan Sonar/Snyk/Gitleaks evidence, build Docker image và push lên Docker Hub với tag commit SHA và branch name. Khi merge main, Jenkins push image main/SHA và cập nhật GitOps repo DoubleHo05/yas-deployment ở envs/dev. Argo CD theo dõi GitOps repo và auto sync vào namespace dev. Khi tạo release tag vX.Y.Z, Jenkins push image tag release và update envs/staging để Argo CD sync vào namespace staging. Developer có job developer_build để deploy preview theo parameter và cleanup job để xóa preview. Ứng dụng chạy trên K3s, truy cập qua ingress-nginx NodePort 30303 bằng domain local hosts file. Nhóm bổ sung observability bằng Prometheus, Loki, Tempo, Grafana, OpenTelemetry và service mesh bằng Istio/Kiali trong mesh-demo để chứng minh mTLS, retry và authorization policy.
```

## 17. Checklist Trước Khi Demo

### Project 1

- [ ] GitHub branch protection `main`.
- [ ] Có PR mở hoặc tạo PR live.
- [ ] PR cần 2 approvals.
- [ ] PR có Jenkins status check.
- [ ] Jenkins multibranch có Discover branches/PR/tags.
- [ ] Jenkins console có `Detect Changed Services`.
- [ ] Jenkins chỉ build service thay đổi.
- [ ] Test Result có dữ liệu.
- [ ] Coverage/SonarCloud có dữ liệu.
- [ ] Gitleaks evidence.
- [ ] Snyk evidence.

### Project 2 Bắt Buộc

- [ ] `kubectl get nodes -o wide` có master/worker.
- [ ] DockerHub có tag commit SHA và branch.
- [ ] `developer_build` có parameter.
- [ ] `developer_build` deploy preview đúng tag.
- [ ] Preview có NodePort/domain evidence.
- [ ] Cleanup job xóa preview.
- [ ] Web storefront mở được bằng `storefront.yas.local.com:30303`.

### Project 2 Nâng Cao

- [ ] Argo CD UI có `yas-root-dev`.
- [ ] Argo CD UI có `yas-root-staging`.
- [ ] `yas-tax-dev` hoặc service demo Synced/Healthy.
- [ ] `yas-tax-staging` có release tag.
- [ ] GitOps repo có commit update values.
- [ ] Observability pods Running.
- [ ] Grafana query Prometheus `up`.
- [ ] Grafana query Loki `{namespace="staging"}`.
- [ ] Grafana query Tempo `telemetrygen`.
- [ ] Kiali topology mesh-demo.
- [ ] PeerAuthentication STRICT.
- [ ] AuthorizationPolicy allow/deny.
- [ ] VirtualService retry.

## 18. Nếu Có Sự Cố Trong Demo

### Jenkins không tự build tag

Kiểm tra:

```text
YAS -> Configure -> Discover tags -> Build strategies -> Tags
```

Không để `Ignore tags older than` là `0`.

### Argo CD Synced nhưng Health Progressing

Kiểm tra workload thật:

```bash
kubectl get deploy,pod,svc -n dev
```

Nếu pod Running 1/1 và API/web chạy được thì nói:

```text
Health trong Argo đôi khi chậm cập nhật với Ingress/rollout, nhóm kiểm chứng trực tiếp bằng Kubernetes resource và API test.
```

### Web trả 500

Kiểm tra dependency:

```bash
kubectl get pods -n postgres
```

```bash
kubectl get pods -n kafka
```

```bash
kubectl get elasticsearch -n elasticsearch
```

```bash
kubectl logs -n dev deploy/product --tail=80
```

### Grafana CrashLoop

Thường do PostgreSQL không nhận connection. Kiểm tra:

```bash
kubectl get pod,svc,endpoints -n postgres -o wide
```

```bash
kubectl logs -n postgres postgresql-0 --tail=120
```

### Promtail không chạy trên master

Giải thích:

```text
Promtail từng gặp lỗi too many open files trên master nên nhóm pin Promtail sang worker để demo ổn định. Về bản chất Promtail là DaemonSet thu log node, production có thể tăng ulimit/tune node để chạy trên mọi node.
```

### Service mesh làm hỏng dev/staging

Không bật Istio injection cho `dev`/`staging` trong demo. Dùng `mesh-demo`:

```text
mesh-demo được tách riêng để chứng minh service mesh mà không phá môi trường CD chính.
```

