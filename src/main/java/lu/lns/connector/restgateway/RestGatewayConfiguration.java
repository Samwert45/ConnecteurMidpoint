package lu.lns.connector.restgateway;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class RestGatewayConfiguration extends AbstractConfiguration {

    private String gatewayUrl = "http://localhost:5000";
    private Integer connectionTimeout = 30000;
    private Integer requestTimeout = 60000;
    private Boolean validateSsl = true;

    @Override
    public void validate() {
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Gateway URL cannot be empty");
        }

        if (!gatewayUrl.startsWith("http://") && !gatewayUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Gateway URL must start with http:// or https://");
        }

        if (connectionTimeout != null && connectionTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }

        if (requestTimeout != null && requestTimeout <= 0) {
            throw new IllegalArgumentException("Request timeout must be positive");
        }
    }

    @ConfigurationProperty(
        order = 1,
        displayMessageKey = "gatewayUrl.display",
        helpMessageKey = "gatewayUrl.help",
        required = true
    )
    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    @ConfigurationProperty(
        order = 2,
        displayMessageKey = "connectionTimeout.display",
        helpMessageKey = "connectionTimeout.help"
    )
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @ConfigurationProperty(
        order = 3,
        displayMessageKey = "requestTimeout.display",
        helpMessageKey = "requestTimeout.help"
    )
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @ConfigurationProperty(
        order = 4,
        displayMessageKey = "validateSsl.display",
        helpMessageKey = "validateSsl.help"
    )
    public Boolean getValidateSsl() {
        return validateSsl;
    }

    public void setValidateSsl(Boolean validateSsl) {
        this.validateSsl = validateSsl;
    }
}
