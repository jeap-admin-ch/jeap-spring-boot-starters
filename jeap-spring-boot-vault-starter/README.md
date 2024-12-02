# jEAP Spring Boot Vault Starter

Provides integration with Hashicorp Vault for secrets management in Spring Boot Apps.

## Development

To test the starter, a Spring Boot App named ```VaultTestApp``` is provided. It injects a secret from a local vault
instance if started with the Spring profile 'vault' and fails to start if the secret cannot be injected.

The test app requires a running vault instance in a local docker container, which can be started using

```shell
cd docker && docker-compose up
```

The docker compose file will also run a vault client to provision a test secret,
see [vault-client.sh](docker/vault-client.sh).
