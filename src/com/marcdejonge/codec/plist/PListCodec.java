package com.marcdejonge.codec.plist;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.ParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class PListCodec {
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US);

	private final Document document;

	public PListCodec(Reader reader) throws ParseException {
		Document document = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new InputSource(reader));
		} catch (ParserConfigurationException e) {
			throw new RuntimeException();
		} catch (SAXException e) {
			if (!"Unexpected end of document".equals(e.getMessage())) {
				throw new ParseException(e);
			}
		} catch (IOException e) {
			throw new ParseException(e);
		}

		this.document = document;
	}

	public MixedMap parseExpectMap() throws ParseException {
		Object result = parsePropertyList();
		if (result == null) {
			return new MixedMap();
		} else if (result instanceof MixedMap) {
			return (MixedMap) result;
		} else {
			throw new ParseException("Did not contain a dict");
		}
	}

	public MixedList parseExpectList() throws ParseException {
		Object result = parsePropertyList();
		if (result == null) {
			return new MixedList();
		} else if (result instanceof MixedList) {
			return (MixedList) result;
		} else if (result instanceof Collection) {
			return new MixedList((Collection<?>) result);
		} else {
			throw new ParseException("Did not contain an array");
		}
	}

	private Object parsePropertyList() throws ParseException {
		if (document == null) {
			return null;
		}

		Element parent = document.getDocumentElement();
		if (parent == null) {
			return null;
		} else if (!parent.getTagName().equals("plist")) {
			throw new ParseException("Expected a plist");
		}

		NodeList children = parent.getChildNodes();
		for (int ix = 0; ix < children.getLength(); ix++) {
			Node node = children.item(ix);
			if (node instanceof Element) {
				try {
					return parseObject((Element) node);
				} catch (SAXParseException e) {
					throw new ParseException(e);
				}
			}
		}

		return null;
	}

	private Object parseObject(Element node) throws SAXParseException {
		switch (node.getTagName().toLowerCase(Locale.US)) {
		case "true":
			return Boolean.TRUE;
		case "false":
			return Boolean.FALSE;
		case "real":
			return Double.parseDouble(node.getTextContent().trim());
		case "integer":
			return Long.parseLong(node.getTextContent().trim());
		case "string":
			return node.getTextContent().trim();
		case "date":
			try {
				return DATE_FORMAT.parse(node.getTextContent().trim());
			} catch (java.text.ParseException e) {
				throw new SAXParseException("Illegal date format", null, e);
			}
		case "data":
			return Base64.getDecoder().decode(node.getTextContent().trim());
		case "dict":
			return parseDict(node.getChildNodes());
		case "array":
			return parseArray(node.getChildNodes());
		default:
			throw new SAXParseException("Unexpected element type: " + node.getTagName(), null);
		}
	}

	private MixedList parseArray(NodeList nodes) throws SAXParseException {
		MixedList result = new MixedList();
		for (int ix = 0; ix < nodes.getLength(); ix++) {
			Node node = nodes.item(ix);
			if (node instanceof Element) {
				result.add(parseObject((Element) node));
			}
		}
		return result;
	}

	private MixedMap parseDict(NodeList nodes) throws SAXParseException {
		MixedMap result = new MixedMap();
		String currentKey = null;
		for (int ix = 0; ix < nodes.getLength(); ix++) {
			Node node = nodes.item(ix);
			if (node instanceof Element) {
				Element elem = (Element) node;
				if (currentKey == null) {
					if (!elem.getTagName().equals("key")) {
						throw new SAXParseException("Expected a key in the dict", null);
					}
					currentKey = elem.getTextContent().trim();
				} else {
					result.put(currentKey, parseObject(elem));
					currentKey = null;
				}
			}
		}
		return result;
	}

	public static String generatePropertyList(Object object) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<!DOCTYPE plist PUBLIC \"-//Apple Inc//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
		sb.append("<plist version=\"1.0\">\n");
		appendObject(sb, object, 1);
		sb.append("</plist>\n");
		return sb.toString();
	}

	private static void appendObject(StringBuilder sb, Object object, int indent) {
		indent(sb, indent);
		if (object == null) {
			throw new NullPointerException();
		} else if (object instanceof Number) {
			Number nr = (Number) object;
			if (object instanceof Double || object instanceof Float) {
				sb.append("<real>").append(nr.doubleValue()).append("</real>\n");
			} else {
				sb.append("<integer>").append(nr.longValue()).append("</integer>\n");
			}
		} else if (object instanceof Boolean) {
			if (object.equals(Boolean.TRUE)) {
				sb.append("<true/>\n");
			} else {
				sb.append("<false/>\n");
			}
		} else if (object instanceof Date) {
			sb.append("<date>").append(DATE_FORMAT.format(object)).append("</date>\n");
		} else if (object instanceof byte[]) {
			sb.append("<data>").append(new String(Base64.getEncoder().encode((byte[]) object))).append("</data>\n");
		} else if (object instanceof Collection) {
			sb.append("<array>\n");
			for (Object value : (Collection<?>) object) {
				appendObject(sb, value, indent + 1);
			}
			indent(sb, indent);
			sb.append("</array>\n");
		} else if (object instanceof Map) {
			sb.append("<dict>\n");
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
				indent(sb, indent + 1);
				sb.append("<key>");
				addString(sb, entry.getKey());
				sb.append("</key>\n");
				appendObject(sb, entry.getValue(), indent + 1);
			}
			indent(sb, indent);
			sb.append("</dict>\n");
		} else {
			sb.append("<string>");
			addString(sb, object);
			sb.append("</string>\n");
		}
	}

	private static void addString(StringBuilder sb, Object stuff) {
		String string = stuff.toString();

		for (int ix = 0; ix < string.length(); ix++) {
			char c = string.charAt(ix);

			if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
				sb.append("\uFFFD");
			} else if (c >= 0xFFFD) {
				sb.append("\uFFFD");
			} else if (c == '&') {
				sb.append("&amp;");
			} else if (c == '<') {
				sb.append("&lt;");
			} else if (c == '>') {
				sb.append("&gt;");
			} else {
				sb.append(c);
			}
		}
	}

	private static void indent(StringBuilder sb, int indent) {
		for (int ix = 0; ix < indent; ix++) {
			sb.append("    ");
		}
	}

	@Override
	public String toString() {
		try {
			return parsePropertyList().toString();
		} catch (ParseException e) {
			return "Invalid property list: " + e.getMessage();
		} catch (NullPointerException e) {
			return "Invalid property list: null";
		}
	}
}
