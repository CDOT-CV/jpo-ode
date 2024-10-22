package us.dot.its.jpo.ode.services.asn1.message;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.ODEKafkaProperties;
import us.dot.its.jpo.ode.OdeProperties;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = ODEKafkaProperties.class)
public class AsnCodecMessageServiceControllerTest {

	@Autowired
	ODEKafkaProperties odeKafkaProps;

	@Test
	public void shouldStartTwoConsumers() {
		OdeProperties odeProps = new OdeProperties();

		AsnCodecMessageServiceController asnCodecMessageServiceController = new AsnCodecMessageServiceController(odeProps, odeKafkaProps);
		assertNotNull(asnCodecMessageServiceController);
	}

}
