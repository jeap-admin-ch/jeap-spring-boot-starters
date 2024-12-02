package ch.admin.bit.jeap.tls;

import lombok.SneakyThrows;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.time.Duration;
import java.util.Date;

class PemKeyCertPairFactory {

    @SneakyThrows
    static PemKeyCertPair createPemKeyCertPair(String host, Duration validity) {

        KeyPair keyPair = generateKeyPair();

        X500Principal subject = new X500Principal("CN=" + host);
        X500Principal signedByPrincipal = subject; // self-signing
        KeyPair signedByKeyPair = keyPair; // self-signing

        long notBefore = System.currentTimeMillis();
        long notAfter = notBefore + validity.toMillis();

        ASN1Encodable[] encodableAltNames = new ASN1Encodable[]{new GeneralName(GeneralName.dNSName, host)};
        KeyPurposeId[] purposes = new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth};

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(signedByPrincipal,
                BigInteger.ONE, new Date(notBefore), new Date(notAfter), subject, keyPair.getPublic());

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment));
        certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purposes));
        certBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(encodableAltNames));

        final ContentSigner signer = new JcaContentSignerBuilder(("SHA256withRSA")).build(signedByKeyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);

        String privateKeyPem = getPrivateKeyPem(keyPair);
        String certPem = getCertificatePem(certHolder);

        return new PemKeyCertPair(privateKeyPem, certPem);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (GeneralSecurityException gse) {
            throw new IllegalStateException("Unable to create RSA key pair. Maybe there is a problem with the cryptography provider?", gse);
        }
    }

    private static String getPrivateKeyPem(KeyPair keyPair) {
        return createPem(keyPair.getPrivate());
    }

    private static String getCertificatePem(X509CertificateHolder certHolder) {
        return createPem(certHolder);
    }

    private static String createPem(Object pemmable) {
        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(pemmable);
            pemWriter.close();
            return writer.toString();}
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to encode the given object in PEM format.", e);
        }
    }

}
