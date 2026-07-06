# Thành viên 4 - Service Mesh & QA/Report Lead

File này là checklist làm việc chính của bạn. Mục tiêu của bạn là đảm bảo demo có bằng chứng rõ ràng, report sạch, và làm phần Istio/Kiali nâng cao nếu phần bắt buộc đã ổn.

## Kết quả phải bàn giao

Phần bắt buộc:

- Test plan cho CI/CD preview.
- Screenshot/log đầy đủ.
- README/report mô tả đúng: Project 2 kế thừa Jenkins Project 1.
- Evidence chứng minh:
  - Jenkins cũ chạy CI/CD.
  - Docker image có commit SHA.
  - K3s có 1 master + 1 worker.
  - `developer_build` deploy đúng branch/tag.
  - NodePort preview truy cập được.
  - Cleanup xóa được preview.

Phần nâng cao nếu kịp:

- Istio mTLS manifest.
- AuthorizationPolicy allow/deny.
- VirtualService retry.
- Kiali topology screenshot.
- Curl logs chứng minh allow/deny/retry.

## Nguyên tắc viết báo cáo

- Không nói Project 2 thiết kế lại từ đầu.
- Không nói thay Jenkins bằng GitHub Actions.
- Không nói Jenkins mới là phương án chính.
- Phải nhấn mạnh Jenkins cũ Project 1 được tái sử dụng.
- Google Cloud chỉ chạy K3s/workload YAS.
- Docker tag chính theo đề là commit SHA.
- Branch alias chỉ là tag phụ.
- Không đưa secret/token/password thật vào ảnh/report.

## Ngày 1 - Chuẩn bị checklist bằng chứng

Tạo thư mục bằng chứng ngoài repo hoặc trong repo nếu chỉ chứa ảnh đã che secret:

```text
evidence/
  lab1/
  k3s/
  jenkins-ci/
  dockerhub/
  developer-build/
  cleanup/
  argocd/
  istio/
```

Danh sách ảnh Project 1 cần xin từ nhóm:

- GitHub branch protection `main`.
- Rule 2 reviewers.
- Required CI pass.
- PR open.
- Jenkins Multibranch Pipeline.
- Test result.
- Coverage report.
- Gitleaks.
- SonarCloud/SonarQube.
- Snyk.

Nếu chưa có ảnh, ghi rõ trong audit/report là “chưa có screenshot, cần bổ sung trước khi nộp”.

Done ngày 1 khi:

- Có checklist ảnh.
- Biết ai trong nhóm cung cấp ảnh nào.

## Ngày 2 - QA CI và Docker image

Khi thành viên 2 chạy Jenkins branch demo, bạn kiểm tra:

```bash
git rev-parse --short=12 origin/dev_tax_service
```

Docker Hub phải có tag giống SHA đó.

Evidence cần chụp:

- Jenkins stage `Detect Changed Services`, thấy chỉ `tax`.
- Jenkins test/build/coverage.
- Jenkins Docker push.
- Docker Hub `yas-tax:<commit-sha>`.

Ghi log kiểm tra:

```text
Branch demo: dev_tax_service
Service changed: tax
Commit SHA: <sha>
Docker image: docker.io/<dockerhub-user>/yas-tax:<sha>
Result: PASS/FAIL
```

Done ngày 2 khi:

- Có bằng chứng image commit SHA.
- Có bằng chứng Jenkins không build full monorepo.

## Ngày 3 - QA developer_build và cleanup

Sau khi thành viên 3 deploy preview:

```bash
kubectl get nodes -o wide
kubectl get ns | grep preview
kubectl get pods -n preview-demo
kubectl get svc -n preview-demo
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl -I http://yas-preview.local:30080
```

Kỳ vọng:

- Namespace `preview-demo` tồn tại.
- Pod Ready.
- Image `tax` dùng commit SHA branch `dev_tax_service`.
- Preview URL trả HTTP response.

Sau cleanup:

```bash
kubectl get ns preview-demo
```

Kỳ vọng namespace không còn hoặc NotFound.

Done ngày 3 khi:

- Có ảnh/log `developer_build`.
- Có ảnh/log preview URL.
- Có ảnh/log cleanup.

## Ngày 4 - Istio/Kiali nâng cao

Chỉ làm nếu bắt buộc đã xong. Nếu bắt buộc chưa ổn, bỏ Istio và tập trung report/demo.

### 1. Enable injection

Ví dụ namespace preview:

```bash
kubectl label namespace preview-demo istio-injection=enabled --overwrite
kubectl rollout restart deployment -n preview-demo
kubectl get pods -n preview-demo
```

Kiểm tra pod có sidecar:

```bash
kubectl get pod -n preview-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

### 2. mTLS strict

Tạo `mtls-strict.yaml`:

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default
  namespace: preview-demo
spec:
  mtls:
    mode: STRICT
```

Apply:

```bash
kubectl apply -f mtls-strict.yaml
kubectl get peerauthentication -n preview-demo
```

### 3. AuthorizationPolicy allow/deny

Ý tưởng demo đơn giản:

- Cho phép `storefront-bff` gọi `tax`.
- Pod curl lạ không được gọi `tax`.

Ví dụ policy cần chỉnh label theo service account/pod thật:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: tax-allow-storefront-bff
  namespace: preview-demo
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: tax
  action: ALLOW
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/preview-demo/sa/storefront-bff
```

Nếu service account name khác, lấy bằng:

```bash
kubectl get pod -n preview-demo -o jsonpath='{range .items[*]}{.metadata.name}{" sa="}{.spec.serviceAccountName}{" labels="}{.metadata.labels}{"\n"}{end}'
```

### 4. Retry VirtualService

Tạo `tax-retry.yaml`:

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: tax-retry
  namespace: preview-demo
spec:
  hosts:
    - tax
  http:
    - route:
        - destination:
            host: tax
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx
```

Apply:

```bash
kubectl apply -f tax-retry.yaml
kubectl get virtualservice -n preview-demo
```

### 5. Curl evidence

Allow case:

```bash
kubectl exec -n preview-demo deploy/storefront-bff -- \
  curl -i http://tax/tax/actuator/health
```

Deny case:

```bash
kubectl run curl-test -n preview-demo --image=curlimages/curl:8.8.0 -it --rm -- sh
curl -i http://tax/tax/actuator/health
```

Kỳ vọng deny có thể là 403 hoặc RBAC denied tùy policy.

### 6. Kiali

Chụp:

- Graph/topology có service YAS.
- Namespace `preview-demo` hoặc `dev`.
- Traffic line giữa services nếu có.

Done ngày 4 khi:

- Có ít nhất mTLS + Kiali screenshot.
- Nếu kịp, có allow/deny hoặc retry evidence.

## Ngày 5 - Report và demo script

### Report structure gợi ý

1. Giới thiệu.
2. Kế thừa Project 1.
3. Kiến trúc Project 2.
4. CI flow.
5. Docker image tag strategy.
6. K3s trên GCP.
7. `developer_build`.
8. Cleanup.
9. Argo CD nâng cao nếu có.
10. Istio/Kiali nâng cao nếu có.
11. Evidence.
12. Kết luận và hạn chế.

### Demo script ngắn

Nói:

> Nhóm kế thừa Jenkins Project 1. Jenkins cũ vẫn branch scan, test, coverage, security scan, build Docker image và push Docker Hub. GCP chỉ chạy K3s cluster. Preview được deploy bằng Jenkins job `developer_build`.

Chạy/show:

```bash
kubectl get nodes -o wide
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

## Checklist trước khi nộp

- Có đủ ảnh bắt buộc.
- Ảnh không lộ secret.
- Report nhấn mạnh Jenkins cũ.
- Không có đề xuất GitHub Actions cho phần mới.
- Không có Jenkins mới là phương án chính.
- Có command kiểm tra nhanh.
- Có giải thích nếu scope demo bị cắt.
- Có link repo nhóm.
- Có link Docker Hub hoặc ảnh Docker Hub.
- Có URL preview/hosts instruction.

## Câu nói trong demo

> Phần nâng cao Istio/Kiali được đặt sau phần bắt buộc. Nhóm ưu tiên hoàn thành chắc CI/CD preview trước, sau đó mới bật service mesh để chứng minh mTLS, authorization, retry và topology.
