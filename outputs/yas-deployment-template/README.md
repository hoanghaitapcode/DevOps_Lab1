# YAS Deployment GitOps Repo

This repository is the deployment source of truth for Argo CD.

## Flow

```text
DevOps_Lab1 source code changes
Jenkins builds and pushes Docker image
Jenkins updates envs/dev or envs/staging image tag in this repository
Argo CD detects this repository change
Argo CD syncs Kubernetes dev or staging namespace
```

## Layout

```text
argocd/
  root-dev.yaml
  root-staging.yaml
apps/
  dev/applications.yaml
  staging/applications.yaml
envs/
  dev/*-values.yaml
  staging/*-values.yaml
```

The Argo CD Applications use multi-source configuration:

- Helm charts are loaded from `https://github.com/hoanghaitapcode/DevOps_Lab1.git`.
- Environment values are loaded from this GitOps repository.

## Bootstrap

Push this repository to GitHub as:

```text
https://github.com/DoubleHo05/yas-deployment.git
```

Then apply the root apps once:

```bash
kubectl apply -f argocd/root-dev.yaml
kubectl apply -f argocd/root-staging.yaml
```

Check:

```bash
kubectl get applications -n argocd
kubectl get deploy -n dev
kubectl get deploy -n staging
```

## Jenkins Integration

The application repository Jenkinsfile expects a Jenkins credential:

```text
ID: github-push-token
Kind: Username with password
Username: GitHub username
Password: GitHub PAT with write permission to yas-deployment
```

On `main`, Jenkins updates `envs/dev/*-values.yaml` to the commit SHA.

On release tags such as `v1.0.0`, Jenkins updates
`envs/staging/*-values.yaml` to the release tag.
