pipeline {
    agent any

    parameters {
        string(name: 'ENV_NAME', defaultValue: 'demo', description: 'Preview environment name')
        string(name: 'PRODUCT_BRANCH', defaultValue: 'main')
        string(name: 'CART_BRANCH', defaultValue: 'main')
        string(name: 'ORDER_BRANCH', defaultValue: 'main')
        string(name: 'CUSTOMER_BRANCH', defaultValue: 'main')
        string(name: 'INVENTORY_BRANCH', defaultValue: 'main')
        string(name: 'TAX_BRANCH', defaultValue: 'main')
        string(name: 'MEDIA_BRANCH', defaultValue: 'main')
        string(name: 'SEARCH_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_BFF_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_UI_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_BFF_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_UI_BRANCH', defaultValue: 'main')
        string(name: 'SAMPLEDATA_BRANCH', defaultValue: 'main')
        string(name: 'SWAGGER_UI_TAG', defaultValue: 'v4.16.0')
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

        stage('Deploy Preview') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-yas-k3s', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        NAMESPACE="preview-${ENV_NAME}"

                        resolve_tag() {
                          branch="$1"
                          if [ "$branch" = "main" ]; then
                            echo "main"
                          else
                            git fetch origin "$branch"
                            git rev-parse --short=12 "origin/$branch"
                          fi
                        }

                        deploy_backend() {
                          release="$1"
                          chart="$2"
                          image="$3"
                          branch="$4"
                          tag="$(resolve_tag "$branch")"

                          echo "Deploying ${release}: docker.io/${DOCKERHUB_USER}/${image}:${tag}"
                          helm dependency build "$chart" || true
                          helm upgrade --install "$release" "$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set backend.image.repository="docker.io/${DOCKERHUB_USER}/${image}" \
                            --set backend.image.tag="$tag" \
                            --set backend.serviceMonitor.enabled=false
                        }

                        deploy_ui() {
                          release="$1"
                          chart="$2"
                          image="$3"
                          branch="$4"
                          tag="$(resolve_tag "$branch")"

                          echo "Deploying ${release}: docker.io/${DOCKERHUB_USER}/${image}:${tag}"
                          helm dependency build "$chart" || true
                          helm upgrade --install "$release" "$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set ui.image.repository="docker.io/${DOCKERHUB_USER}/${image}" \
                            --set ui.image.tag="$tag"
                        }

                        deploy_swagger_ui() {
                          release="$1"
                          chart="$2"
                          tag="$3"

                          echo "Deploying ${release}: swaggerapi/swagger-ui:${tag}"
                          helm dependency build "$chart" || true
                          helm upgrade --install "$release" "$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set image.repository="swaggerapi/swagger-ui" \
                            --set image.tag="$tag" \
                            --set ingress.enabled=false
                        }

                        echo "Namespace: $NAMESPACE"
                        kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

                        kubectl create namespace postgres --dry-run=client -o yaml | kubectl apply -f -

                        cat <<'EOF' | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
  namespace: postgres
type: Opaque
stringData:
  POSTGRES_USER: yasadminuser
  POSTGRES_PASSWORD: admin
  POSTGRES_DB: tax
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql
  namespace: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:15
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: postgresql-secret
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: postgres
spec:
  selector:
    app: postgresql
  ports:
    - name: postgres
      port: 5432
      targetPort: 5432
EOF

                        kubectl rollout status deployment/postgresql -n postgres --timeout=180s || true

                        kubectl exec -n postgres deployment/postgresql -- sh -c '
                          set -eu
                          for db in product cart order customer inventory tax media search sampledata; do
                            if psql -U "$POSTGRES_USER" -d postgres -lqt | cut -d "|" -f 1 | grep -qw "$db"; then
                              echo "Database ${db} already exists"
                            else
                              echo "Creating database ${db}"
                              createdb -U "$POSTGRES_USER" "$db"
                            fi
                          done
                        '

                        kubectl delete secret yas-postgresql-credentials-secret -n "$NAMESPACE" --ignore-not-found=true
                        helm dependency build k8s/charts/yas-configuration || true
                        helm upgrade --install yas-configuration k8s/charts/yas-configuration \
                          -n "$NAMESPACE" --create-namespace

                        deploy_backend product k8s/charts/product yas-product "$PRODUCT_BRANCH"
                        deploy_backend cart k8s/charts/cart yas-cart "$CART_BRANCH"
                        deploy_backend order k8s/charts/order yas-order "$ORDER_BRANCH"
                        deploy_backend customer k8s/charts/customer yas-customer "$CUSTOMER_BRANCH"
                        deploy_backend inventory k8s/charts/inventory yas-inventory "$INVENTORY_BRANCH"
                        deploy_backend tax k8s/charts/tax yas-tax "$TAX_BRANCH"
                        deploy_backend media k8s/charts/media yas-media "$MEDIA_BRANCH"
                        deploy_backend search k8s/charts/search yas-search "$SEARCH_BRANCH"
                        deploy_backend storefront-bff k8s/charts/storefront-bff yas-storefront-bff "$STOREFRONT_BFF_BRANCH"
                        deploy_ui storefront-ui k8s/charts/storefront-ui yas-storefront "$STOREFRONT_UI_BRANCH"
                        deploy_backend backoffice-bff k8s/charts/backoffice-bff yas-backoffice-bff "$BACKOFFICE_BFF_BRANCH"
                        deploy_ui backoffice-ui k8s/charts/backoffice-ui yas-backoffice "$BACKOFFICE_UI_BRANCH"
                        deploy_swagger_ui swagger-ui k8s/charts/swagger-ui "$SWAGGER_UI_TAG"
                        deploy_backend sampledata k8s/charts/sampledata yas-sampledata "$SAMPLEDATA_BRANCH"

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
      targetPort: http
      nodePort: 30080
EOF

                        echo "===== Preview resources ====="
                        kubectl get all -n "$NAMESPACE"
                        kubectl get svc -n "$NAMESPACE"
                        kubectl get endpoints yas-preview-nodeport -n "$NAMESPACE" || true

                        echo "===== Deployment images ====="
                        for deploy in product cart order customer inventory tax media search storefront-bff storefront-ui backoffice-bff backoffice-ui swagger-ui sampledata; do
                          kubectl get deployment "$deploy" -n "$NAMESPACE" \
                            -o jsonpath="${deploy}={.spec.template.spec.containers[0].image}{'\\n'}" || true
                        done
                    '''
                }
            }
        }
    }
}
