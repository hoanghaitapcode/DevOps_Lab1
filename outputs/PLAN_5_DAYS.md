# Plan 5 Ngày - Phân công 4 thành viên

## Mục tiêu tổng

Hoàn thành chắc phần bắt buộc trước ngày 3. Ngày 4 làm Argo CD và Istio theo mức có thể. Ngày 5 đóng băng kỹ thuật, chụp bằng chứng, viết README/report và luyện demo.

Phương án chính luôn là Jenkins cũ Project 1 làm CI/CD. Google Cloud chỉ chạy K3s/YAS workload.

## Vai trò

| Thành viên | Vai trò | Phụ trách chính |
|---|---|---|
| Thành viên 1 | Infrastructure Lead | GCP, VM, K3s master/worker, firewall, kubeconfig/service account Jenkins, Docker Hub credential hỗ trợ Jenkins, Argo CD base |
| Thành viên 2 | Jenkins CI & Container Lead | Nâng Jenkinsfile Project 1, change detection, test/build/coverage/scan, Docker build/push, commit SHA tag, tối ưu Jenkins 8GB |
| Thành viên 3 | Jenkins CD & GitOps Lead | Helm/K8s manifest, `developer_build`, cleanup job, preview namespace, NodePort/domain, Argo CD dev/staging |
| Thành viên 4 | Service Mesh & QA/Report Lead | Istio, mTLS, AuthorizationPolicy, retry, Kiali, curl tests, screenshot/log, README/report |

## Ngày 1 - Khóa nền bắt buộc

| Thành viên | Việc |
|---|---|
| TV1 | Tạo GCP VM `yas-master`, `yas-worker`; cấu hình firewall SSH/internal/NodePort demo; cài K3s server/agent; kiểm tra `kubectl get nodes`. |
| TV2 | Kiểm tra Jenkins tool `JDK25` đúng theo thống nhất nhóm; sửa JUnit glob; thêm service map UI/backend; chuẩn bị stage Docker build/push tuần tự. |
| TV3 | Audit Helm chart hiện có; xác định cách override Docker Hub image/tag cho từng service; viết skeleton `developer_build`. |
| TV4 | Tạo checklist bằng chứng; rà soát yêu cầu Lab 1 còn thiếu screenshot; chuẩn bị template README/report. |

Output cuối ngày:

- K3s có 1 master + 1 worker Ready.
- Jenkins cũ vẫn truy cập được.
- Docker Hub repo/image naming thống nhất.
- Draft Jenkinsfile build/push.
- Draft spec `developer_build`.

Checklist:

```bash
kubectl get nodes -o wide
kubectl get ns
docker login
git branch -a
```

## Ngày 2 - CI build/push image

| Thành viên | Việc |
|---|---|
| TV1 | Tạo service account/kubeconfig cho Jenkins, test từ Jenkins agent `kubectl get nodes`; tạo Docker Hub credential trong Jenkins. |
| TV2 | Hoàn thành Docker build/push trong Jenkinsfile: tag commit SHA, main, branch alias; test với 1 service nhỏ như `tax`. |
| TV3 | Tạo Helm override cho preview; deploy thủ công 1-2 service bằng tag `main`/commit SHA vào K3s. |
| TV4 | Ghi log/screenshot Jenkins CI, Docker Hub image tag, coverage, Gitleaks/Sonar/Snyk. |

Output cuối ngày:

- Push được `docker.io/<user>/yas-tax:<commit-sha>`.
- Push được `docker.io/<user>/yas-tax:main` khi branch main.
- Jenkins kết nối được K3s.
- Helm deploy thủ công được service mẫu.

Checklist:

```bash
docker pull docker.io/<user>/yas-tax:<commit-sha>
kubectl auth can-i get pods --all-namespaces
helm list -A
kubectl get pods -n preview-manual
```

## Ngày 3 - Hoàn thành CD preview và cleanup

| Thành viên | Việc |
|---|---|
| TV1 | Cố định Worker external IP, kiểm tra firewall NodePort, hỗ trợ dependency YAS tối thiểu. |
| TV2 | Fix lỗi CI/CD, đảm bảo Jenkins không build song song quá tải; document image tag strategy. |
| TV3 | Hoàn thành Jenkins job `developer_build` và `developer_cleanup`; deploy preview bằng branch/tag parameter; expose NodePort. |
| TV4 | Chạy QA flow e-commerce tối thiểu; ghi test plan; chụp screenshot preview và cleanup. |

Output cuối ngày:

- Job `developer_build` chạy được.
- Service branch riêng dùng commit SHA, service còn lại dùng `main`.
- Có URL `http://yas-preview.local:30080`.
- Cleanup job xóa được preview namespace.

Checklist:

```bash
kubectl get all -n preview-demo
kubectl get svc -n preview-demo
curl -I http://yas-preview.local:30080
kubectl delete namespace preview-demo --dry-run=client -o yaml
```

Mốc bắt buộc: hết ngày 3 phải xong phần bắt buộc. Nếu chưa xong, dừng Argo/Istio và chỉ tập trung fix preview.

## Ngày 4 - Nâng cao Argo CD và Istio

| Thành viên | Việc |
|---|---|
| TV1 | Cài Argo CD namespace `argocd`; nếu đủ tài nguyên, cài Istio base/control-plane và Kiali. |
| TV2 | Thêm logic release tag `vX.Y.Z` push image tag release; đảm bảo main push tag `main`. |
| TV3 | Tạo Argo CD app cho `dev` và `staging`; main auto deploy dev, release tag deploy staging qua GitOps values. |
| TV4 | Tạo manifest mTLS, AuthorizationPolicy, VirtualService retry; chạy curl allow/deny/retry; chụp Kiali topology. |

Output cuối ngày:

- Argo CD sync được ít nhất namespace `dev`.
- Nếu kịp, tag `v1.2.3` deploy `staging`.
- Có manifest Istio và evidence curl/Kiali.

Checklist:

```bash
kubectl get pods -n argocd
kubectl get applications -n argocd
kubectl get peerauthentication -A
kubectl get authorizationpolicy -A
kubectl get virtualservice -A
```

## Ngày 5 - Đóng gói, bằng chứng, demo

| Thành viên | Việc |
|---|---|
| TV1 | Chụp GCP VM, K3s nodes, firewall, service account/kubeconfig proof không lộ token. |
| TV2 | Chụp Jenkins CI, changed service detection, Docker Hub tags, test result, coverage, scans. |
| TV3 | Chụp developer_build, preview NodePort, cleanup, Argo CD nếu có. |
| TV4 | Gom screenshot/log, hoàn thiện README/report `.docx`, chạy rehearsal demo 2 lần. |

Output cuối ngày:

- README hướng dẫn từng bước.
- Báo cáo `.docx`.
- Screenshot/log đầy đủ.
- Demo script cuối cùng.

Checklist trước nộp:

- Không commit secret/token/password thật.
- Jenkins cũ được nhấn mạnh là phương án chính.
- Có ảnh Jenkins Project 1/Project 2.
- Có ảnh Docker Hub tag commit SHA.
- Có ảnh K3s 1 master + 1 worker.
- Có ảnh preview URL.
- Có ảnh cleanup.
- Nếu làm nâng cao: Argo CD, Kiali, mTLS, AuthorizationPolicy, retry evidence.

## Scope cắt giảm nếu bị trễ

Thứ tự cắt:

1. Full observability Grafana/Loki/Tempo/Prometheus.
2. Backoffice UI/BFF nếu demo storefront đủ.
3. Các service ngoài flow e-commerce như promotion, rating, payment, recommendation, webhook, location.
4. Argo CD staging release tag nếu dev đã đủ.
5. Istio AuthorizationPolicy nhiều service; giữ 1 allow/deny case rõ.

Không được cắt:

- K3s 1 master + 1 worker.
- Jenkins cũ CI/CD.
- Docker Hub image tag commit SHA.
- `developer_build`.
- Preview NodePort/domain hosts.
- Cleanup job.

## Scope demo tối thiểu nếu tài nguyên thiếu

Giữ flow e-commerce:

- `storefront-ui`
- `storefront-bff`
- `product`
- `cart`
- `order`
- `inventory`
- `tax`
- `customer`
- `search` nếu kịp

`sampledata` chạy 1 lần rồi có thể scale down hoặc xóa job/pod để tiết kiệm tài nguyên.
