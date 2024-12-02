package ch.admin.bit.jeap.config.aws.appconfig.client;

import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for JeapAppConfigDataClient instances that reuses instances per app config profile because the polling for
 * configuration changes in a profile is based on a session that identifies the client to aws app config. We don't want
 * to have more than one session open for the same profile as this might interfere with the aws app config deployment
 * strategies.
 */
public class JeapAppConfigDataClientFactory {

    private static final Map<String, JeapAppConfigDataClient> clients = new HashMap<>();

    synchronized public static JeapAppConfigDataClient create(String appId, String envId, String profileId,
                                                              Integer requiredMinimumPollIntervalInSeconds,
                                                              AppConfigDataClient appConfigDataClient) {
        final String clientId = String.join("-", profileId, envId, appId);
        return clients.computeIfAbsent(clientId, k ->
                new JeapAppConfigDataClient(appConfigDataClient, appId, envId, profileId, requiredMinimumPollIntervalInSeconds));
    }

    synchronized public static void evictAllCachedClients() {
        clients.values().forEach(JeapAppConfigDataClient::disablePolling);
        clients.clear();
    }

}
