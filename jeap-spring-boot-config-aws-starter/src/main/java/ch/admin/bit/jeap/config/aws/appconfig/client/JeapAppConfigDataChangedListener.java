package ch.admin.bit.jeap.config.aws.appconfig.client;

public interface JeapAppConfigDataChangedListener {

    /**
     * Notification that the configuration data of the given aws app config profile changed.
     *
     * @param profileId The aws app config profile with the data change
     */
    void appConfigDataChanged(String profileId);

}
