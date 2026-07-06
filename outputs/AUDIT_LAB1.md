# Audit Project 1 - CI YAS

Ngày audit: 2026-06-25
Repo local: `/Users/giabao/Documents/devops/DevOps_Lab1`
Remote hiện thấy: `https://github.com/hoanghaitapcode/DevOps_Lab1.git`

## Kết luận nhanh

Project 1 đã có nền CI bằng Jenkins và có thể tái sử dụng cho Project 2. Jenkinsfile hiện tại đã làm được các phần quan trọng: phát hiện service thay đổi trong monorepo, chạy test/build tuần tự, publish JUnit, publish JaCoCo coverage, enforce coverage 70%, Gitleaks, SonarCloud và Snyk.

Chưa thấy trong repo phần Docker build/push image lên Docker Hub, chưa thấy Jenkins CD job `developer_build`, chưa thấy cleanup job, chưa thấy manifest Argo CD/Istio/Kiali. Vì vậy Project 2 nên mở rộng Jenkinsfile/Jenkins job hiện có, không thay bằng GitHub Actions và không cài Jenkins mới nếu Jenkins Project 1 còn truy cập được.

## Bằng chứng đã đọc trong repo

- `Jenkinsfile`: pipeline CI chính.
- `.github/workflows/gitleaks.yml`, `.github/workflows/gitleaks-check.yaml`, `.github/workflows/codeql.yml`: workflow GitHub Actions còn tồn tại, chỉ nên audit, không phát triển hướng mới bằng GitHub Actions.
- `gitleaks.toml`: cấu hình Gitleaks, có allowlist cho realm/config mẫu.
- `pom.xml` và nhiều `*/pom.xml`: có JaCoCo và SonarCloud properties.
- `storefront/package.json`, `backoffice/package.json`: có `build`, `lint`, chưa thấy test script.
- `Dockerfile` ở các service chính: backend, BFF, UI.
- `k8s/charts/*`: có Helm chart cho nhiều service YAS.
- `k8s/deploy/*`: script deploy dependency/YAS lên Kubernetes, hiện thiên về minikube/local.
- Không thấy file Argo CD/Istio/Kiali trong repo.
- Không thấy thư mục report `.docx` hoặc screenshot Project 1 trong repo local.

## Checklist Project 1

| Yêu cầu | Trạng thái | Ghi chú |
|---|---:|---|
| Fork repo từ `nashtech-garage/yas` | Đạt một phần | Remote là repo nhóm `hoanghaitapcode/DevOps_Lab1`; nội dung là YAS. Chưa xác minh được metadata fork từ repo local. |
| Branch protection cho `main` | Chưa thấy trong repo | Cần screenshot GitHub Settings/Rulesets. |
| Ít nhất 2 reviewer approve | Chưa thấy trong repo | Cần screenshot branch protection/ruleset. |
| CI pass mới merge `main` | Chưa thấy trong repo | Cần screenshot required status checks. |
| Tối thiểu 1 PR đang open | Chưa thấy trong repo | Cần link/screenshot PR open. |
| Jenkins quét branch | Có dấu hiệu | `Jenkinsfile` dùng biến `BRANCH_NAME`, `CHANGE_ID`, `CHANGE_TARGET`, phù hợp Multibranch Pipeline. Cần screenshot Jenkins job scan branch. |
| Pipeline chạy theo từng branch | Có | Logic PR/branch trong `Jenkinsfile`. |
| Phase test | Có | Stage `Test`. |
| Phase build | Có | Stage `Build`. |
| Upload test result | Có | `junit(testResults: ...)`. |
| Upload coverage report | Có | `recordCoverage` với JaCoCo XML. |
| Monorepo change detection | Có | Stage `Detect Changed Services`. |
| Chỉ build/test service thay đổi | Có | Duyệt `CHANGED_SERVICES` tuần tự. |
| Unit test tăng coverage | Có dấu hiệu | Có nhiều test trong `media`, `customer`, `tax`, v.v. Cần screenshot coverage thực tế. |
| Enforce coverage > 70% | Có | `recordCoverage` threshold 70 line, criticality `FAILURE`. |
| Gitleaks | Có | Jenkins stage `Gitleaks Scan`, thêm GitHub Actions Gitleaks. |
| SonarQube/SonarCloud | Có | Jenkins stage `SonarQube Analysis`; dùng SonarCloud org `hoanghaitapcode`. |
| Snyk | Có | Jenkins stage `Snyk Security Scan`; dùng Jenkins credential `snyk-token`. |
| Hardcode secret/token/password | Có rủi ro | Repo có secret mẫu Keycloak/client-secret, password `admin`, `.env`. Một số có `#gitleaks:allow` hoặc allowlist. Không hardcode secret thật mới. |
| README hướng dẫn CI/demo | Thiếu | README vẫn là README gốc YAS, chưa thấy hướng dẫn Jenkins CI của nhóm. |

## Vấn đề Critical

1. Chưa có Docker build/push cho Project 2.
   - Jenkinsfile hiện chỉ test/build Maven và scan.
   - Cần thêm stage chỉ cho branch cần publish image: build service thay đổi, tag `<commit-sha>`, `main`, và tùy chọn branch alias.

2. Chưa có Jenkins job `developer_build`.
   - Đây là yêu cầu bắt buộc Project 2.
   - Nên tạo Pipeline job parameterized trong Jenkins cũ, không tạo GitHub Actions.

3. Chưa có cleanup job cho preview deployment.
   - Cần job xóa namespace/release preview.

4. Chưa có hướng dẫn Jenkins kết nối K3s.
   - Cần kubeconfig/service account cho Jenkins, lưu trong Jenkins Credentials.
   - Không commit kubeconfig/token vào repo.

5. Chưa có bằng chứng branch protection/PR/Jenkins screenshots trong repo.
   - Cần nhóm chụp lại từ GitHub/Jenkins để nộp.

## Vấn đề Important

1. README gốc vẫn nhắc GitHub Actions và công nghệ Java 25/Spring Boot 4.0.
   - Nhóm đã thống nhất tiếp tục dùng JDK25 theo hiện trạng Jenkins Project 1 và fork YAS hiện tại.
   - Báo cáo Project 2 phải nói rõ CI/CD chính dùng Jenkins cũ; GitHub Actions chỉ là file cũ/gốc, không dùng cho phần CD mới.

2. `Jenkinsfile` dùng `jdk 'JDK25'`.
   - Đây là cấu hình nhóm đang thống nhất dùng.
   - Cần kiểm tra Jenkins cũ đã có tool tên đúng `JDK25`; nếu chưa có thì cấu hình Jenkins tool, không đổi xuống JDK21.

3. Pattern JUnit hiện là `TEST-.xml`, có thể sai.
   - Nên dùng `**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml`.

4. `SNYK_TOKEN = credentials('snyk-token')` nhưng stage gọi `snyk test` không truyền token rõ ràng.
   - Nếu Jenkins plugin/CLI không tự đọc, cần `SNYK_TOKEN=${SNYK_TOKEN}` trong environment hoặc `snyk auth`.

5. Helm chart mặc định dùng `ghcr.io/nashtech-garage/...:latest`.
   - Project 2 yêu cầu Docker Hub của nhóm; cần override `image.repository` và `image.tag`.

6. `k8s/deploy` hiện hướng dẫn minikube 16GB.
   - Project 2 dùng K3s trên Google Cloud Compute Engine; cần runbook riêng.

## Nice-to-have

1. Tách service registry chung cho Jenkinsfile để map service -> Dockerfile -> image name -> Helm value.
2. Thêm UI service detection cho `storefront`/`backoffice` vì Jenkinsfile hiện chỉ liệt kê Java services.
3. Tạo `docs/ci-cd-project2.md` sau khi chạy thật.
4. Thêm evidence folder cho screenshot/log.

## Phần tái sử dụng cho Project 2

- Jenkins server Project 1: tái sử dụng làm trung tâm CI/CD.
- Jenkins credentials:
  - `snyk-token`.
  - SonarCloud config `SonarCloud`.
  - Cần bổ sung `dockerhub-credentials`.
  - Cần bổ sung `kubeconfig-yas-k3s` hoặc service account token.
- Jenkins branch scanning: tái sử dụng Multibranch Pipeline hiện có.
- Service change detection: tái sử dụng stage `Detect Changed Services`, chỉnh để không build all song song.
- Test/coverage logic: tái sử dụng stage `Test` và `recordCoverage`.
- Security scan: tái sử dụng Gitleaks/Sonar/Snyk.
- Dockerfile từng service: tái sử dụng để build image.
- Helm chart `k8s/charts/*`: tái sử dụng cho preview/dev/staging, override Docker Hub image/tag.

## Jenkins cũ có đủ dùng không?

Kết luận: đủ dùng làm phương án chính nếu Jenkins Project 1 còn truy cập được, còn chạy được Docker build, và có thể kết nối K3s bằng kubeconfig/service account.

Với Jenkins khoảng 8GB RAM:

- Không build toàn bộ 14 service song song.
- Giới hạn executor Jenkins còn 1 hoặc 2.
- Build/test service thay đổi theo monorepo detection.
- Nếu root `pom.xml` hoặc `common-library` thay đổi, build tuần tự và ưu tiên demo services trước.
- Workload YAS chạy trên K3s/GCP, không chạy trên Jenkins.

Chỉ đề xuất Jenkins mới như phương án dự phòng nếu Jenkins Project 1 không truy cập được, hỏng nặng, thiếu tài nguyên không thể xử lý, hoặc không thể kết nối Kubernetes cluster.
