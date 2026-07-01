# Object storage starter

`jeap-spring-boot-object-storage-starter` auto-configures an AWS SDK v2 `S3Client` bean for
S3-compatible object storage. Object storage keeps data as unstructured *objects* in a flat,
virtually unlimited repository — ideal for cloud-native applications that need to store files,
backups or archives at scale. The starter targets Amazon S3 in production and works equally with
S3-compatible providers such as LocalStack / MinIO for local development, so the same application
code runs everywhere. The configuration prefix is `jeap.s3.client`.

## How it works

The auto-configuration `S3ClientConfiguration` is active when `jeap.s3.client.enabled` is `true`
(the default) and builds the `S3Client` from `S3ClientProperties`. Notable wiring:

- **HTTP client** — the JDK-based `UrlConnectionHttpClient` is used. Its proxy configuration is
  pinned to ignore system properties and environment variables, as a workaround for an AWS SDK proxy
  issue (aws-sdk-java-v2 #4728).
- **Path-style access** — `pathStyleAccessEnabled(true)` is set on the service configuration, which
  is what S3-compatible providers (LocalStack, MinIO) require.
- **Endpoint override** — when `endpoint-url` is set, it is applied as an endpoint override. The URL
  may include the protocol; if it does not, `tls` decides between `https://` and `http://`.
- **Credentials** — if both `access-key` and `secret-key` are set, static credentials are used;
  otherwise the bean falls back to the AWS `DefaultCredentialsProvider` (a separate
  `AwsCredentialsProvider` bean, overridable via `@ConditionalOnMissingBean`). On AWS, leave the keys
  unset so authentication happens through the IAM role of the workload.
- **Signer workaround** — on the Kubernetes (RHOS / OpenShift) cloud platform the client overrides
  the signer with `AwsS3V4Signer` to keep request signing working after the AWS SDK update.

The `S3Client` bean is annotated `@ConditionalOnMissingBean`, so an application can supply its own
client (e.g. an async or custom-tuned variant) and the starter steps aside.

## Configuration

```yaml
jeap:
  s3:
    client:
      region: us-east-1
      endpoint-url: localhost:4566   # LocalStack; omit on AWS
      access-key: ${ACCESS_KEY}      # omit on AWS (use IAM)
      secret-key: ${SECRET_KEY}      # omit on AWS (use IAM)
      tls: false                     # http for local docker; true (default) on AWS
```

| Property                      | Default      | Description                                                                     |
|-------------------------------|--------------|---------------------------------------------------------------------------------|
| `jeap.s3.client.enabled`      | `true`       | Create the `S3Client` bean                                                      |
| `jeap.s3.client.region`       | `AWS_GLOBAL` | AWS region; must be a value of the AWS SDK `Region` enum                        |
| `jeap.s3.client.endpoint-url` | —            | Base URL (host[:port], protocol optional) of the S3 provider. Do not set on AWS |
| `jeap.s3.client.access-key`   | —            | Static access key; usually omitted on AWS (IAM is used instead)                 |
| `jeap.s3.client.secret-key`   | —            | Static secret key; usually omitted on AWS                                       |
| `jeap.s3.client.tls`          | `true`       | Use HTTPS; set `false` only for local LocalStack/Docker over HTTP               |

On the various platforms these properties may be set automatically, or the SDK may already be
preconfigured — check the platform-specific documentation before setting them by hand. The
`access-key` and `secret-key` are excluded from the bean's `toString()` for security.

## Using the client

Inject the `S3Client` bean and use the AWS SDK for Java v2 directly:

```java
@Component
@RequiredArgsConstructor
public class S3BucketService {

    private final S3Client s3Client;

    public boolean doesBucketExist(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}
```

## Common patterns and pitfalls

- **AWS vs. local** — on AWS, set only `region` (and rely on IAM); locally, set `endpoint-url`,
  `tls: false` and static keys for LocalStack/MinIO. The same code path serves both.
- **Region required** — the default `AWS_GLOBAL` is rarely what you want against a regional bucket;
  set an explicit region that is a valid AWS SDK `Region` value.
- **Path-style only** — the starter always uses path-style access, which is necessary for
  S3-compatible providers; virtual-hosted-style is not configured.
- **Proxy** — system/environment proxy values are intentionally ignored by the HTTP client; route
  through a proxy by supplying your own `S3Client` bean if needed.

## Related

- [jeap-spring-boot-vault-starter](jeap-spring-boot-vault-starter.md)
- [jeap-spring-boot-postgresql-aws-starter](jeap-spring-boot-postgresql-aws-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
