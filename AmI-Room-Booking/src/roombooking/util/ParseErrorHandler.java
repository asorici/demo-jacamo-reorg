package roombooking.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ParseErrorHandler implements ErrorHandler {

	@Override
	public void error(SAXParseException exception) throws SAXException {
		System.out.println(exception.getMessage());
		throw exception;
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		System.out.println(exception.getMessage());
		throw exception;
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		System.out.println(exception.getMessage());
		throw exception;
	}

}
