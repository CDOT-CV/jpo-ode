/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.services.asn1;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.OdeBsmDataCreatorHelper;
import us.dot.its.jpo.ode.coder.OdeMapDataCreatorHelper;
import us.dot.its.jpo.ode.coder.OdeSpatDataCreatorHelper;
import us.dot.its.jpo.ode.coder.OdeSsmDataCreatorHelper;
import us.dot.its.jpo.ode.coder.OdeSrmDataCreatorHelper;
import us.dot.its.jpo.ode.coder.OdePsmDataCreatorHelper;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.plugin.j2735.J2735DSRCmsgID;
import us.dot.its.jpo.ode.traveler.TimTransmogrifier;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.wrapper.AbstractSubscriberProcessor;
import us.dot.its.jpo.ode.wrapper.MessageProducer;
import us.dot.its.jpo.ode.wrapper.serdes.OdeBsmSerializer;

public class Asn1DecodedDataRouter extends AbstractSubscriberProcessor<String, String> {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private OdeProperties odeProperties;
	private OdeKafkaProperties odeKafkaProps;
	private MessageProducer<String, OdeBsmData> bsmProducer;
	private MessageProducer<String, String> timProducer;
	private MessageProducer<String, String> spatProducer;
	private MessageProducer<String, String> mapProducer;
	private MessageProducer<String, String> ssmProducer;
	private MessageProducer<String, String> srmProducer;
	private MessageProducer<String, String> psmProducer;

	public Asn1DecodedDataRouter(OdeProperties odeProps, OdeKafkaProperties odeKafkaProperties) {
		super();
		this.odeProperties = odeProps;
		this.odeKafkaProps = odeKafkaProperties;
		this.bsmProducer = new MessageProducer<>(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				null,
				OdeBsmSerializer.class.getName(),
				odeKafkaProperties.getDisabledTopics());
		this.timProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
		this.spatProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
		this.mapProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
		this.ssmProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
		this.srmProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
		this.psmProducer = MessageProducer.defaultStringMessageProducer(odeKafkaProperties.getBrokers(),
				odeKafkaProperties.getProducerType(),
				odeKafkaProperties.getDisabledTopics());
	}

	@Override
	public Object process(String consumedData) {
		try {
			JSONObject consumed = XmlUtils.toJSONObject(consumedData).getJSONObject(OdeAsn1Data.class.getSimpleName());
			J2735DSRCmsgID messageId = J2735DSRCmsgID.valueOf(
					consumed.getJSONObject(AppContext.PAYLOAD_STRING)
							.getJSONObject(AppContext.DATA_STRING)
							.getJSONObject("MessageFrame")
							.getInt("messageId")
			);

			RecordType recordType = RecordType
					.valueOf(consumed.getJSONObject(AppContext.METADATA_STRING).getString("recordType"));

            switch (messageId) {
                case BasicSafetyMessage -> {
                    // ODE-518/ODE-604 Demultiplex the messages to appropriate topics based on the
                    // "recordType"
                    OdeBsmData odeBsmData = OdeBsmDataCreatorHelper.createOdeBsmData(consumedData);
                    switch (recordType) {
                        case bsmLogDuringEvent ->
                                bsmProducer.send(odeKafkaProps.getBsmProperties().getDuringEventPojoTopic(), getRecord().key(), odeBsmData);
                        case rxMsg ->
                                bsmProducer.send(odeKafkaProps.getBsmProperties().getRxPojoTopic(), getRecord().key(), odeBsmData);
                        case bsmTx ->
                                bsmProducer.send(odeKafkaProps.getBsmProperties().getTxPojoTopic(), getRecord().key(), odeBsmData);
                        default -> logger.trace("Consumed BSM data with record type: {}", recordType);
                    }
                    // Send all BSMs also to OdeBsmPojo
                    bsmProducer.send(odeKafkaProps.getBsmProperties().getPojoTopic(), getRecord().key(), odeBsmData);
                    logger.debug("Submitted to BSM Pojo topic");
                }
                case TravelerInformation -> {
                    String odeTimData = TimTransmogrifier.createOdeTimData(consumed).toString();
                    switch (recordType) {
                        case dnMsg ->
                                timProducer.send(odeProperties.getKafkaTopicOdeDNMsgJson(), getRecord().key(), odeTimData);
                        case rxMsg ->
                                timProducer.send(odeProperties.getKafkaTopicOdeTimRxJson(), getRecord().key(), odeTimData);
						default -> logger.trace("Consumed TIM data with record type: {}", recordType);
                    }
                    // Send all TIMs also to OdeTimJson
                    timProducer.send(odeProperties.getKafkaTopicOdeTimJson(), getRecord().key(), odeTimData);
                    logger.debug("Submitted to TIM Pojo topic");
                }
                case SPATMessage -> {
                    String odeSpatData = OdeSpatDataCreatorHelper.createOdeSpatData(consumedData).toString();
                    switch (recordType) {
                        case dnMsg ->
                                spatProducer.send(odeProperties.getKafkaTopicOdeDNMsgJson(), getRecord().key(), odeSpatData);
                        case rxMsg ->
                                spatProducer.send(odeProperties.getKafkaTopicOdeSpatRxJson(), getRecord().key(), odeSpatData);
                        case spatTx ->
                                spatProducer.send(odeProperties.getKafkaTopicOdeSpatTxPojo(), getRecord().key(), odeSpatData);
                        default -> logger.trace("Consumed SPAT data with record type: {}", recordType);
                    }
                    // Send all SPATs also to OdeSpatJson
                    spatProducer.send(odeProperties.getKafkaTopicOdeSpatJson(), getRecord().key(), odeSpatData);
                    logger.debug("Submitted to SPAT Pojo topic");
                }
                case MAPMessage -> {
                    String odeMapData = OdeMapDataCreatorHelper.createOdeMapData(consumedData).toString();
                    if (recordType == RecordType.mapTx) {
                        mapProducer.send(odeProperties.getKafkaTopicOdeMapTxPojo(), getRecord().key(), odeMapData);
                    }
                    // Send all Map also to OdeMapJson
                    mapProducer.send(odeProperties.getKafkaTopicOdeMapJson(), getRecord().key(), odeMapData);
                    logger.debug("Submitted to MAP Pojo topic");
                }
                case SSMMessage -> {
                    String odeSsmData = OdeSsmDataCreatorHelper.createOdeSsmData(consumedData).toString();
                    if (recordType == RecordType.ssmTx) {
                        ssmProducer.send(odeProperties.getKafkaTopicOdeSsmPojo(), getRecord().key(), odeSsmData);
                    }
                    // Send all SSMs also to OdeSsmJson
                    ssmProducer.send(odeProperties.getKafkaTopicOdeSsmJson(), getRecord().key(), odeSsmData);
                    logger.debug("Submitted to SSM Pojo topic");
                }
                case SRMMessage -> {
                    String odeSrmData = OdeSrmDataCreatorHelper.createOdeSrmData(consumedData).toString();
                    if (recordType == RecordType.srmTx) {
                        srmProducer.send(odeProperties.getKafkaTopicOdeSrmTxPojo(), getRecord().key(), odeSrmData);
                    }
                    // Send all SRMs also to OdeSrmJson
                    srmProducer.send(odeProperties.getKafkaTopicOdeSrmJson(), getRecord().key(), odeSrmData);
                    logger.debug("Submitted to SRM Pojo topic");
                }
                case PersonalSafetyMessage -> {
                    String odePsmData = OdePsmDataCreatorHelper.createOdePsmData(consumedData).toString();
                    if (recordType == RecordType.psmTx) {
                        psmProducer.send(odeProperties.getKafkaTopicOdePsmTxPojo(), getRecord().key(), odePsmData);
                    }
                    // Send all PSMs also to OdePsmJson
                    psmProducer.send(odeProperties.getKafkaTopicOdePsmJson(), getRecord().key(), odePsmData);
                    logger.debug("Submitted to PSM Pojo topic");
                }
                case null, default -> logger.warn("Unknown message type: {}", messageId);
            }
		} catch (Exception e) {
            logger.error("Failed to route received data: {}", consumedData, e);
		}
		return null;
	}
}
