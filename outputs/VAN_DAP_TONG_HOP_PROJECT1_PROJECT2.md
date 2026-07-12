# Vấn Đáp Tổng Hợp Project 1 và Project 2

File này dùng để ôn vấn đáp và thao tác demo trực tiếp trước giảng viên. Mỗi câu gồm:

- **Trả lời miệng**: nói ngắn gọn trước.
- **Nếu thầy hỏi sâu**: giải thích kỹ hơn.
- **Mở ở đâu / chỉ code nào**: nơi cần mở trong GitHub/Jenkins/Kubernetes/Grafana.
- **Demo từng bước**: thao tác cụ thể.

Nguyên tắc trả lời xuyên suốt:

- Jenkins là CI/CD chính.
- YAS là monorepo, nên pipeline phải detect service thay đổi.
- Project 2 dùng Jenkins build/push image, GitOps repo lưu values, Argo CD sync vào K3s.
- GitHub Actions nếu được hỏi chỉ nói là phần Gitleaks/Project 1 cũ, không phải hướng CD mới.
- Không nói Jenkinsfile dùng `changeset`; Jenkinsfile hiện tại dùng `git diff --name-only`.

## A. Project 1 - GitHub, Jenkins CI, Security Scan

## A1. PR có 2 approval và Jenkins check không?

### Trả lời miệng

```text
Dạ có. Nhóm cấu hình bảo vệ branch main: không push trực tiếp vào main, PR cần ít nhất 2 approval và Jenkins check phải pass thì mới merge được. Jenkins check được sinh từ Multibranch Pipeline.
```

### Nếu thầy hỏi sâu

```text
Developer phải tạo branch riêng rồi mở Pull Request vào main. Branch protection/ruleset của GitHub kiểm tra đủ reviewer approve và required status check từ Jenkins. Nếu Jenkins fail hoặc thiếu approval thì GitHub không cho merge.
```

### Mở ở đâu

GitHub repo:

```text
https://github.com/hoanghaitapcode/DevOps_Lab1
```

Vào:

```text
Settings -> Branches -> Branch protection rules
```

hoặc:

```text
Settings -> Rules -> Rulesets
```

### Demo từng bước

1. Mở GitHub repo.
2. Vào `Settings`.
3. Mở rule/ruleset áp dụng cho `main`.
4. Chỉ các mục:

```text
Require a pull request before merging
Required approvals: 2
Require status checks to pass before merging
Required status check: Jenkins/YAS
Block force pushes / restrict direct push nếu có
```

5. Mở một PR.
6. Chỉ phần Checks có Jenkins.
7. Chỉ phần Review có approval.

### Nếu thầy hỏi "Jenkins check nằm đâu?"

```text
Nó nằm trong tab Checks của PR hoặc ngay cuối conversation của Pull Request. Jenkins gửi build status ngược về GitHub thông qua GitHub Branch Source/credential.
```

## A2. Multibranch Pipeline cấu hình như thế nào?

### Trả lời miệng

```text
Nhóm dùng Jenkins Multibranch Pipeline. Jenkins kết nối GitHub repo, bật Discover branches, Discover pull requests và Discover tags. Mỗi branch, PR hoặc tag được Jenkins tạo thành một job con riêng và chạy Jenkinsfile trong repo.
```

### Nếu thầy hỏi sâu

```text
Multibranch Pipeline phù hợp với monorepo và workflow nhiều branch. Jenkins scan SCM, thấy branch/PR/tag nào có Jenkinsfile thì tạo job tương ứng. Build của PR có biến CHANGE_ID/CHANGE_TARGET, build của branch có BRANCH_NAME, build tag có TAG_NAME.
```

### Mở ở đâu

Jenkins:

```text
YAS -> Configure
```

Chỉ các phần:

```text
Branch Sources: GitHub
Repository HTTPS URL
Credentials
Behaviours
Build Strategies
Build Configuration -> Script Path: Jenkinsfile
```

### Demo từng bước

1. Mở Jenkins.
2. Vào job `YAS`.
3. Bấm `Configure`.
4. Kéo tới `Branch Sources`.
5. Chỉ:

```text
GitHub repository URL
Credentials dùng để scan repo
Discover branches
Discover pull requests
Discover tags
```

6. Kéo tới `Build Configuration`.
7. Chỉ:

```text
Script Path: Jenkinsfile
```

8. Bấm `Scan Multibranch Pipeline Now`.
9. Mở scan log và chỉ:

```text
Checking branches...
Checking pull requests...
Checking tags...
Jenkinsfile found
Met criteria
```

## A3. Thầy chỉ vào Build Strategy và hỏi nó hoạt động như thế nào

### Trả lời miệng

```text
Build Strategy quyết định Jenkins có tự động build SCM head vừa scan được hay không. SCM head có thể là branch, pull request hoặc tag. Branch Source phát hiện đối tượng, còn Build Strategy quyết định đối tượng đó có được trigger build không.
```

### Nếu thầy chỉ `Regular branches`

```text
Regular branches dùng để build các branch thông thường như main, dev_tax_service, test_order. Khi developer push commit lên branch, Jenkins scan thấy commit mới và trigger build job của branch đó.
```

### Nếu thầy chỉ `Change requests`

```text
Change requests dùng để build Pull Request. Khi mở PR, Jenkins tạo job PR riêng, dùng CHANGE_ID và CHANGE_TARGET để biết PR đang merge vào branch nào. Pipeline dùng thông tin đó để diff với target branch và detect service thay đổi.
```

### Nếu thầy chỉ `Tags`

```text
Tags dùng để build Git tag như v1.0.0. Nhóm dùng tag cho release staging. Khi push tag v1.0.0, Jenkins build image với tag v1.0.0, push Docker Hub, rồi update GitOps staging để Argo CD deploy vào namespace staging.
```

### Câu lưu ý cực quan trọng

```text
Lúc đầu ô Ignore tags older than để 0 nên Jenkins scan thấy tag nhưng không tự build. Sau khi bỏ số 0, Jenkins tự build tag v1.0.0 bình thường.
```

### Demo từng bước

1. Mở `YAS -> Configure`.
2. Kéo tới `Build Strategies`.
3. Nếu thầy chỉ `Tags`, nói:

```text
Rule này cho phép Jenkins tự build tag release. Với tag v1.0.0, Jenkins dùng TAG_NAME để biết đây là release tag.
```

4. Mở build tag `v1.0.0`.
5. Chỉ console:

```text
Docker Build and Push
docker push docker.io/doubleho/yas-tax:v1.0.0
Update GitOps Manifests
envs/staging/tax-values.yaml -> tag v1.0.0
```

## A4. Khi thay đổi một service thì Jenkins build như thế nào?

### Trả lời miệng

```text
Vì YAS là monorepo nên Jenkins không build toàn bộ hệ thống cho mọi commit. Stage Detect Changed Services dùng git diff để lấy danh sách file thay đổi, map path sang service tương ứng, rồi chỉ test/build/push image service đó.
```

### Mở code ở đâu

File:

```text
Jenkinsfile
```

Stage:

```text
Detect Changed Services
```

Đoạn code chính:

```groovy
if (env.CHANGE_ID) {
    git diff --name-only origin/${env.CHANGE_TARGET}...HEAD
} else {
    git diff --name-only HEAD~1 HEAD
}
```

Map file sang service:

```groovy
if (changedFiles.any { it.startsWith("${service}/") }) {
    changedJavaServices.add(service)
}
```

Nếu sửa dependency chung:

```groovy
file == 'pom.xml' || file.startsWith('common-library/')
```

thì Jenkins build toàn bộ Java services vì dependency chung có thể ảnh hưởng nhiều service.

### Nhóm có dùng `changeset` không?

```text
Dạ không. Nhóm không dùng Declarative changeset. Nhóm dùng custom git diff trong Jenkinsfile vì cần sinh danh sách service động cho monorepo, rồi loop qua từng service để test/build/push image.
```

### Nếu thầy hỏi tại sao không dùng `changeset`

```text
changeset phù hợp với điều kiện đơn giản để bật/tắt stage. Nhưng YAS có nhiều service, nhóm cần biết chính xác service nào thay đổi để đưa vào biến CHANGED_SERVICES. Vì vậy dùng git diff linh hoạt hơn.
```

### Demo từng bước

1. Mở Jenkins build của branch/PR.
2. Mở Console Output.
3. Tìm:

```text
Detect Changed Services
```

4. Chỉ log:

```text
Changed files:
tax/...
Services cần build: tax
Java services: tax
```

5. Chỉ các stage sau chỉ chạy `tax`:

```text
Testing: tax
Building: tax
Building docker.io/doubleho/yas-tax:<commit-sha>
docker push docker.io/doubleho/yas-tax:<commit-sha>
docker push docker.io/doubleho/yas-tax:<branch-name>
```

## A5. Jenkins push Docker Hub như thế nào? Ở đâu trong code?

### Trả lời miệng

```text
Sau khi detect service thay đổi, Jenkins lấy commit SHA ngắn, build Docker image cho từng service, tag image bằng commit SHA và branch name, rồi push lên Docker Hub bằng credential `dockerhub-credentials`.
```

### Mở code ở đâu

File:

```text
Jenkinsfile
```

Stage:

```text
Docker Build and Push
```

Đoạn quan trọng:

```groovy
def commitSha = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
def branchAlias = env.BRANCH_NAME.replaceAll('[^A-Za-z0-9_.-]', '-')
```

Credential:

```groovy
withCredentials([usernamePassword(
    credentialsId: 'dockerhub-credentials',
    usernameVariable: 'DOCKER_USER',
    passwordVariable: 'DOCKER_PASS'
)])
```

Build/push:

```groovy
docker build -t ${image}:${commitSha} -t ${image}:${branchAlias} ./${svc}
docker push ${image}:${commitSha}
docker push ${image}:${branchAlias}
```

Nếu branch là main:

```groovy
docker tag ${image}:${commitSha} ${image}:main
docker push ${image}:main
```

### Demo từng bước

1. Mở Jenkins build.
2. Tìm stage `Docker Build and Push`.
3. Chỉ:

```text
git rev-parse --short=12 HEAD
docker build -t docker.io/doubleho/yas-tax:<sha>
docker push docker.io/doubleho/yas-tax:<sha>
docker push docker.io/doubleho/yas-tax:<branch>
```

4. Mở Docker Hub:

```text
https://hub.docker.com/repository/docker/doubleho/yas-tax/tags
```

5. Chỉ tag:

```text
<commit-sha>
<branch-name>
main hoặc v1.0.0 nếu có
```

## A6. Demo tạo PR/push branch thì Jenkins trigger job

### Trả lời miệng

```text
Khi developer push branch hoặc tạo Pull Request, Jenkins Multibranch Pipeline scan GitHub repo, phát hiện branch/PR mới, đọc Jenkinsfile và trigger build tương ứng. Kết quả build được trả về GitHub PR dưới dạng status check.
```

### Demo push branch

1. Tạo branch demo:

```bash
git switch -c demo-ci-tax
```

2. Sửa file trong service:

```bash
mkdir -p tax
echo "demo ci $(date)" >> tax/ci-demo.txt
```

3. Commit:

```bash
git add tax/ci-demo.txt
git commit -m "test(ci): trigger tax build"
```

4. Push:

```bash
git push origin demo-ci-tax
```

5. Mở Jenkins `YAS`.
6. Nếu webhook chưa tự chạy thì bấm:

```text
Scan Multibranch Pipeline Now
```

7. Chỉ branch `demo-ci-tax` có build mới.

### Demo PR

1. Mở GitHub.
2. Bấm `Compare & pull request`.
3. Base: `main`, compare: `demo-ci-tax`.
4. Tạo PR.
5. Chỉ Jenkins check trong PR.
6. Mở Jenkins job PR tương ứng.
7. Chỉ console `Detect Changed Services`.

## A7. Coverage > 70% nằm ở đâu?

### Trả lời miệng an toàn

```text
JaCoCo tạo coverage report trong quá trình Maven verify. Jenkins truyền report đó lên SonarCloud. Việc enforce coverage threshold như 70% được cấu hình trong SonarCloud Quality Gate; Jenkins dùng waitForQualityGate để fail pipeline nếu Quality Gate fail.
```

### Mở code ở đâu

File:

```text
pom.xml
```

Plugin:

```text
jacoco-maven-plugin
```

Có goal:

```text
prepare-agent
report ở phase verify
```

File:

```text
Jenkinsfile
```

Sonar config:

```groovy
-Dsonar.coverage.jacoco.xmlReportPaths=${svc}/target/site/jacoco/jacoco.xml
```

Quality Gate:

```groovy
waitForQualityGate abortPipeline: true
```

### Nếu thầy hỏi "Jenkinsfile có hardcode 70% không?"

```text
Không hardcode trong Jenkinsfile hiện tại. Nhóm để threshold trong SonarCloud Quality Gate để dễ quản lý tập trung. Jenkins chỉ chờ kết quả Quality Gate và fail nếu SonarCloud trả về failed.
```

### Demo

1. Mở `pom.xml`, tìm `jacoco-maven-plugin`.
2. Mở Jenkinsfile, tìm `sonar.coverage.jacoco.xmlReportPaths`.
3. Mở SonarCloud project.
4. Chỉ Quality Gate condition về coverage.
5. Chỉ coverage report trong SonarCloud.

## A8. SonarQube/SonarCloud, Gitleaks, Snyk report xem ở đâu?

### Trả lời miệng

```text
SonarCloud report xem trong SonarCloud dashboard và Jenkins Quality Gate stage. Snyk report xem trong Jenkins console stage Snyk Security Scan và Snyk dashboard sau khi chạy snyk monitor. Gitleaks report xem trong GitHub Checks/Actions hoặc log scan.
```

### SonarCloud

Mở:

```text
Jenkins build -> SonarQube Analysis
Jenkins build -> Quality Gate
SonarCloud dashboard
```

Code trong `Jenkinsfile`:

```groovy
withSonarQubeEnv('SonarCloud')
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
waitForQualityGate abortPipeline: true
```

### Snyk

Code trong `Jenkinsfile`:

```groovy
snyk test --file=${svc}/pom.xml --severity-threshold=high || true
snyk monitor --file=${svc}/pom.xml || true
```

Xem:

```text
Jenkins Console Output -> Snyk Security Scan
Snyk dashboard
```

Nếu thầy hỏi vì sao có `|| true`:

```text
Trong demo nhóm để Snyk không chặn pipeline nhằm ghi nhận report trước. Production nên bỏ `|| true` hoặc cấu hình policy fail rõ ràng theo severity.
```

### Gitleaks

File:

```text
gitleaks.toml
.github/workflows/gitleaks.yml
.github/workflows/gitleaks-check.yaml
```

Xem:

```text
GitHub -> Actions -> gitleaks
Pull Request -> Checks -> gitleaks-check
```

Nếu thầy hỏi vì sao có GitHub Actions:

```text
Đây là phần Project 1/security scan cũ để chứng minh Gitleaks. Luồng triển khai Project 2 không phát triển bằng GitHub Actions; Jenkins vẫn là CI/CD chính.
```

## A9. Khi Gitleaks scan ra leaked credentials thì làm gì?

### Trả lời miệng

```text
Nếu Gitleaks phát hiện credential thật thì coi như secret đã lộ. Việc đầu tiên là revoke hoặc rotate secret. Sau đó xóa khỏi code/history, chuyển sang Jenkins Credentials/Kubernetes Secret/GitHub Secret, rồi chạy lại Gitleaks để xác nhận sạch.
```

### Quy trình chuẩn

1. Xác định leak thật hay false positive.
2. Nếu leak thật, revoke/rotate ngay:

```text
GitHub PAT
Docker Hub token
Snyk token
Cloud key
Database password
```

3. Xóa secret khỏi code.
4. Nếu đã commit, xóa khỏi history bằng:

```text
git filter-repo hoặc BFG Repo-Cleaner
```

5. Chuyển secret sang:

```text
Jenkins Credentials
Kubernetes Secret
GitHub Secret nếu là workflow cũ
```

6. Chạy lại Gitleaks.
7. Chụp report pass.

### Demo nếu thầy hỏi "secret đang để đâu?"

Jenkins:

```text
Manage Jenkins -> Credentials
```

Các credential:

```text
dockerhub-credentials
snyk-token
github-push-token nếu dùng push GitOps
kubeconfig credential cho Jenkins deploy
```

Không mở giá trị secret, chỉ mở danh sách credential ID.

## A10. SonarQube có cài cái gì không? Quality Gate là gì?

### Trả lời miệng

```text
Nhóm dùng SonarCloud tích hợp Jenkins. Jenkins cài/cấu hình SonarQube Scanner plugin để dùng `withSonarQubeEnv` và `waitForQualityGate`. Java project scan bằng Maven Sonar plugin. Quality Gate là bộ điều kiện pass/fail như coverage, bugs, vulnerabilities, code smells, duplications.
```

### Nếu thầy nghe "gateway" thì sửa lại

```text
Dạ cái đó là Quality Gate, không phải gateway. Quality Gate là cổng chất lượng của Sonar. Nếu Quality Gate fail thì Jenkins pipeline fail.
```

### Cần cài/cấu hình gì

Trong Jenkins:

```text
Manage Jenkins -> Plugins -> SonarQube Scanner for Jenkins
Manage Jenkins -> System -> SonarQube servers -> SonarCloud
Credential token SonarCloud
```

Trong SonarCloud:

```text
Project
Quality Gate
Coverage threshold
Vulnerabilities/Bugs/Code Smells
Webhook về Jenkins nếu Jenkins public được
```

Webhook nếu có:

```text
http://<jenkins-url>/sonarqube-webhook/
```

### Demo

1. Mở Jenkins `Manage Jenkins -> System`.
2. Chỉ SonarQube servers `SonarCloud`.
3. Không mở token.
4. Mở Jenkins build, chỉ `SonarQube Analysis`.
5. Chỉ `Quality Gate`.
6. Mở SonarCloud dashboard.

## B. Project 2 - Jenkins CD, Web Demo, developer_build, Cleanup

## B1. Khi thay đổi một service ở Project 2 thì Jenkins làm gì?

### Trả lời miệng

```text
Với branch developer, Jenkins detect service thay đổi, build image service đó, tag image bằng commit SHA và branch name, rồi push lên Docker Hub. Khi merge main, Jenkins còn update GitOps repo `DoubleHo05/yas-deployment` để Argo CD sync vào namespace dev. Khi release tag vX.Y.Z, Jenkins update GitOps staging.
```

### Flow branch developer

```text
Push branch dev_tax_service
-> Jenkins detect tax thay đổi
-> build/test tax
-> docker push doubleho/yas-tax:<commit-sha>
-> docker push doubleho/yas-tax:dev_tax_service
```

### Flow main

```text
Merge main
-> Jenkins build/push image
-> Jenkins update envs/dev/<service>-values.yaml
-> Argo CD sync namespace dev
```

### Flow release tag

```text
Push tag v1.0.0
-> Jenkins build/push image tag v1.0.0
-> Jenkins update envs/staging/<service>-values.yaml
-> Argo CD sync namespace staging
```

### Demo

1. Mở Jenkins build branch.
2. Chỉ `Detect Changed Services`.
3. Chỉ Docker push tag branch/commit SHA.
4. Mở Docker Hub tag.
5. Nếu build main/tag, mở GitOps repo values file.
6. Mở Argo CD app dev/staging.

## B2. Mở web lên cho thầy xem, search gì đó

### Trả lời miệng

```text
Nhóm expose ứng dụng qua ingress-nginx NodePort 30303. Vì không có DNS thật, developer thêm domain vào file hosts trỏ về external IP của worker node. Sau đó truy cập storefront bằng domain local.
```

### Hosts file

Trên máy local:

```text
35.198.213.72 storefront.yas.local.com
35.198.213.72 backoffice.yas.local.com
35.198.213.72 identity.yas.local.com
```

### URL demo

```text
http://storefront.yas.local.com:30303
```

API test:

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

### Demo từng bước

1. Mở browser.
2. Vào:

```text
http://storefront.yas.local.com:30303
```

3. Tìm/search:

```text
iPhone
```

hoặc mở trang product list.

4. Nếu cần chứng minh API:

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

5. Chỉ kết quả:

```text
iPhone 15
iPhone 15 Pro
Dell XPS
iPad Pro
```

### Nếu thầy hỏi vì sao không dùng IP trực tiếp

```text
Ingress route theo Host header. Cùng một IP worker và NodePort 30303 nhưng Host khác nhau sẽ route tới app khác nhau. Vì vậy phải dùng domain trong hosts file như storefront.yas.local.com, không nên dùng thẳng IP.
```

## B3. Khi build parameterized `developer_build` thì hoạt động ra sao?

### Trả lời miệng

```text
`developer_build` là Jenkins job riêng cho preview environment. Developer nhập namespace preview và tag/branch muốn deploy cho từng service. Service nào không nhập thì dùng tag mặc định main/latest. Jenkins dùng Helm upgrade/install để deploy tất cả service vào namespace preview, trong đó service đang test dùng tag developer nhập.
```

### Cấu hình trên Jenkins

Mở:

```text
Jenkins -> developer_build -> Configure
```

Chỉ:

```text
This project is parameterized
ENV_NAME
TAX_TAG hoặc tax-service parameter
PRODUCT_TAG
CART_TAG
...
Kubeconfig credential
Pipeline script
```

### Flow

```text
Developer nhập:
ENV_NAME=demo
tax tag=dev_tax_service hoặc commit SHA
các service khác để trống/main

Jenkins tạo namespace:
preview-demo

Jenkins deploy:
tax -> docker.io/doubleho/yas-tax:dev_tax_service
service khác -> docker.io/doubleho/yas-<service>:main
```

### Demo từng bước

1. Mở Jenkins.
2. Vào job:

```text
developer_build
```

3. Bấm:

```text
Build with Parameters
```

4. Nhập:

```text
ENV_NAME=demo
TAX_TAG=dev_tax_service hoặc commit SHA/tag có trên Docker Hub
```

5. Bấm Build.
6. Mở Console Output.
7. Chỉ:

```text
kubectl create namespace preview-demo
helm upgrade --install tax ...
--set backend.image.tag=<tag>
```

8. Kiểm tra:

```bash
kubectl get pods -n preview-demo
kubectl get deploy tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

### Nếu thầy hỏi "dependency dùng sao?"

```text
Preview job có thể dùng dependency có sẵn trong cluster hoặc tạo dependency riêng cho preview tùy cấu hình job. Trong demo hiện tại nhóm ưu tiên preview độc lập để developer test nhanh, còn dev/staging chính dùng dependency đã triển khai trong các namespace postgres, kafka, elasticsearch, redis, keycloak.
```

## B4. Có job xóa tài nguyên trên Jenkins không?

### Trả lời miệng

```text
Dạ có cleanup job để xóa preview deployment sau khi developer test xong. Job nhận ENV_NAME rồi xóa namespace preview tương ứng, ví dụ preview-demo.
```

### Cấu hình Jenkins

Mở:

```text
Jenkins -> cleanup_preview hoặc cleanup job tương ứng -> Configure
```

Chỉ:

```text
This project is parameterized
ENV_NAME
Kubeconfig credential
kubectl delete namespace preview-${ENV_NAME}
```

### Demo từng bước

1. Mở cleanup job.
2. Bấm `Build with Parameters`.
3. Nhập:

```text
ENV_NAME=demo
```

4. Chạy job.
5. Kiểm tra:

```bash
kubectl get ns preview-demo
```

Kỳ vọng:

```text
NotFound
```

hoặc namespace đang `Terminating`.

## C. Argo CD, Helm Chart, Kubernetes YAML

## C1. Mở Argo CD lên cho thầy xem

### Trả lời miệng

```text
Nhóm dùng Argo CD cho GitOps dev/staging. Jenkins không deploy trực tiếp vào dev/staging mà cập nhật GitOps repo. Argo CD theo dõi repo đó và tự động sync vào Kubernetes.
```

### Mở UI

Port-forward:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Mở:

```text
https://localhost:8080
```

Login:

```text
Username: admin
Password: lấy từ argocd secret
```

Lấy password:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d; echo
```

### Demo

1. Mở Argo UI.
2. Chỉ apps:

```text
yas-root-dev
yas-root-staging
yas-tax-dev
yas-tax-staging
```

3. Mở `yas-tax-dev`.
4. Chỉ:

```text
Synced
Healthy
namespace dev
Deployment tax
Pod tax
```

5. Mở `yas-tax-staging`.
6. Chỉ image tag release:

```text
docker.io/doubleho/yas-tax:v1.0.0
```

Hoặc kiểm tra bằng lệnh:

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

## C2. Argo CD dùng Helm chart hay Go của Argo CD?

### Trả lời miệng

```text
Argo CD bản thân là một Kubernetes controller được viết bằng Go. Nhưng để deploy YAS, nhóm dùng Helm charts. Argo CD đọc Application trong GitOps repo, lấy chart path và values file, render Helm chart rồi apply manifest vào cluster.
```

### Nếu thầy hỏi "Go template là gì?"

```text
Helm chart dùng Go template syntax trong các file template YAML. Ví dụ `{{ .Values.service.type }}` hoặc `{{ include "backend.fullname" . }}`. Argo CD không tự viết Go code để deploy app, mà gọi/render Helm chart dựa trên values.
```

### Mở ở đâu

Repo source:

```text
k8s/charts/backend/
k8s/charts/backend/templates/deployment.yaml
k8s/charts/backend/templates/service.yaml
k8s/charts/backend/templates/ingress.yaml
k8s/charts/backend/templates/serviceaccount.yaml
k8s/charts/tax/Chart.yaml
k8s/charts/tax/values.yaml
k8s/charts/ui/
```

GitOps repo:

```text
https://github.com/DoubleHo05/yas-deployment
envs/dev/*-values.yaml
envs/staging/*-values.yaml
apps/dev/applications.yaml
apps/staging/applications.yaml
```

### Demo

1. Mở `k8s/charts/tax/Chart.yaml`.
2. Chỉ dependency:

```text
dependencies:
  - name: backend
    repository: file://../backend
```

3. Mở `k8s/charts/backend/templates/deployment.yaml`.
4. Chỉ image:

```text
image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
```

5. Mở `k8s/charts/backend/templates/service.yaml`.
6. Chỉ service type và selector.
7. Mở GitOps repo `envs/dev/tax-values.yaml`.
8. Chỉ:

```text
backend.image.repository
backend.image.tag
```

## C3. Các Helm templates có tác dụng gì?

### Trả lời miệng

```text
Helm templates là các YAML mẫu dùng Go template để sinh Kubernetes manifest theo values. Cùng một chart backend có thể deploy nhiều service khác nhau như tax, product, order bằng cách thay values image, port, databaseName, config.
```

### Các template chính

```text
deployment.yaml: tạo Deployment/Pod, định nghĩa image, env, volume, probes.
service.yaml: tạo Service ổn định để các pod/service khác gọi.
ingress.yaml: tạo route HTTP theo domain/host.
serviceaccount.yaml: tạo identity cho pod.
servicemonitoring.yaml: tạo ServiceMonitor cho Prometheus nếu bật.
```

### Demo

Mở:

```text
k8s/charts/backend/templates/deployment.yaml
```

Giải thích:

```text
Deployment đảm bảo số replica, rollout image mới, tự tạo pod mới khi pod chết.
```

Mở:

```text
k8s/charts/backend/templates/service.yaml
```

Giải thích:

```text
Service tạo DNS ổn định, ví dụ http://tax hoặc http://product. Pod IP thay đổi nhưng service name không đổi.
```

## C4. Service trong Kubernetes có vai trò gì? Có những type nào?

### Trả lời miệng

```text
Service cung cấp địa chỉ ổn định và load balancing cho một nhóm pod được chọn bằng label selector. Pod IP có thể thay đổi khi rollout/restart, nhưng Service DNS và ClusterIP ổn định để service khác gọi.
```

### Mở code

File:

```text
k8s/charts/backend/templates/service.yaml
```

Đoạn chính:

```yaml
spec:
  type: {{ .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: http
  selector:
    app.kubernetes.io/name: ...
```

### Các type

```text
ClusterIP: chỉ truy cập nội bộ trong cluster. Đây là default.
NodePort: mở một port trên mỗi node để truy cập từ ngoài vào.
LoadBalancer: cloud provider cấp load balancer public.
ExternalName: ánh xạ service tới DNS name bên ngoài.
```

### Liên hệ project

```text
Các microservice YAS chủ yếu dùng ClusterIP để gọi nội bộ. Ingress-nginx được expose qua NodePort 30303 để developer truy cập từ ngoài bằng domain local.
```

## C5. Argo CD có ích gì so với gõ tay `helm template` hoặc `helm upgrade`?

### Trả lời miệng

```text
Nếu gõ tay helm/kubectl thì trạng thái cluster phụ thuộc thao tác thủ công. Argo CD theo GitOps: Git là source of truth, Argo tự so sánh desired state trong Git với live state trong cluster, báo OutOfSync, sync lại, self-heal, có lịch sử và rollback.
```

### So sánh nhanh

```text
helm template/helm upgrade tay:
- Phải chạy lệnh thủ công.
- Dễ quên values hoặc chạy sai namespace.
- Khó biết cluster drift so với Git.

Argo CD:
- Theo dõi GitOps repo tự động.
- Hiện Synced/OutOfSync/Healthy.
- Auto sync dev/staging.
- Có UI, history, rollback.
- Self-heal nếu tài nguyên bị sửa tay.
```

### Demo

1. Mở Argo UI.
2. Chỉ app đang `Synced`.
3. Bấm app xem resource tree.
4. Chỉ history/sync revision.
5. Nếu cần, mở GitOps repo commit gần nhất do Jenkins tạo.

## C6. Có trigger Jenkins khi release tag không? Deploy staging khi nào?

### Trả lời miệng

```text
Có. Jenkins Multibranch Pipeline bật Discover tags và Build Strategy cho Tags. Khi push release tag như v1.0.0, Jenkins build tag đó, push Docker image tag v1.0.0, cập nhật GitOps `envs/staging/<service>-values.yaml`, rồi Argo CD sync vào namespace staging.
```

### Demo tạo release tag

Kiểm tra main mới nhất:

```bash
git fetch origin main --tags
```

Tạo tag:

```bash
git tag -a v1.0.1 origin/main -m "release: v1.0.1 staging test"
```

Push tag:

```bash
git push origin v1.0.1
```

Mở Jenkins:

```text
YAS -> tag v1.0.1 build
```

Chỉ console:

```text
Docker push docker.io/doubleho/yas-tax:v1.0.1
Update envs/staging/tax-values.yaml -> tag v1.0.1
```

Kiểm tra staging:

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
docker.io/doubleho/yas-tax:v1.0.1
```

### Nếu tag không tự build

Kiểm tra Jenkins:

```text
YAS -> Configure -> Build Strategies -> Tags
```

Ô:

```text
Ignore tags older than
```

không để `0`.

## D. Service Mesh

## D1. Service Mesh nhóm làm gì?

### Trả lời miệng

```text
Nhóm dùng Istio để chứng minh service mesh trong namespace mesh-demo. Nhóm bật mTLS STRICT, cấu hình AuthorizationPolicy default-deny và allow order gọi tax, cấu hình VirtualService retry, rồi dùng curl pod và Kiali topology để chứng minh allow/deny/retry.
```

### File ở đâu

```text
outputs/service-mesh/01-mesh-demo-namespace.yaml
outputs/service-mesh/02-mtls-strict.yaml
outputs/service-mesh/03-tax-retry-virtualservice.yaml
outputs/service-mesh/04-authz-default-deny.yaml
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
outputs/service-mesh/06-faulty-service.yaml nếu PR đã merge
outputs/service-mesh/allowed-client.yaml nếu PR đã merge
outputs/service-mesh/blocked-client.yaml nếu PR đã merge
outputs/service-mesh/README.md
```

### Vì sao dùng `mesh-demo`, không dùng dev/staging?

```text
Dev/staging là môi trường CD chính đang demo Jenkins -> Argo CD. Bật service mesh trực tiếp ở đó có rủi ro làm gián đoạn demo. Vì vậy nhóm tạo namespace mesh-demo độc lập để chứng minh đủ mTLS, AuthorizationPolicy, retry và Kiali topology mà không phá luồng chính.
```

## D2. mTLS là gì? Demo ở đâu?

### Trả lời miệng

```text
mTLS là mutual TLS giữa các service trong mesh. Cả client và server đều xác thực lẫn nhau bằng certificate do Istio cấp. Nhóm bật STRICT mTLS trong namespace mesh-demo bằng PeerAuthentication.
```

### Mở file

```text
outputs/service-mesh/02-mtls-strict.yaml
```

Nội dung chính:

```yaml
kind: PeerAuthentication
spec:
  mtls:
    mode: STRICT
```

### Demo

```bash
kubectl get peerauthentication -n mesh-demo -o yaml
kubectl get pods -n mesh-demo
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

Kỳ vọng pod có:

```text
istio-proxy
```

## D3. AuthorizationPolicy allow/deny hoạt động sao?

### Trả lời miệng

```text
Nhóm tạo default-deny để mặc định chặn traffic, sau đó tạo allow policy chỉ cho service account order gọi service tax. Client dùng service account khác sẽ bị chặn 403/RBAC denied.
```

### Mở file

```text
outputs/service-mesh/04-authz-default-deny.yaml
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
```

Nội dung allow:

```yaml
principals:
  - cluster.local/ns/mesh-demo/sa/order
```

### Demo

Allow:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- curl -i http://tax:8090/actuator/health || true
```

Deny:

```bash
kubectl exec -n mesh-demo curl-deny -c curl-deny -- curl -i http://tax:8090/actuator/health || true
```

Kỳ vọng:

```text
curl-order được phép hoặc tới được service.
curl-deny bị 403/RBAC access denied.
```

## D4. Retry policy nằm ở đâu?

### Trả lời miệng

```text
Retry policy được cấu hình bằng Istio VirtualService. Khi request gặp lỗi 5xx, connect-failure hoặc refused-stream, Envoy sidecar tự retry theo số lần cấu hình mà client không cần tự viết retry code.
```

### Mở file

```text
outputs/service-mesh/03-tax-retry-virtualservice.yaml
```

Nội dung chính:

```yaml
retries:
  attempts: 3
  perTryTimeout: 2s
  retryOn: 5xx,connect-failure,refused-stream
```

### Demo

1. Mở Kiali topology.
2. Tạo traffic tới service có lỗi 500 hoặc faulty service.
3. Chỉ VirtualService retry.
4. Chỉ log/curl evidence retry nếu có.

## D5. Kiali topology giải thích sao?

### Trả lời miệng

```text
Kiali đọc telemetry từ Istio để vẽ topology service-to-service. Trong topology mesh-demo, mình thấy client/order gọi tax, thấy traffic được allow/deny và có thể thấy lỗi/retry tùy kịch bản.
```

### Mở Kiali

```bash
istioctl dashboard kiali
```

hoặc:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

Chọn namespace:

```text
mesh-demo
```

## E. Observability

## E1. Observability deploy được không? Mở dashboard cho thầy coi

### Trả lời miệng

```text
Dạ có. Nhóm deploy observability stack trong namespace observability gồm Prometheus, Grafana, Loki, Promtail, Tempo và OpenTelemetry Collector. Grafana là UI chính để xem metrics, logs và traces.
```

### Kiểm tra pod

```bash
kubectl get pods -n observability
```

Nếu Grafana lỗi vì Postgres:

```text
Postgres là DB backend của Grafana. Nếu Postgres OOMKilled thì Grafana crash. Nhóm đã tăng resource Postgres để ổn định lại.
```

### Mở Grafana

```bash
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Mở:

```text
http://localhost:3000
```

Login:

```text
admin / password trong secret hoặc password demo
```

Lấy password:

```bash
kubectl --namespace observability get secrets prometheus-grafana -o jsonpath="{.data.admin-password}" | base64 -d; echo
```

## E2. Giải thích Prometheus, Loki, Tempo

### Trả lời miệng

```text
Prometheus thu metrics. Loki lưu logs. Tempo lưu traces. Promtail đọc log pod và gửi về Loki. OpenTelemetry Collector nhận trace OTLP và gửi sang Tempo. Grafana kết nối cả ba datasource để quan sát tập trung.
```

### Flow

```text
Metrics: Kubernetes/node/pod -> Prometheus -> Grafana
Logs: Pod logs -> Promtail -> Loki -> Grafana
Traces: telemetrygen/app -> OpenTelemetry Collector -> Tempo -> Grafana
```

## E3. Demo query data trong Grafana

### Prometheus

1. Grafana -> Explore.
2. Chọn datasource:

```text
Prometheus
```

3. Query:

```text
up
```

4. Chỉ các target có value `1`.

### Loki

1. Grafana -> Explore.
2. Chọn datasource:

```text
Loki
```

3. Query:

```text
{namespace="staging"}
```

4. Chỉ log workload.

### Tempo

1. Chạy telemetrygen nếu cần:

```bash
kubectl delete pod telemetrygen -n observability --ignore-not-found
kubectl run telemetrygen -n observability --rm -i --restart=Never \
  --image=ghcr.io/open-telemetry/opentelemetry-collector-contrib/telemetrygen:latest \
  --command -- /telemetrygen traces \
  --otlp-endpoint opentelemetry-collector:4317 \
  --otlp-insecure \
  --traces 20
```

2. Grafana -> Explore.
3. Chọn datasource:

```text
Tempo
```

4. Query:

```text
{resource.service.name="telemetrygen"}
```

5. Chỉ trace:

```text
telemetrygen / lets-go
```

## E4. Tạo dashboard mới trên Grafana và lọc data

### Trả lời miệng

```text
Grafana có thể tạo dashboard custom từ datasource. Nhóm demo tạo panel Prometheus query `up`, lọc theo namespace/job, rồi save dashboard.
```

### Demo từng bước

1. Mở Grafana.
2. Vào:

```text
Dashboards -> New -> New dashboard
```

3. Bấm:

```text
Add visualization
```

4. Chọn datasource:

```text
Prometheus
```

5. Query đơn giản:

```text
up
```

6. Nếu muốn lọc theo job:

```text
up{job="kubelet"}
```

hoặc:

```text
up{namespace="observability"}
```

7. Chọn visualization:

```text
Time series hoặc Table
```

8. Đặt title:

```text
Kubernetes Targets Up
```

9. Bấm:

```text
Apply
```

10. Bấm Save dashboard:

```text
YAS Demo Observability
```

### Nếu thầy muốn lọc log

Trong Explore Loki:

```text
{namespace="staging"}
```

Lọc thêm container:

```text
{namespace="staging", container="product"}
```

Nếu không có container product thì chọn label có thật trong giao diện.

## F. Câu hỏi YAML/code hay bị hỏi

## F1. Thầy hỏi "mấy cái YAML này làm gì?"

### Trả lời chung

```text
YAML là manifest Kubernetes hoặc template Helm. Kubernetes dùng YAML để mô tả desired state. Helm template giúp tái sử dụng YAML cho nhiều service bằng values.
```

### Các file nên mở

```text
k8s/charts/backend/templates/deployment.yaml
k8s/charts/backend/templates/service.yaml
k8s/charts/backend/templates/ingress.yaml
k8s/deploy/postgres/postgresql/templates/postgresql.yaml
outputs/service-mesh/*.yaml
```

### Nói theo từng file

```text
deployment.yaml: chạy pod, image, env, probes.
service.yaml: tạo DNS/load balancing nội bộ.
ingress.yaml: route HTTP theo domain.
postgresql.yaml: tạo Postgres cluster qua Zalando Postgres Operator.
PeerAuthentication.yaml: bật mTLS.
AuthorizationPolicy.yaml: allow/deny traffic.
VirtualService.yaml: retry/routing policy.
```

## F2. Nếu thầy bảo sửa header thành `yas-<tên>` rồi push xem deploy không

### Cách làm an toàn

Chọn frontend nếu muốn dễ nhìn, ví dụ `storefront`.

1. Tạo branch:

```bash
git switch -c demo-header-yas-name
```

2. Sửa text header trong `storefront` hoặc service UI tương ứng.
3. Commit:

```bash
git add storefront
git commit -m "demo: update storefront header"
```

4. Push:

```bash
git push origin demo-header-yas-name
```

5. Mở Jenkins branch build.
6. Chỉ Jenkins detect `storefront`.
7. Sau khi image có tag branch/commit, dùng `developer_build` deploy preview với tag đó.
8. Mở:

```text
http://storefront.yas.local.com:30303
```

hoặc preview domain/NodePort nếu job tạo riêng.

### Nếu demo main/dev

Merge PR vào main:

```text
Jenkins update GitOps dev -> Argo CD sync namespace dev -> storefront hiển thị header mới.
```

## G. Checklist đứng trước thầy

### Project 1 mở sẵn

- [ ] GitHub branch protection/ruleset main.
- [ ] Một PR có Jenkins check.
- [ ] Jenkins `YAS` Multibranch Configure.
- [ ] Jenkins build console có Detect Changed Services.
- [ ] Jenkinsfile mở đúng stage detect/push/Sonar/Snyk.
- [ ] SonarCloud dashboard.
- [ ] Snyk/Gitleaks report.

### Project 2 mở sẵn

- [ ] Docker Hub tag của service.
- [ ] GitOps repo `DoubleHo05/yas-deployment`.
- [ ] Argo CD UI dev/staging.
- [ ] K3s pods dev/staging.
- [ ] Web storefront `http://storefront.yas.local.com:30303`.
- [ ] Jenkins `developer_build`.
- [ ] Jenkins cleanup job.

### Nâng cao mở sẵn

- [ ] Kiali topology `mesh-demo`.
- [ ] Service mesh YAML files.
- [ ] Grafana dashboard/Explore.
- [ ] Prometheus query `up`.
- [ ] Loki query `{namespace="staging"}`.
- [ ] Tempo query `{resource.service.name="telemetrygen"}`.

## H. Câu trả lời siêu ngắn học thuộc

### Jenkins-first

```text
Jenkins là CI/CD chính. GitHub chỉ quản lý repo/PR, Argo CD chỉ sync GitOps, Docker Hub chỉ lưu image.
```

### Monorepo

```text
Pipeline dùng git diff để tìm file thay đổi, map path sang service, rồi chỉ build/test/push service đó.
```

### Docker tag

```text
Branch build push tag commit SHA và branch name. Main build thêm tag main. Release tag build thêm tag vX.Y.Z.
```

### Argo CD

```text
Jenkins update GitOps repo, Argo CD tự sync dev/staging. Git là source of truth.
```

### Helm

```text
YAS deploy bằng Helm chart. Helm templates là YAML dùng Go template, values quyết định image/tag/config từng service.
```

### Service

```text
Kubernetes Service tạo DNS/load balancing ổn định cho pods. Types gồm ClusterIP, NodePort, LoadBalancer, ExternalName.
```

### Service Mesh

```text
Istio mesh-demo chứng minh mTLS, AuthorizationPolicy allow/deny, retry VirtualService và Kiali topology.
```

### Observability

```text
Prometheus metrics, Loki logs, Tempo traces, Grafana là UI. Promtail gửi logs về Loki, OpenTelemetry Collector gửi traces về Tempo.
```

