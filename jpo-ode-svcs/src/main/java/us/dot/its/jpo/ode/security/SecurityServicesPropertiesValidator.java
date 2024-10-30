package us.dot.its.jpo.ode.security;

import org.springframework.validation.Validator;

import java.net.URI;
import java.net.URISyntaxException;

public class SecurityServicesPropertiesValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return SecurityServicesProperties.class.equals(clazz);
    }

    @Override
    public void validate(Object target, org.springframework.validation.Errors errors) {
        SecurityServicesProperties properties = (SecurityServicesProperties) target;

        if (!properties.isRsuEnabled() && !properties.isSdwEnabled()) {
            // if neither RSU nor SDW are enabled, then no further validation is needed because no security services are enabled
            return;
        }

        if (properties.getSignatureEndpoint() != null && !properties.getSignatureEndpoint().isEmpty()) {
            try {
                URI uri = new URI(properties.getSignatureEndpoint());
                if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
                    errors.rejectValue("signatureEndpoint", "signatureEndpoint.invalid", "Signature endpoint must be an http URL");
                }
            } catch (URISyntaxException e) {
                errors.rejectValue("signatureEndpoint", "signatureEndpoint.invalid", "Signature endpoint must be a valid URL");
            }
        } else {
            if (properties.getHostIP() == null || properties.getHostIP().isEmpty()) {
                errors.rejectValue("hostIP", "hostIP.invalid", "Host IP must be provided");
            }
            if (properties.getPort() <= 0) {
                errors.rejectValue("port", "port.invalid", "Port must be greater than 0");
            }
        }
    }
}
