package org.mule.extension.email.internal.value;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;
import org.mule.extension.email.internal.sender.EmailBody;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

/**
 * {@link ValueProvider} implementation which provides the possible and supported values for the {@link EmailBody#contentTransferEncoding}
 * parameter.
 *
 * @since 1.0
 */
public final class ContentTransferEncodingValueProvider implements ValueProvider {

  private static final Set<Value> encodings = getValuesFor("Base64", "7BIT", "8BIT", "Quoted-Printable", "Binary");

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return encodings;
  }
}
