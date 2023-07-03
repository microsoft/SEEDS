az login
az acr login -n seedsregistry
docker build -f Dockerfile -t seedsregistry.azurecr.io/fsm-executor-seeds:latest .
docker push seedsregistry.azurecr.io/fsm-executor-seeds:latest
PAUSE