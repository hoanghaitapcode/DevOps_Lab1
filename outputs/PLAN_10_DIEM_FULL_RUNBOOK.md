# Kế hoạch lấy 10 điểm - YAS Continuous Deployment

Tài liệu này thay thế tư duy "demo tối thiểu". Mục tiêu mới là hoàn thành đầy đủ yêu cầu bắt buộc và làm phần nâng cao đủ chắc để nhắm 10 điểm.

Nguyên tắc không đổi:

- Vẫn kế thừa Jenkins cũ từ Project 1.
- Không chuyển sang GitHub Actions.
- Không cài Jenkins mới nếu Jenkins Project 1 còn dùng được.
- Jenkins chỉ điều phối CI/CD, build image, push Docker Hub, deploy bằng `kubectl`/`helm`.
- Workload YAS chạy trên K3s ở Google Cloud.
- Không build full service song song trên Jenkins 8GB.
- Không commit secret/token/password vào repo.

## Hiện trạng nhóm đã có

Nhóm đã làm được nền khá tốt:

- K3s chạy trên GCP với `yas-master` và `yas-worker`.
- Jenkins cũ đã kết nối được K3s qua kubeconfig.
- Jenkins đã push được image `docker.io/doubleho/yas-tax`.
- Có tag commit SHA, branch alias `dev_tax_service`, và tag `main` cho `tax`.
- Job `developer_build` đã deploy preview `tax`.
- Job `developer_cleanup` đã xóa được namespace preview.
- NodePort `30080` đã tạo được.
- Postgres runtime tối thiểu đã biết cách tạo tại DNS `postgresql.postgres`.
- `yas-configuration` đã biết cách cài bằng Helm.
- Argo CD đã Running trong namespace `argocd`.
- Argo CD Application `yas-tax-dev` đã `Synced` nhưng health còn `Progressing` vì thiếu dependency runtime trong namespace `dev`.

Từ đây, việc cần làm không phải làm lại từ đầu. Việc cần làm là mở rộng từ service mẫu `tax` sang kiến trúc đủ service, đủ dev/staging, đủ service mesh.

---

# Đích cuối để lấy 10 điểm

## Bắt buộc phải hoàn thành

1. Jenkins cũ Project 1 tiếp tục là trung tâm CI/CD.
2. K3s cluster có 1 master, 1 worker trên Google Cloud.
3. Docker Hub có image tag `main` cho tất cả service giữ lại.
4. Khi developer push branch, Jenkins build service thay đổi và push image tag commit SHA.
5. Job `developer_build` nhận branch từng service.
6. Service nào để `main` thì deploy image tag `main`.
7. Service nào nhập branch riêng thì deploy image tag commit SHA của branch đó.
8. Preview expose qua NodePort/domain local.
9. Job `developer_cleanup` xóa preview environment.

## Nâng cao để nhắm 10 điểm

1. Argo CD quản lý namespace `dev`.
2. Argo CD quản lý namespace `staging`.
3. `main` deploy vào `dev`.
4. Release tag, ví dụ `v1.0.0`, deploy vào `staging`.
5. Istio mTLS STRICT.
6. Kiali topology.
7. VirtualService retry.
8. AuthorizationPolicy allow/deny.
9. Có test curl chứng minh allow/deny.
10. Có README/report/screenshot/log đầy đủ.

---

# Phân công 4 người

| Thành viên | Vai trò | Mục tiêu 10 điểm |
|---|---|---|
| TV1 | Infrastructure Lead | Nâng tài nguyên nếu cần, giữ K3s ổn, cài Argo CD/Istio/Kiali, firewall, kubeconfig |
| TV2 | Jenkins CI & Container Lead | Build/push image `main`, commit SHA, release tag cho toàn bộ service cần giữ |
| TV3 | Jenkins CD & GitOps Lead | Hoàn thiện `developer_build`, cleanup, Helm deploy core flow, Argo CD dev/staging |
| TV4 | Service Mesh & QA/Report Lead | Istio mTLS/retry/auth, Kiali, curl evidence, README/report/demo script |

---

# Danh sách service mục tiêu

## Nhóm phải giữ trong kiến trúc

| Service trong đề | Thư mục/image đề xuất | Ghi chú |
|---|---|---|
| product | `product` -> `yas-product` | Core shop |
| cart | `cart` -> `yas-cart` | Core shop |
| order | `order` -> `yas-order` | Core shop, dùng mesh retry |
| customer | `customer` -> `yas-customer` | Core shop |
| inventory | `inventory` -> `yas-inventory` | Core shop |
| tax | `tax` -> `yas-tax` | Đã demo, dùng mesh retry |
| media | `media` -> `yas-media` | Product images |
| search | `search` -> `yas-search` | AuthorizationPolicy demo |
| storefront-bff | `storefront-bff` -> `yas-storefront-bff` | BFF user |
| storefront-ui | `storefront` -> `yas-storefront` | UI user |
| backoffice-bff | `backoffice-bff` -> `yas-backoffice-bff` | BFF admin |
| backoffice-ui | `backoffice` -> `yas-backoffice` | UI admin |
| swagger-ui | `swaggerapi/swagger-ui` hoặc wrapper `yas-swagger-ui` | Repo hiện dùng image official |
| sampledata | `sampledata` -> `yas-sampledata` | Chạy 1 lần rồi scale 0 |

## Runtime demo nên chạy

Để lấy điểm cao, cố chạy runtime ít nhất nhóm này:

```text
storefront-ui
storefront-bff
product
cart
order
customer
inventory
tax
```

Nếu cluster còn khỏe, mở thêm:

```text
media
search
backoffice-bff
backoffice-ui
swagger-ui
sampledata
```

Nếu cluster không chịu nổi full, vẫn giữ full service trong Jenkins parameters, image tags, Argo plan, report. Runtime demo dùng subset nhưng giải thích rõ giới hạn 1 master/1 worker.

---

# Chặng 0 - Đóng băng trạng thái hiện tại

Mục tiêu: không mất những gì đã làm được.

## TV4 tạo evidence baseline

Chạy và chụp:

```bash
kubectl get nodes -o wide
kubectl get pods -n argocd
kubectl get svc -n preview-demo || true
kubectl get pods -n preview-demo || true
kubectl get svc -n postgres || true
```

Chụp Jenkins:

```text
developer_build last SUCCESS
developer_cleanup last SUCCESS
Docker Hub yas-tax tags
Argo CD yas-tax-dev Synced
```

## TV3 backup Jenkins scripts

Trong Jenkins UI, copy script của 2 job ra file nháp local:

```text
evidence/jenkins/developer_build.groovy.txt
evidence/jenkins/developer_cleanup.groovy.txt
```

Không commit credential/kubeconfig.

## TV2 kiểm tra branch làm việc

```bash
git status
git branch --show-current
git fetch origin
```

Nếu đang ở branch linh tinh như `dev_tax_service`, tạo branch làm Project 2:

```bash
git checkout main
git pull
git checkout -b project2-10-score
```

Nếu đã có branch Project 2, dùng branch đó.

Điều kiện pass chặng 0:

- Có evidence hiện trạng.
- Không xóa `preview-demo`, `postgres`, `argocd` nếu chưa chụp.
- Có branch làm việc rõ ràng.

---

# Chặng 1 - Nâng hạ tầng để chịu được full demo

Mục tiêu: cluster đủ tài nguyên cho core e-commerce + Argo + Istio.

## TV1 kiểm tra tài nguyên

```bash
kubectl get nodes -o wide
kubectl top nodes || true
kubectl top pods -A || true
kubectl describe node yas-worker | sed -n '/Allocated resources:/,/Events:/p'
```

Nếu chưa có Metrics Server, cài hoặc bỏ qua. Không để việc này chặn.

## TV1 đề xuất cấu hình máy

Khuyến nghị nếu muốn lấy 10 điểm:

```text
yas-master: ít nhất 2 vCPU, 4GB RAM
yas-worker: ít nhất 4 vCPU, 8GB RAM
Disk worker: 50GB hoặc 80GB
```

Nếu hiện worker quá yếu, dừng VM rồi đổi machine type. Ví dụ:

```bash
gcloud compute instances stop yas-worker --zone <ZONE>
gcloud compute instances set-machine-type yas-worker \
  --zone <ZONE> \
  --machine-type e2-standard-4
gcloud compute instances start yas-worker --zone <ZONE>
```

Nếu cần tăng disk:

```bash
gcloud compute disks resize yas-worker --zone <ZONE> --size 80GB
```

SSH vào worker và mở rộng filesystem nếu OS chưa tự mở rộng:

```bash
df -h
lsblk
```

Nếu boot disk là `/dev/sda1`:

```bash
sudo growpart /dev/sda 1 || true
sudo resize2fs /dev/sda1 || true
df -h
```

Nếu lệnh không đúng thiết bị, dừng và hỏi TV1 kiểm tra `lsblk`.

## TV1 kiểm tra lại sau khi nâng

```bash
kubectl get nodes -o wide
kubectl get pods -A
```

Điều kiện pass chặng 1:

- `yas-master` Ready.
- `yas-worker` Ready.
- Argo CD vẫn Running.
- Worker còn đủ CPU/RAM để chạy thêm service.

---

# Chặng 2 - Build image `main` cho toàn bộ service giữ lại

Mục tiêu: đáp ứng yêu cầu "mặc định có image cho tất cả service với tag main hoặc latest".

## TV2 chốt image map

Ghi vào report và Jenkins note:

```text
product          -> docker.io/doubleho/yas-product
cart             -> docker.io/doubleho/yas-cart
order            -> docker.io/doubleho/yas-order
customer         -> docker.io/doubleho/yas-customer
inventory        -> docker.io/doubleho/yas-inventory
tax              -> docker.io/doubleho/yas-tax
media            -> docker.io/doubleho/yas-media
search           -> docker.io/doubleho/yas-search
storefront-bff   -> docker.io/doubleho/yas-storefront-bff
storefront-ui    -> docker.io/doubleho/yas-storefront
backoffice-bff   -> docker.io/doubleho/yas-backoffice-bff
backoffice-ui    -> docker.io/doubleho/yas-backoffice
sampledata       -> docker.io/doubleho/yas-sampledata
swagger-ui       -> docker.io/doubleho/yas-swagger-ui hoặc swaggerapi/swagger-ui:v4.16.0
```

Repo hiện chưa thấy `./swagger-ui/Dockerfile`. Có 2 hướng:

```text
Hướng A: dùng official image swaggerapi/swagger-ui:v4.16.0 như chart hiện tại.
Hướng B: tạo wrapper image yas-swagger-ui để Docker Hub cũng có tag main.
```

Để lấy điểm tối đa, chọn hướng B nếu còn thời gian.

## TV2 tạo Jenkins job bootstrap `build_all_main_images`

Tạo Jenkins Pipeline job mới, tên:

```text
build_all_main_images
```

Job này không thay Project 1 CI. Nó chỉ bootstrap default image `main` cho toàn bộ service, build tuần tự.

Pipeline skeleton:

```groovy
pipeline {
    agent any
    parameters {
        string(name: 'IMAGE_TAG', defaultValue: 'main')
        string(name: 'SERVICES', defaultValue: 'product,cart,order,customer,inventory,tax,media,search,storefront-bff,storefront,backoffice-bff,backoffice,sampledata')
    }
    environment {
        DOCKERHUB_USER = 'doubleho'
        REPO_URL = 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: "${REPO_URL}"
            }
        }
        stage('Build Push Sequential') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        set -eu
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                        image_name() {
                          case "$1" in
                            storefront) echo "yas-storefront" ;;
                            backoffice) echo "yas-backoffice" ;;
                            *) echo "yas-$1" ;;
                          esac
                        }

                        IFS=','
                        for svc in $SERVICES; do
                          img="$(image_name "$svc")"
                          echo "Building $svc -> docker.io/$DOCKERHUB_USER/$img:$IMAGE_TAG"
                          docker build -t "docker.io/$DOCKERHUB_USER/$img:$IMAGE_TAG" "./$svc"
                          docker push "docker.io/$DOCKERHUB_USER/$img:$IMAGE_TAG"
                        done

                        docker logout
                    '''
                }
            }
        }
    }
}
```

Chạy job:

```text
IMAGE_TAG=main
SERVICES=product,cart,order,customer,inventory,tax,media,search,storefront-bff,storefront,backoffice-bff,backoffice,sampledata
```

Nếu Jenkins thiếu RAM, tách thành 3 lượt:

```text
Lượt 1: product,cart,order,customer
Lượt 2: inventory,tax,media,search
Lượt 3: storefront-bff,storefront,backoffice-bff,backoffice,sampledata
```

## TV2 tạo wrapper swagger-ui nếu muốn đẹp

Nếu chọn hướng B:

```bash
mkdir -p swagger-ui
```

Tạo `swagger-ui/Dockerfile`:

```dockerfile
FROM swaggerapi/swagger-ui:v4.16.0
```

Commit lên branch Project 2, rồi thêm `swagger-ui` vào job build:

```text
SERVICES=swagger-ui
```

Trong `image_name()`, thêm:

```bash
swagger-ui) echo "yas-swagger-ui" ;;
```

Nếu không làm wrapper, report ghi:

```text
swagger-ui uses official image swaggerapi/swagger-ui:v4.16.0 from the upstream chart.
```

## TV4 chụp evidence Docker Hub

Chụp Docker Hub tags:

```text
yas-product:main
yas-cart:main
yas-order:main
yas-customer:main
yas-inventory:main
yas-tax:main
yas-media:main
yas-search:main
yas-storefront-bff:main
yas-storefront:main
yas-backoffice-bff:main
yas-backoffice:main
yas-sampledata:main
```

Điều kiện pass chặng 2:

- Ít nhất core 8 service có tag `main`.
- Tốt nhất 13 custom service có tag `main`.
- Jenkins build tuần tự, không crash vì chạy song song.

---

# Chặng 3 - Nâng Jenkins CI branch build cho mọi service

Mục tiêu: khi developer sửa service nào, Jenkins build/push đúng image service đó bằng commit SHA.

## TV2 cập nhật service list trong Jenkinsfile

Trong Jenkinsfile, service list nên bao gồm toàn bộ service có Dockerfile cần giữ:

```groovy
JAVA_SERVICES = 'product,cart,order,customer,inventory,tax,media,search,storefront-bff,backoffice-bff,sampledata'
UI_SERVICES = 'storefront,backoffice'
OPTIONAL_JAVA_SERVICES = 'location,payment,payment-paypal,promotion,rating,recommendation,webhook,delivery'
```

Nếu Jenkinsfile hiện đang dùng một biến duy nhất, dùng danh sách này:

```groovy
JAVA_SERVICES = 'cart,customer,inventory,location,media,order,payment,payment-paypal,product,promotion,rating,search,storefront-bff,backoffice-bff,tax,webhook,sampledata,recommendation,delivery'
UI_SERVICES = 'storefront,backoffice'
```

Không bắt buộc build optional service trong demo, nhưng monorepo detection nên không bị bỏ sót nếu có thay đổi.

## TV2 kiểm tra Docker build path

Mapping quan trọng:

```text
storefront-ui parameter -> folder storefront -> image yas-storefront
backoffice-ui parameter -> folder backoffice -> image yas-backoffice
```

Không đặt image là `yas-storefront-ui` nếu Docker Hub đang dùng `yas-storefront`.

## TV2 test branch mới cho service khác tax

Tạo branch demo `dev_product_service`:

```bash
git checkout main
git pull
git checkout -b dev_product_service
mkdir -p product/docs
printf "Demo change for product CI/CD\\n" > product/docs/project2-demo.txt
git add product/docs/project2-demo.txt
git commit -m "demo: update product service for project 2"
git push origin dev_product_service
```

Jenkins phải:

```text
Detect product
Build/test product
Push yas-product:<commit-sha>
Push yas-product:dev_product_service
```

Sau đó test branch UI:

```bash
git checkout main
git pull
git checkout -b dev_storefront_ui
mkdir -p storefront/docs
printf "Demo change for storefront UI CI/CD\\n" > storefront/docs/project2-demo.txt
git add storefront/docs/project2-demo.txt
git commit -m "demo: update storefront ui for project 2"
git push origin dev_storefront_ui
```

Jenkins phải push:

```text
docker.io/doubleho/yas-storefront:<commit-sha>
docker.io/doubleho/yas-storefront:dev_storefront_ui
```

Điều kiện pass chặng 3:

- `tax`, `product`, `storefront` đều chứng minh branch -> commit SHA image.
- Không build toàn bộ repo khi chỉ sửa một service.
- Có screenshot Jenkins console.

---

# Chặng 4 - Làm `developer_build` thành preview e-commerce thật

Mục tiêu: `developer_build` không chỉ deploy `tax`, mà deploy được core flow với branch override từng service.

## TV3 giữ job hiện tại, tạo bản mới nếu sợ hỏng

Trong Jenkins, duplicate hoặc copy script cũ ra ngoài trước.

Tên job chính vẫn phải là:

```text
developer_build
```

Nếu muốn test trước, tạo job phụ:

```text
developer_build_v2_test
```

Khi ổn mới copy sang `developer_build`.

## TV3 mở rộng parameters

Parameters bắt buộc:

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
SWAGGER_UI_BRANCH=main
DEPLOY_MODE=core
```

`DEPLOY_MODE`:

```text
tax-only: deploy tax để debug nhanh
core: deploy storefront-ui, storefront-bff, product, cart, order, customer, inventory, tax
full: deploy core + media + search + backoffice + swagger + sampledata
```

Nếu Jenkins parameter choice phức tạp, dùng string:

```groovy
string(name: 'DEPLOY_MODE', defaultValue: 'core')
```

## TV3 thêm deploy functions

Trong shell của Jenkins job, giữ:

```bash
resolve_tag()
bootstrap_postgres()
bootstrap_yas_configuration()
deploy_backend()
deploy_ui()
```

Thêm deploy swagger:

```bash
deploy_swagger() {
  tag="$1"
  repo="swaggerapi/swagger-ui"
  if [ "$tag" != "main" ]; then
    repo="docker.io/$DOCKERHUB_USER/yas-swagger-ui"
  fi

  helm dependency build k8s/charts/swagger-ui || true
  helm upgrade --install swagger-ui k8s/charts/swagger-ui \
    -n "$NAMESPACE" --create-namespace \
    --set image.repository="$repo" \
    --set image.tag="$tag"
}
```

Nếu không làm wrapper `yas-swagger-ui`, luôn dùng official:

```bash
--set image.repository=swaggerapi/swagger-ui
--set image.tag=v4.16.0
```

## TV3 deploy theo mode

Sau khi resolve tag cho toàn bộ parameter:

```bash
bootstrap_postgres
bootstrap_yas_configuration

if [ "$DEPLOY_MODE" = "tax-only" ]; then
  deploy_backend tax tax yas-tax "$TAX_TAG"
fi

if [ "$DEPLOY_MODE" = "core" ] || [ "$DEPLOY_MODE" = "full" ]; then
  deploy_backend product product yas-product "$PRODUCT_TAG"
  deploy_backend cart cart yas-cart "$CART_TAG"
  deploy_backend order order yas-order "$ORDER_TAG"
  deploy_backend customer customer yas-customer "$CUSTOMER_TAG"
  deploy_backend inventory inventory yas-inventory "$INVENTORY_TAG"
  deploy_backend tax tax yas-tax "$TAX_TAG"
  deploy_backend storefront-bff storefront-bff yas-storefront-bff "$STOREFRONT_BFF_TAG"
  deploy_ui storefront-ui storefront-ui yas-storefront "$STOREFRONT_UI_TAG"
fi

if [ "$DEPLOY_MODE" = "full" ]; then
  deploy_backend media media yas-media "$MEDIA_TAG"
  deploy_backend search search yas-search "$SEARCH_TAG"
  deploy_backend backoffice-bff backoffice-bff yas-backoffice-bff "$BACKOFFICE_BFF_TAG"
  deploy_ui backoffice-ui backoffice-ui yas-backoffice "$BACKOFFICE_UI_TAG"
  deploy_backend sampledata sampledata yas-sampledata main
  deploy_swagger main
fi
```

## TV3 đổi NodePort trỏ vào UI

Với demo 10 điểm, NodePort nên trỏ vào `storefront-ui`, không phải `tax`.

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: $NAMESPACE
spec:
  type: NodePort
  selector:
    app.kubernetes.io/name: storefront-ui
    app.kubernetes.io/instance: storefront-ui
  ports:
    - name: http
      port: 80
      targetPort: 3000
      nodePort: 30080
EOF
```

Nếu `storefront-ui` chưa chạy, tạm trỏ vào `tax` để debug, nhưng final demo nên là UI.

## TV1 mở hosts/domain

Trên máy demo, thêm:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
<WORKER_EXTERNAL_IP> storefront.yas.local.com
<WORKER_EXTERNAL_IP> api.yas.local.com
<WORKER_EXTERNAL_IP> backoffice.yas.local.com
```

Test:

```bash
curl -I http://yas-preview.local:30080
```

## TV3 chạy demo branch override nhiều service

Ví dụ:

```text
ENV_NAME=demo
DEPLOY_MODE=core
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=dev_product_service
STOREFRONT_UI_BRANCH=dev_storefront_ui
Các branch khác main
```

Kiểm tra:

```bash
kubectl get pods -n preview-demo
kubectl get svc -n preview-demo
kubectl get deployment tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment product -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment storefront-ui -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Điều kiện pass chặng 4:

- `developer_build` deploy được `core`.
- Ít nhất 2 service override branch riêng dùng commit SHA.
- Service còn lại dùng `main`.
- NodePort trỏ UI.
- `developer_cleanup` xóa `preview-demo`, không xóa `postgres`.

---

# Chặng 5 - Runtime dependency cho dev/staging

Mục tiêu: Argo CD không còn chỉ sync chart lẻ; namespace `dev` và `staging` có dependency đủ để service Healthy hơn.

## TV1 tạo shared infrastructure namespace

Postgres dùng chung:

```bash
kubectl get ns postgres || kubectl create namespace postgres
kubectl get svc -n postgres
kubectl get pods -n postgres
```

Nếu chưa có Postgres, dùng YAML từ Day4 hoặc để `developer_build` bootstrap rồi giữ lại.

Redis nếu BFF cần:

```bash
ls k8s/deploy/setup-redis.sh
bash k8s/deploy/setup-redis.sh || true
kubectl get pods -A | grep -i redis || true
```

Keycloak nếu demo login/backoffice:

```bash
ls k8s/deploy/setup-keycloak.sh
bash k8s/deploy/setup-keycloak.sh || true
kubectl get pods -A | grep -i keycloak || true
```

Elasticsearch nếu demo `search`:

```bash
helm dependency build k8s/deploy/elasticsearch/elasticsearch-cluster || true
helm upgrade --install elasticsearch k8s/deploy/elasticsearch/elasticsearch-cluster \
  -n elasticsearch --create-namespace
kubectl get pods -n elasticsearch
```

Kafka nếu service phụ thuộc event:

```bash
helm upgrade --install kafka k8s/deploy/kafka/kafka-cluster \
  -n kafka --create-namespace
kubectl get pods -n kafka
```

Không cài Kafka/Elasticsearch nếu cluster thiếu tài nguyên. Ghi rõ trong report nếu chỉ dùng subset.

## TV3 cài `yas-configuration` cho dev/staging

```bash
kubectl create namespace dev --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace staging --dry-run=client -o yaml | kubectl apply -f -

helm dependency build k8s/charts/yas-configuration || true
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n dev --create-namespace
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n staging --create-namespace
```

Kiểm tra:

```bash
kubectl get configmap -n dev | grep yas-configuration
kubectl get secret -n dev | grep yas-postgresql
kubectl get configmap -n staging | grep yas-configuration
kubectl get secret -n staging | grep yas-postgresql
```

Điều kiện pass chặng 5:

- `dev` và `staging` có `yas-configuration-configmap`.
- `dev` và `staging` có `yas-postgresql-credentials-secret`.
- `postgresql.postgres` tồn tại.

---

# Chặng 6 - Argo CD dev/staging thật sự

Mục tiêu: Argo CD quản lý nhiều Application, không chỉ `yas-tax-dev`.

## TV3 tạo thư mục GitOps trong repo

Tạo trên branch Project 2:

```bash
mkdir -p k8s/gitops/dev
mkdir -p k8s/gitops/staging
```

Không để secret trong thư mục này.

## TV3 tạo Application dev cho core service

Tạo file `k8s/gitops/dev/yas-product-dev.yaml`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: yas-product-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/hoanghaitapcode/DevOps_Lab1.git
    targetRevision: main
    path: k8s/charts/product
    helm:
      parameters:
        - name: backend.image.repository
          value: docker.io/doubleho/yas-product
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

Copy pattern cho:

```text
yas-cart-dev
yas-order-dev
yas-customer-dev
yas-inventory-dev
yas-tax-dev
yas-storefront-bff-dev
yas-storefront-ui-dev
```

Khác nhau:

- `path`
- `metadata.name`
- image repository
- chart parameter `backend.*` hoặc `ui.*`

Với UI:

```yaml
helm:
  parameters:
    - name: ui.image.repository
      value: docker.io/doubleho/yas-storefront
    - name: ui.image.tag
      value: main
```

## TV3 apply dev Applications

```bash
kubectl apply -f k8s/gitops/dev/
kubectl get application -n argocd
```

Mở Argo CD UI:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Chụp:

```text
yas-product-dev Synced
yas-cart-dev Synced
yas-order-dev Synced
yas-tax-dev Synced
yas-storefront-ui-dev Synced
```

Nếu health Progressing, xem thiếu dependency:

```bash
kubectl get pods -n dev
kubectl describe pod -n dev -l app.kubernetes.io/name=tax
kubectl logs -n dev deploy/tax --tail=100 || true
```

## TV2 tạo release tag

Khi main ổn:

```bash
git checkout main
git pull
git tag v1.0.0
git push origin v1.0.0
```

Nếu không muốn đụng main khi chưa chắc, tạo tag trên commit đã merge Project 2.

## TV2 tạo Jenkins job `release_build`

Tạo Jenkins Pipeline job:

```text
release_build
```

Parameters:

```text
RELEASE_TAG=v1.0.0
SERVICES=product,cart,order,customer,inventory,tax,storefront-bff,storefront
```

Pipeline giống `build_all_main_images`, nhưng checkout tag:

```groovy
git branch: "${RELEASE_TAG}", url: "${REPO_URL}"
```

Nếu Jenkins git step không checkout tag tốt, dùng:

```groovy
git url: "${REPO_URL}"
sh '''
  git fetch --tags
  git checkout "$RELEASE_TAG"
'''
```

Build/push:

```text
docker.io/doubleho/yas-product:v1.0.0
docker.io/doubleho/yas-cart:v1.0.0
...
```

## TV3 tạo staging Applications

Copy dev YAML sang `k8s/gitops/staging`, đổi:

```text
metadata.name: yas-tax-staging
destination.namespace: staging
image tag: v1.0.0
```

Apply:

```bash
kubectl apply -f k8s/gitops/staging/
kubectl get application -n argocd | grep staging
```

Điều kiện pass chặng 6:

- Argo CD dev có nhiều app Synced.
- Argo CD staging có ít nhất core app hoặc tối thiểu `tax/product/order` Synced với tag `v1.0.0`.
- Docker Hub có release tag `v1.0.0`.
- Report nói rõ Jenkins build image, Argo CD sync manifest/values.

---

# Chặng 7 - Sampledata chạy 1 lần

Mục tiêu: đáp ứng yêu cầu sampledata chỉ chạy 1 lần rồi tắt.

## TV3 deploy sampledata vào dev hoặc preview

Ví dụ namespace `dev`:

```bash
helm dependency build k8s/charts/sampledata
helm upgrade --install sampledata k8s/charts/sampledata \
  -n dev --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-sampledata \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false
```

Theo dõi log:

```bash
kubectl get pods -n dev -l app.kubernetes.io/name=sampledata
kubectl logs -n dev deploy/sampledata --tail=100 || true
```

Sau khi thấy data seed xong hoặc nếu không cần chạy lâu:

```bash
kubectl scale deployment sampledata -n dev --replicas=0
kubectl get deployment sampledata -n dev
```

Evidence:

```text
sampledata deployed once
logs captured
replicas scaled to 0
```

---

# Chặng 8 - Istio/Kiali service mesh

Mục tiêu: có đủ deliverables mTLS, AuthorizationPolicy, retry, Kiali, curl evidence.

## TV1 cài Istio

Trên máy có `istioctl`. Nếu chưa có:

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH="$PWD/bin:$PATH"
istioctl version
```

Cài profile demo:

```bash
istioctl install --set profile=demo -y
kubectl get pods -n istio-system
```

Cài Kiali:

```bash
kubectl apply -f samples/addons/kiali.yaml
kubectl rollout status deployment/kiali -n istio-system --timeout=180s || true
kubectl get pods -n istio-system
```

Mở Kiali:

```bash
istioctl dashboard kiali
```

Nếu `istioctl dashboard` không mở được, dùng port-forward:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

## TV3 tạo namespace mesh-demo

Không phá `dev` nếu chưa chắc. Tạo namespace riêng:

```bash
kubectl create namespace mesh-demo --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace mesh-demo istio-injection=enabled --overwrite
```

Cài config:

```bash
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n mesh-demo --create-namespace
```

Deploy 3 service demo mesh:

```bash
helm upgrade --install tax k8s/charts/tax \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-tax \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false

helm upgrade --install order k8s/charts/order \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-order \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false

helm upgrade --install product k8s/charts/product \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-product \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false
```

Restart để inject sidecar nếu cần:

```bash
kubectl rollout restart deployment/tax -n mesh-demo
kubectl rollout restart deployment/order -n mesh-demo
kubectl rollout restart deployment/product -n mesh-demo
kubectl get pods -n mesh-demo
```

Kỳ vọng pod có `2/2` container nếu sidecar injected.

## TV4 tạo mTLS STRICT

Tạo file `mesh-mtls.yaml`:

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default-strict-mtls
  namespace: mesh-demo
spec:
  mtls:
    mode: STRICT
```

Apply:

```bash
kubectl apply -f mesh-mtls.yaml
kubectl get peerauthentication -n mesh-demo
```

Evidence:

```bash
kubectl get peerauthentication -n mesh-demo -o yaml
```

## TV4 tạo retry policy

Tạo file `tax-retry.yaml`:

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: tax-retry
  namespace: mesh-demo
spec:
  hosts:
    - tax
  http:
    - route:
        - destination:
            host: tax
            port:
              number: 80
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx,connect-failure,refused-stream
```

Apply:

```bash
kubectl apply -f tax-retry.yaml
kubectl get virtualservice -n mesh-demo
```

Nếu cần chứng minh retry rõ hơn, tạo thêm một service test `retry-demo` trả 500 rồi dùng Envoy access log/stats. Nhưng tối thiểu phải có YAML VirtualService và giải thích retry policy.

## TV4 tạo AuthorizationPolicy allow/deny

Tạo default deny:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: default-deny
  namespace: mesh-demo
spec: {}
```

Tạo allow `order` gọi `tax`:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-order-to-tax
  namespace: mesh-demo
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: tax
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/mesh-demo/sa/order
      to:
        - operation:
            ports:
              - "80"
```

Apply:

```bash
kubectl apply -f mesh-authz.yaml
kubectl get authorizationpolicy -n mesh-demo
```

## TV4 test allow/deny bằng curl pod

Tạo curl pod dùng service account `order`:

```bash
kubectl run curl-order -n mesh-demo \
  --image=curlimages/curl \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl-order","image":"curlimages/curl","command":["sleep","3600"]}]}}'
```

Tạo curl pod dùng service account default:

```bash
kubectl run curl-deny -n mesh-demo \
  --image=curlimages/curl \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl-deny","image":"curlimages/curl","command":["sleep","3600"]}]}}'
```

Chờ Running:

```bash
kubectl get pods -n mesh-demo
```

Test allow:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- \
  curl -i http://tax/actuator/health || true
```

Test deny:

```bash
kubectl exec -n mesh-demo curl-deny -c curl-deny -- \
  curl -i http://tax/actuator/health || true
```

Kỳ vọng:

```text
curl-order: được hoặc tới được service, tùy app health.
curl-deny: HTTP 403 RBAC: access denied.
```

Nếu app path khác, thử:

```bash
curl -i http://tax/tax/actuator/health
curl -i http://tax/actuator/health/readiness
```

Evidence cần chụp:

```bash
kubectl get pods -n mesh-demo
kubectl get peerauthentication -n mesh-demo
kubectl get authorizationpolicy -n mesh-demo
kubectl get virtualservice -n mesh-demo
kubectl exec ... curl allow
kubectl exec ... curl deny
```

## TV4 chụp Kiali

Tạo traffic:

```bash
for i in $(seq 1 20); do
  kubectl exec -n mesh-demo curl-order -c curl-order -- curl -s http://tax/actuator/health >/dev/null || true
done
```

Mở Kiali, chọn namespace `mesh-demo`, chụp topology.

Điều kiện pass chặng 8:

- Có mTLS YAML.
- Có AuthorizationPolicy YAML.
- Có VirtualService retry YAML.
- Có Kiali topology.
- Có curl allow/deny logs.

---

# Chặng 9 - Report/README/demo cuối

Mục tiêu: biến kỹ thuật thành điểm.

## TV4 tạo bảng hoàn thành yêu cầu

Trong report:

| Yêu cầu | Evidence |
|---|---|
| Kế thừa Jenkins Project 1 | Screenshot Jenkins cũ, Jenkinsfile |
| K3s 1 master 1 worker | `kubectl get nodes -o wide` |
| Image `main` cho service | Docker Hub screenshots |
| Branch image tag commit SHA | Jenkins console + Docker Hub |
| `developer_build` | Jenkins job parameters + console |
| Service default dùng `main` | Deployment image check |
| Service branch riêng dùng SHA | Deployment image check |
| NodePort/domain | `kubectl get svc`, hosts file, curl |
| cleanup job | Jenkins cleanup console |
| Argo CD dev/staging | Argo UI Application Synced |
| Istio mTLS/retry/auth | YAML + curl + Kiali |

## TV4 viết câu chuyện demo

Đọc theo flow:

```text
1. Nhóm kế thừa Jenkins Project 1, không thay bằng GitHub Actions.
2. Jenkins vẫn branch scan, test/build/coverage/security scan.
3. Project 2 mở rộng Jenkins thêm Docker build/push.
4. Mỗi branch developer chỉ build service thay đổi.
5. Image tag chính là commit SHA cuối branch.
6. developer_build cho developer chọn branch từng service.
7. Jenkins deploy preview lên K3s bằng Helm/kubectl.
8. K3s chạy workload, NodePort expose preview.
9. developer_cleanup xóa preview.
10. Argo CD quản lý dev/staging theo GitOps.
11. Istio chứng minh mTLS, retry, AuthorizationPolicy và Kiali topology.
```

## TV4 chuẩn bị demo script 10 điểm

Demo live theo thứ tự:

1. Mở Jenkins Project 1 Multibranch.
2. Mở branch `dev_tax_service`, chỉ ra Docker image commit SHA.
3. Mở Docker Hub `yas-tax` và một vài service khác.
4. Chạy `developer_build` với:

```text
DEPLOY_MODE=core
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=dev_product_service
STOREFRONT_UI_BRANCH=dev_storefront_ui
```

5. Chạy:

```bash
kubectl get deployment tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment product -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment storefront-ui -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get svc -n preview-demo
```

6. Mở NodePort URL.
7. Chạy `developer_cleanup`.
8. Mở Argo CD UI dev/staging.
9. Mở Kiali.
10. Chạy curl allow/deny.

## Final checklist trước khi nộp

| Hạng mục | Trạng thái |
|---|---|
| Jenkins cũ được dùng lại | yes/no |
| Không dùng GitHub Actions cho CD mới | yes/no |
| K3s master/worker Ready | yes/no |
| Docker Hub main tags core service | yes/no |
| Docker Hub SHA tags branch demo | yes/no |
| developer_build deploy core | yes/no |
| developer_cleanup OK | yes/no |
| NodePort trỏ UI | yes/no |
| Argo CD dev Synced | yes/no |
| Argo CD staging Synced | yes/no |
| Istio mTLS | yes/no |
| Istio retry | yes/no |
| Istio AuthorizationPolicy allow/deny | yes/no |
| Kiali screenshot | yes/no |
| Report/README/screenshots đủ | yes/no |

---

# Nếu gặp lỗi thì cắt scope thế nào mà vẫn nhắm điểm cao

Thứ tự cắt nếu cluster quá yếu:

1. Cắt backoffice runtime, giữ image + chart + report.
2. Cắt search runtime nếu Elasticsearch nặng, giữ image + Argo Application evidence nếu có.
3. Cắt Kafka runtime nếu không dùng trong demo trực tiếp.
4. Giữ core flow: storefront-ui, storefront-bff, product, cart, order, customer, inventory, tax.
5. Istio chỉ cần mesh-demo 2-3 service, không cần mesh toàn bộ 14 service.

Không được cắt:

- Jenkins cũ.
- Commit SHA image tag.
- `developer_build`.
- `developer_cleanup`.
- K3s.
- NodePort.
- Argo CD dev/staging evidence nếu muốn 10.
- Ít nhất một allow/deny AuthorizationPolicy nếu làm service mesh.

---

# Kết luận chiến lược 10 điểm

Muốn 10 điểm không phải cứ deploy full 14 service bằng mọi giá. Muốn 10 điểm là phải chứng minh được kiến trúc đúng, automation đúng, có mở rộng cho toàn bộ service, và demo live không gãy.

Chiến lược tốt nhất:

```text
Jenkins preview: core e-commerce flow chạy được, branch override ít nhất 2-3 service.
Docker Hub: image main cho toàn bộ service giữ lại.
Argo CD: dev/staging Synced với core service.
Istio: mesh-demo chứng minh mTLS + retry + allow/deny + Kiali.
Report: giải thích rõ full YAS architecture và giới hạn tài nguyên 1 master/1 worker.
```

Một câu chốt khi demo:

```text
Nhóm kế thừa Jenkins CI từ Project 1 và mở rộng thành CI/CD. Jenkins build image theo service thay đổi, tag bằng commit SHA, push Docker Hub và deploy preview theo branch bằng developer_build. K3s trên Google Cloud chạy workload. Argo CD quản lý dev/staging theo GitOps. Istio/Kiali chứng minh service mesh với mTLS, retry và AuthorizationPolicy.
```
