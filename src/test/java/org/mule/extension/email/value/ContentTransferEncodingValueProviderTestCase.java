package org.mule.extension.email.value;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

public class ContentTransferEncodingValueProviderTestCase extends EmailConnectorTestCase {

  @Inject
  private ValueProviderService service;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Rule
  public SystemProperty systemProperty = disableAutomaticTestConnectivity();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"sender/smtp.xml", "sender/smtp-flows.xml"};
  }

  @Override
  public String getProtocol() {
    return "SMTP";
  }

  @Test
  public void contentTransferEncodingAsValueProvider() {
    ValueResult values = service.getValues(Location.builder().globalName("sendEmail").addProcessorsPart().addIndexPart(0).build(),
                                           "contentTransferEncoding");
    assertThat(values.isSuccess(), is(true));
    assertThat(values.getValues(),
               hasItems(valueWithId("7BIT"), valueWithId("8BIT"), valueWithId("Quoted-Printable"), valueWithId("Binary")));
  }
}
