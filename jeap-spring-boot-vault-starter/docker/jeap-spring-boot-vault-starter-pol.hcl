# See https://www.vaultproject.io/docs/concepts/policies for details

# Read-only permission on secrets for jeap-spring-boot-vault-starter
path "secret/data/jeap/jeap-spring-boot-vault-starter" {
  capabilities = [
    "read"
  ]
}

# Read-only permission on profile specific secrets for jeap-spring-boot-vault-starter
path "secret/data/jeap/jeap-spring-boot-vault-starter/*" {
  capabilities = [
    "read"
  ]
}

# Read-only permission on shared secrets of the system jeap
path "secret/data/jeap/shared" {
  capabilities = [
    "read"
  ]
}

# Read-only permission on profile specific shared secrets of the system jeap
path "secret/data/jeap/shared/*" {
  capabilities = [
    "read"
  ]
}
