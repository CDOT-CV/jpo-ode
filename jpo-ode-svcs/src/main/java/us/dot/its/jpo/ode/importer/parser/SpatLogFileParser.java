package us.dot.its.jpo.ode.importer.parser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeSpatMetadata.SpatSource;

public class SpatLogFileParser extends LogFileParser {
	private static final Logger logger = LoggerFactory.getLogger(SpatLogFileParser.class.getName());
	private static final int RX_FROM_LENGTH = 1;
	/*ieee 1609 (acceptable values 0 = no,1 =yes by default the Cert shall be present)*/
	private static final int IS_CERT_PRESENT_LENGTH= 1;

	private SpatSource spatSource;
	private boolean isCertPresent;

	public SpatLogFileParser(OdeLogMetadata.RecordType recordType, String fileName) {
		super(recordType, fileName);
		setIntersectionParser(new IntersectionParser(recordType, filename));
		setTimeParser(new TimeParser(recordType, filename));
		setSecResCodeParser(new SecurityResultCodeParser(recordType, filename));
		setPayloadParser(new PayloadParser(recordType, fileName));
	}
	
	@Override
	public ParserStatus parseFile(BufferedInputStream bis) throws FileParserException {
		ParserStatus status;
		try {
			status = super.parseFile(bis);
			if (status != ParserStatus.COMPLETE)
				return status;

			if (getStep() == 1) {
				status = parseStep(bis, RX_FROM_LENGTH);
				if (status != ParserStatus.COMPLETE)
					return status;
				setSpatSource(readBuffer);
			}

			if (getStep() == 2) {
				status = nextStep(bis, intersectionParser);
				if (status != ParserStatus.COMPLETE)
					return status;
			}

			if (getStep() == 3) {
				status = nextStep(bis, timeParser);
				if (status != ParserStatus.COMPLETE)
					return status;
			}

			if (getStep() == 4) {
				status = nextStep(bis, secResCodeParser);
				if (status != ParserStatus.COMPLETE)
					return status;
			}
			
			if (getStep() == 5) {
				status = parseStep(bis, IS_CERT_PRESENT_LENGTH);
				if (status != ParserStatus.COMPLETE)
					return status; 
				setCertPresent(readBuffer);
			}

			if (getStep() == 6) {
				status = nextStep(bis, payloadParser);
				if (status != ParserStatus.COMPLETE)
					return status;
			}

			resetStep();
			status = ParserStatus.COMPLETE;

		} catch (Exception e) {
			throw new FileParserException("Error parsing " + getFilename(), e);
		}

		return status;
	}

	
	public SpatSource getSpatSource() {
		return spatSource;
	}

	public void setSpatSource(SpatSource spatSource) {
		this.spatSource = spatSource;
	}

	public boolean isCertPresent() {
		return isCertPresent;
	}

	public void setCertPresent(boolean isCertPresent) {
		this.isCertPresent = isCertPresent;
	}
	
	public void setCertPresent(byte[] code) {
		try {
			setCertPresent(code[0] == 0? false: true);
		} catch (Exception e) {
			logger.error("Invalid Certificate Presence indicator: {}. Valid values are {}-{} inclusive", code, 0, SpatSource.values());
			setSpatSource(SpatSource.unknown);
		}
	}

	public void setSpatSource(byte[] code) {
		try {
			setSpatSource(SpatSource.values()[code[0]]);
		} catch (Exception e) {
			logger.error("Invalid SpatSource: {}. Valid values are {}-{} inclusive", code, 0, SpatSource.values());
			setSpatSource(SpatSource.unknown);
		}
	}

	@Override
	public void writeTo(OutputStream os) throws IOException {
		os.write((byte)spatSource.ordinal());
		super.writeTo(os);
	}
}
