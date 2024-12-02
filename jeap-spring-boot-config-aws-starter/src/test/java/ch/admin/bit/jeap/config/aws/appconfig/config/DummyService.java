package ch.admin.bit.jeap.config.aws.appconfig.config;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DummyService {

    private final ConfigProperties configProperties;
    private final RefreshScopedBean refreshScopedBean;
    private final StandardScopedBean standardScopedBean;

    String getConfiPropertiesProperty() {
        return configProperties.getProperty();
    }

    String getRefreshScopedBeanProperty() {
        return refreshScopedBean.getProperty();
    }

    String getStandardScopedBeanProperty() {
        return standardScopedBean.getProperty();
    }

}
