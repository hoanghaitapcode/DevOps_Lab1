# Demo Checklist Project 2

## Thứ tự demo khuyến nghị

1. Mở GitHub repo nhóm.
   - Cho thấy repo fork/clone từ YAS.
   - Cho thấy branch protection/ruleset `main`.
   - Cho thấy PR open và required CI.

2. Mở Jenkins cũ Project 1.
   - Cho thấy Multibranch Pipeline.
   - Cho thấy Jenkinsfile Project 1 được kế thừa.
   - Nhấn mạnh không dùng GitHub Actions cho CD mới.

3. Demo CI service changed only.
   - Push branch `dev_tax_service`.
   - Jenkins detect chỉ `tax`.
   - Jenkins chạy test/build/coverage/scans tuần tự.

4. Demo Docker image tag.
   - Mở Docker Hub image `yas-tax`.
   - Có tag `<commit-sha>`.
   - Có tag `main` nếu build từ main.
   - Có thể có tag alias `dev_tax_service`.

5. Demo K3s trên Google Cloud.
   - `kubectl get nodes -o wide`.
   - Có `yas-master` và `yas-worker`.
   - Workload chạy trên worker/GCP, không chạy trên Jenkins.

6. Demo `developer_build`.
   - Chạy Jenkins job với `TAX_BRANCH=dev_tax_service`, service khác `main`.
   - Kiểm tra Helm release/pod.
   - Kiểm tra image của deployment `tax` dùng commit SHA.

7. Demo preview NodePort/domain.
   - Hosts file có `<WORKER_EXTERNAL_IP> yas-preview.local`.
   - Mở `http://yas-preview.local:30080`.
   - Chạy curl nhanh.

8. Demo cleanup.
   - Chạy Jenkins cleanup job.
   - `kubectl get ns preview-demo` không còn.

9. Demo nâng cao nếu có.
   - Argo CD app `dev`, `staging`.
   - Istio mTLS.
   - Kiali topology.
   - AuthorizationPolicy allow/deny.
   - Retry evidence.

## Screenshot/log cần chuẩn bị

Project 1:

- GitHub repo link.
- PR open.
- Branch protection/ruleset yêu cầu 2 approvals.
- Required CI pass before merge.
- Jenkins multibranch job.
- Jenkins build log detect changed service.
- Test result.
- Coverage report, threshold 70%.
- Gitleaks.
- SonarCloud/SonarQube.
- Snyk.

Project 2 bắt buộc:

- GCP VM `yas-master`, `yas-worker`.
- `kubectl get nodes -o wide`.
- Jenkins credential list che giá trị secret.
- Docker Hub image tags `main` và commit SHA.
- Jenkins job `developer_build` parameters.
- Jenkins deploy log.
- `kubectl get pods -n preview-demo`.
- `kubectl describe deployment tax -n preview-demo` hoặc jsonpath image.
- NodePort service.
- Browser/curl `http://yas-preview.local:30080`.
- Cleanup job log.
- Namespace đã bị xóa.

Nâng cao:

- Argo CD Applications.
- Argo CD sync status Healthy/Synced.
- Release tag `v1.2.3`.
- Istio `PeerAuthentication`.
- Istio `AuthorizationPolicy`.
- Istio `VirtualService` retry.
- Kiali topology.
- Curl allow/deny log.
- Retry log.

## Command kiểm tra nhanh

K3s:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl top nodes
kubectl top pods -A
```

Preview:

```bash
kubectl get ns | grep preview
kubectl get all -n preview-demo
kubectl get svc -n preview-demo
kubectl get deployment tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl -I http://yas-preview.local:30080
```

Helm:

```bash
helm list -n preview-demo
helm get values tax -n preview-demo
helm status tax -n preview-demo
```

Docker Hub image local check:

```bash
docker pull docker.io/<dockerhub-user>/yas-tax:<commit-sha>
docker image inspect docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

Cleanup:

```bash
kubectl delete namespace preview-demo --ignore-not-found=true
kubectl get ns preview-demo
```

Argo CD:

```bash
kubectl get pods -n argocd
kubectl get applications -n argocd
kubectl describe application yas-dev -n argocd
```

Istio/Kiali:

```bash
kubectl get pods -n istio-system
kubectl get peerauthentication -A
kubectl get authorizationpolicy -A
kubectl get virtualservice -A
kubectl exec -n preview-demo deploy/storefront-bff -- curl -i http://tax/tax/actuator/health
```

## Checklist trước khi nộp

- Jenkins cũ Project 1 là phương án chính trong báo cáo.
- Không có đoạn nào nói thay Jenkins bằng GitHub Actions.
- Nếu nhắc Jenkins mới, ghi rõ là phương án dự phòng.
- Docker tag commit SHA là tag bắt buộc.
- Branch alias chỉ là tag phụ hỗ trợ deploy.
- Không hardcode secret/token/password thật.
- Không chạy full YAS trên Jenkins.
- Không build 14 service song song.
- Có bằng chứng cleanup.
- README có hướng dẫn hosts file:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

## Demo script ngắn

Lời dẫn:

> Project 2 kế thừa Jenkins CI của Project 1. Jenkins cũ vẫn làm branch scanning, test, coverage, security scan, build và push image. Google Cloud chỉ chạy K3s và workload YAS. Với preview, Jenkins job `developer_build` nhận branch/tag từng service, service không nhập thì dùng tag `main`.

Flow:

```bash
git checkout -b dev_tax_service
# sửa nhỏ tax
git add tax
git commit -m "demo: update tax service"
git push origin dev_tax_service
```

Sau Jenkins CI:

```bash
COMMIT_SHA=$(git rev-parse --short=12 origin/dev_tax_service)
docker pull docker.io/<dockerhub-user>/yas-tax:$COMMIT_SHA
```

Sau `developer_build`:

```bash
kubectl get pods -n preview-demo
kubectl get deployment tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl -I http://yas-preview.local:30080
```

Cleanup:

```bash
kubectl get ns preview-demo
# chạy Jenkins developer_cleanup
kubectl get ns preview-demo
```
