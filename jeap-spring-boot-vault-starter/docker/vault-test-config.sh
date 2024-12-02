#!/bin/sh

# Make sure vault is started
while ! nc -z vault-server 8200 ; do sleep 1 ; done

set -exo pipefail

unset http_proxy
unset HTTP_PROXY
unset http_proxy
unset https_proxy

export VAULT_ADDR=http://vault-server:8200

vault login secret

vault audit enable file file_path=stdout

# Put a test secret into the secrets engine at /secret
vault kv put secret/jeap/jeap-spring-boot-vault-starter test.testSecret=vault-secret-value

# Enable approle auth method
vault auth enable -path=approle/jeap approle

# Create policy 'jeap-spring-boot-vault-starter' for the approle with path restriction
SCRIPT_DIR=`dirname $0`
vault policy write jeap-spring-boot-vault-starter-policy ${SCRIPT_DIR}/jeap-spring-boot-vault-starter-pol.hcl

# Create approle for jeap-spring-boot-vault-starter, assign the jeap-spring-boot-vault-starter-policy policy
APPROLE_PATH=auth/approle/jeap/role/jeap-spring-boot-vault-starter
vault write ${APPROLE_PATH} \
   bind_secret_id=true \
   token_policies=jeap-spring-boot-vault-starter-policy

# Log approle
vault read ${APPROLE_PATH}

# Set fixed role-id for local tests
ROLE_ID=9999-8888-7777
vault write ${APPROLE_PATH}/role-id \
  role_name=jeap-spring-boot-vault-starter \
  role_id="${ROLE_ID}"

# Set fixed secret-id for local tests
SECRET_ID=1234-5678-9012-3456
vault write ${APPROLE_PATH}/custom-secret-id \
  role_name=jeap-spring-boot-vault-starter \
  secret_id="${SECRET_ID}"
