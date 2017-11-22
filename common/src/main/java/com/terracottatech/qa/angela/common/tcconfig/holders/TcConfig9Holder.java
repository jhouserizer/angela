package com.terracottatech.qa.angela.common.tcconfig.holders;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.InputStream;

/**
 * Terracotta config for Terracotta 4.1+
 * <p>
 * 9.0 -> 4.1.x
 * 9.1 -> 4.2.x
 * 9.2 -> 4.3.x
 *
 * @author Aurelien Broszniowski
 */
public class TcConfig9Holder extends TcConfigHolder {

  public TcConfig9Holder(final InputStream tcConfigInputStream) {
    super(tcConfigInputStream);
  }

  @Override
  protected NodeList getServersList(Document tcConfigXml, XPath xPath) throws XPathExpressionException {
    return (NodeList) xPath.evaluate("//*[name()='servers']//*[name()='server']", tcConfigXml.getDocumentElement(), XPathConstants.NODESET);
  }

  @Override
  public void updateDataDirectory(final String rootId, final String newlocation) {
    throw new UnsupportedOperationException("Unimplemented");
  }

  @Override
  public void updateHostname(final String serverName, final String hostname) {
    throw new UnsupportedOperationException("Unimplemented");
  }
}