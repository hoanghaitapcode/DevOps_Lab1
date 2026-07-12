# Huong dan lam bao cao Google Docs - Project 2 YAS CD

Ngay cap nhat: 2026-07-08

File nay huong dan lam bao cao tu dau. Muc tieu la tao mot Google Doc co du bang chung cho Project 2: CD bang Jenkins, Kubernetes, Docker Hub, developer_build, cleanup, Argo CD dev/staging, observability, service mesh.

## 1. Viec can lam truoc khi mo Google Docs

Tao mot folder tren may de gom anh:

```text
Project2_Report_Evidence/
  01_cluster/
  02_dependencies/
  03_jenkins/
  04_dockerhub/
  05_argocd_gitops/
  06_preview_developer_build/
  07_application_demo/
  08_observability/
  09_service_mesh/
  10_appendix_logs/
```

Dat ten anh theo format:

```text
01_cluster_nodes.png
02_kafka_running.png
03_jenkins_main_success.png
04_dockerhub_tax_tags.png
05_argocd_dev_staging_apps.png
...
```

Moi anh trong bao cao phai co caption ro rang:

```text
Hinh X. Mo ta ngan gon anh chung minh yeu cau nao.
```

## 2. Tao Google Docs

1. Vao Google Drive.
2. Tao file Google Docs moi.
3. Dat ten tam thoi:

```text
Project2_YAS_CD_Report
```

4. Vao `File -> Page setup`:
   - Paper size: A4
   - Margins: 1 inch hoac 2.54 cm
5. Vao `Insert -> Page numbers` de them so trang.
6. Dung Heading styles:
   - Title cho ten do an
   - Heading 1 cho cac phan lon
   - Heading 2 cho cac muc con
7. Cuoi cung export:

```text
File -> Download -> Microsoft Word (.docx)
```

Ten file nop theo yeu cau:

```text
<MSSV1>_<MSSV2>_<MSSV3>_<MSSV4>.docx
```

Sap xep MSSV tang dan.

## 3. Cau truc bao cao nen dung

Dung cau truc nay trong Google Docs:

```text
Trang bia
Muc luc
1. Tong quan do an
2. Kien truc he thong
3. Trien khai Kubernetes cluster va dependency
4. CI/CD Jenkins-first
5. Developer preview bang developer_build va cleanup
6. GitOps dev/staging bang Argo CD
7. Trien khai ung dung va kiem thu web YAS
8. Observability
9. Service Mesh
10. Test plan va bang chung
11. Kho khan va cach xu ly
12. Ket luan
13. Phu luc
```

## 4. Noi dung tung phan

### Trang bia

Can co:

```text
Ten truong/khoa
Mon hoc: DevOps
Do an 2: Xay dung he thong CD cho YAS
Ten nhom
Danh sach thanh vien: Ho ten - MSSV - Vai tro
Giang vien
Ngay nop
```

Vai tro nen ghi:

```text
Thanh vien 1: Infrastructure, K3s, dependency
Thanh vien 2: Jenkins CI/CD, Docker Hub, observability
Thanh vien 3: GitOps, Argo CD, developer_build, cleanup
Thanh vien 4: Service Mesh, QA, report evidence
```

### 1. Tong quan do an

Viet ngan gon:

```text
Bao cao mo ta qua trinh xay dung he thong CD va giam sat cho YAS: Yet Another Shop. Nhom su dung Jenkins lam cong cu CI/CD chinh, Docker Hub lam image registry, K3s tren Google Cloud lam Kubernetes cluster, Argo CD cho GitOps dev/staging, Grafana/Loki/Prometheus/Tempo/OpenTelemetry cho observability va Istio/Kiali cho service mesh.
```

Nhac ro:

```text
Nhom khong them GitHub Actions moi cho CD. Jenkins la cong cu CI/CD chinh.
```

### 2. Kien truc he thong

Can co 1 so do tong quan. Co the ve trong Google Drawing:

```text
Developer -> GitHub source repo -> Jenkins -> Docker Hub
Jenkins -> GitOps repo DoubleHo05/yas-deployment
Argo CD -> K3s dev/staging namespaces
K3s -> YAS services + dependencies
Observability -> Prometheus/Loki/Tempo/Grafana/OpenTelemetry
Service Mesh -> Istio/Kiali mesh-demo
```

Caption:

```text
Hinh 1. Kien truc Jenkins-first CI/CD va GitOps cho YAS.
```

### 3. Kubernetes cluster va dependency

Anh can chup:

```bash
kubectl get nodes -o wide
kubectl get ns
```

Caption:

```text
Hinh 2. Cluster K3s gom 1 master node va 1 worker node tren Google Cloud.
```

Dependency:

```bash
kubectl get pods -n postgres
kubectl get pods -n kafka
kubectl get kafka -n kafka
kubectl get elasticsearch -n elasticsearch
kubectl get pods -n elasticsearch
kubectl get pods -n keycloak
kubectl get pods -n redis
```

Noi dung can viet:

```text
Nhom trien khai cac dependency nen tang truoc khi deploy microservices: PostgreSQL, Kafka, Elasticsearch, Redis va Keycloak. Cac service ung dung trong dev/staging ket noi den cac dependency thong qua Kubernetes DNS, vi du postgresql.postgres, kafka-cluster-kafka-brokers.kafka, elasticsearch-es-http.elasticsearch va keycloak-service.keycloak.
```

Can ghi chu trung thuc:

```text
Elasticsearch duoc nang cap len 9.2.3 de tuong thich voi search service hien tai. Kafka dung Strimzi 0.45.2 vi chart YAS su dung CRD kafka.strimzi.io/v1beta2.
```

### 4. CI/CD Jenkins-first

Anh can chup trong Jenkins:

```text
Jenkins multibranch/job list
Jenkins build branch developer thanh cong
Jenkins main build thanh cong
Console log co build/push Docker image
Jenkins credentials Docker Hub/GitHub/Kubeconfig, che gia tri secret
```

Noi dung:

```text
Jenkins duoc su dung lam cong cu CI/CD chinh. Khi developer push code len branch rieng, Jenkins quet branch, detect service thay doi trong monorepo, build/test service do, build Docker image va push len Docker Hub voi tag commit SHA va branch name.
```

Bang nen co:

| Truong hop | Ket qua |
|---|---|
| Commit branch developer | Jenkins build service thay doi |
| Build image | Push len Docker Hub |
| Tag image | Commit SHA va branch name |
| Merge main | Update GitOps dev |
| Release tag | Update GitOps staging |

### 5. Docker Hub image strategy

Anh can chup:

```text
Docker Hub repository doubleho/yas-tax
Tag main
Tag commit SHA, vi du bb479177d6d0
Tag branch, vi du dev_tax_service_2
```

Noi dung:

```text
Moi service co image mac dinh tag main. Khi branch developer thay doi service, Jenkins build image moi va gan tag bang commit id cuoi cung cua branch. Cach tag nay giup developer chon dung version de deploy preview.
```

### 6. developer_build va cleanup

Anh can chup:

```text
Jenkins job developer_build
Man hinh parameters cua job
Console output deploy preview
kubectl get pods -n preview-...
kubectl get svc -n preview-...
NodePort/domain de test
Jenkins cleanup job
Console output cleanup
```

Noi dung:

```text
Job developer_build cho phep developer nhap branch/tag muon deploy cho tung service. Service khong nhap se dung tag mac dinh main/latest. Job tao preview namespace rieng va expose bang NodePort de developer test bang domain local hosts file.
```

Neu developer_build hien chua hoan hao, viet trung thuc:

```text
Trong qua trinh demo, preview job duoc dung de chung minh co che parameter va deploy preview. Moi truong dev/staging bang Argo CD la moi truong chinh de chung minh ung dung full web chay on dinh.
```

### 7. Argo CD dev/staging

Anh can chup:

```bash
kubectl get applications -n argocd
```

Anh UI Argo CD:

```text
yas-root-dev Synced/Healthy
yas-root-staging Synced/Healthy
Mot application service, vi du yas-tax-dev
```

GitOps repo:

```bash
cd /tmp/yas-deployment
git log --oneline -5
sed -n '1,40p' envs/dev/tax-values.yaml
sed -n '1,40p' envs/staging/tax-values.yaml
```

Noi dung:

```text
Nhom su dung Argo CD cho phan nang cao GitOps. Jenkins khong deploy truc tiep dev/staging ma cap nhat image tag trong GitOps repo DoubleHo05/yas-deployment. Argo CD theo doi repo nay va tu dong sync vao namespace dev/staging.
```

Luồng dev:

```text
Merge main -> Jenkins update envs/dev/<service>-values.yaml -> Argo CD sync dev.
```

Luồng staging:

```text
Push release tag vX.Y.Z -> Jenkins update envs/staging/<service>-values.yaml -> Argo CD sync staging.
```

### 8. Application demo

Anh/log can co:

```bash
kubectl get pods -n dev
kubectl get pods -n staging
kubectl get svc -n ingress-nginx ingress-nginx-controller
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

Anh browser:

```text
Trang storefront hien san pham
API tra ve productList co data
```

Noi dung:

```text
Vi khong co DNS that, nhom su dung hosts file tro domain storefront.yas.local.com ve external IP cua worker node. Ingress-nginx expose NodePort 30303 de truy cap web.
```

Giai thich port `30303`:

```text
30303 la NodePort duoc Kubernetes cap cho service ingress-nginx-controller port HTTP 80. Developer truy cap bang domain:30303 vi khong dung LoadBalancer/DNS that.
```

### 9. Observability

Anh can chup:

```bash
kubectl get pods -n observability
kubectl get svc -n observability
```

Grafana:

```text
Data sources co Prometheus, Loki, Tempo
Explore Prometheus query: up
Explore Loki query: {namespace="staging"}
Explore Tempo query: {resource.service.name="telemetrygen"}
```

Noi dung:

```text
Nhom trien khai observability stack gom Prometheus, Grafana, Loki, Promtail, Tempo va OpenTelemetry Collector. Prometheus thu metrics, Loki luu logs, Tempo luu traces va Grafana la giao dien quan sat tap trung.
```

Ghi chu trung thuc:

```text
Promtail duoc cau hinh chay tren worker node de tranh loi "too many open files" tren master. Trace pipeline duoc kiem chung bang telemetrygen gui trace vao OpenTelemetry Collector va hien thi tren Tempo.
```

### 10. Service Mesh

Can lay thong tin tu ban da lam service mesh.

Anh/log bat buoc:

```bash
kubectl get pods -n istio-system
kubectl get pods -n mesh-demo
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
kubectl get peerauthentication -n mesh-demo -o yaml
kubectl get authorizationpolicy -n mesh-demo -o yaml
kubectl get virtualservice -n mesh-demo -o yaml
```

Curl evidence:

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://<service>:<port>/<path>
kubectl exec -n mesh-demo blocked-client -c curl -- curl -i http://<service>:<port>/<path>
```

Can co:

```text
allowed-client: request duoc phep.
blocked-client: HTTP 403 hoac RBAC access denied.
retry test: service tra 500 va VirtualService co retries attempts=3.
```

Anh:

```text
Kiali topology namespace mesh-demo
```

Noi dung:

```text
Nhom dung Istio sidecar mode va Kiali de thuc hien service mesh trong namespace mesh-demo. Namespace nay duoc tach rieng de khong anh huong den dev/staging. PeerAuthentication bat mTLS STRICT, AuthorizationPolicy gioi han service duoc phep giao tiep, VirtualService cau hinh retry khi gap loi 5xx.
```

### 11. Kho khan va cach xu ly

Nen co bang:

| Van de | Nguyen nhan | Cach xu ly |
|---|---|---|
| Strimzi CRD khong khop | Version moi dung `v1`, chart YAS dung `v1beta2` | Dung Strimzi 0.45.2 |
| Elasticsearch search loi | ES 8.8.1 khong tuong thich app search | Nang cap ES len 9.2.3 qua buoc 8.19.6 |
| BFF khong resolve Keycloak | Domain `identity.yas.local.com` khong co DNS trong cluster | Patch hostAliases den `keycloak-service` |
| Storefront API 500/404 | BFF can service `storefront-nextjs` va internal `nginx` | Tao service alias va nginx route |
| Promtail master loi | Too many open files | Chay Promtail tren worker |
| Tempo port sai | Tempo chart expose query port 3200, khong phai 3100 | Sua Grafana datasource sang port 3200 |

### 12. Ket luan

Viet:

```text
Nhom da hoan thanh he thong CD cho YAS voi Jenkins-first pipeline, Docker Hub image registry, K3s cluster, Argo CD GitOps cho dev/staging, developer preview, observability va service mesh. He thong chung minh duoc qua cac bang chung build/push image, Argo CD sync, ung dung web chay tren NodePort/domain local, Grafana quan sat metrics/logs/traces va Istio/Kiali thuc hien mTLS, retry, authorization policy.
```

## 5. Checklist anh can co truoc khi nop

### Bat buoc CD

- [ ] Cluster nodes 1 master + 1 worker.
- [ ] Namespace list.
- [ ] Docker Hub image tags main/commit SHA/branch.
- [ ] Jenkins branch/developer build thanh cong.
- [ ] Jenkins main build/update GitOps thanh cong.
- [ ] developer_build parameters.
- [ ] preview deployment/service/NodePort.
- [ ] cleanup job.
- [ ] Argo CD dev applications.
- [ ] Argo CD staging applications.
- [ ] GitOps repo values file duoc update tag.
- [ ] Storefront web/API chay duoc.

### Observability

- [ ] `kubectl get pods -n observability`.
- [ ] Grafana datasources Prometheus/Loki/Tempo.
- [ ] Prometheus query `up`.
- [ ] Loki query `{namespace="staging"}`.
- [ ] Tempo query `{resource.service.name="telemetrygen"}`.

### Service Mesh

- [ ] `kubectl get pods -n istio-system`.
- [ ] `kubectl get pods -n mesh-demo` co sidecar.
- [ ] PeerAuthentication mTLS STRICT.
- [ ] AuthorizationPolicy YAML.
- [ ] VirtualService retry YAML.
- [ ] Curl allowed.
- [ ] Curl denied.
- [ ] Retry evidence 500.
- [ ] Kiali topology screenshot.

## 6. Cach copy vao Google Docs

1. Mo file `outputs/PROJECT2_REPORT_DRAFT_TO_PASTE.md`.
2. Copy tung section sang Google Docs.
3. Chuyen cac heading thanh Heading 1/2 trong Google Docs.
4. Chen anh ngay duoi doan mo ta tuong ung.
5. Duoi moi anh them caption.
6. Chay spell check.
7. Export `.docx`.

Khong paste toan bo command output qua dai vao than bai. Cac log dai dua vao phu luc hoac chup man hinh phan quan trong.

