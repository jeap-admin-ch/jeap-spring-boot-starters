package ch.admin.bit.jeap.tls;

/**
 * Holder for a private key and its certificate, both encoded in PEM format.
 *
 * @param key
 * @param cert
 */
record PemKeyCertPair (String key, String cert) {
}