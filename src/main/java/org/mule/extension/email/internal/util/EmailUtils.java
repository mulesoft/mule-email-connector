package org.mule.extension.email.internal.util;

import static java.nio.charset.Charset.forName;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import org.mule.extension.email.internal.sender.EmailBody;
import org.mule.runtime.api.metadata.MediaType;

import java.nio.charset.Charset;

/**
 * Utility class to share logic
 *
 * @since 1.0
 */
public class EmailUtils {

  private EmailUtils() {}

  /**
   * Resolves which is the {@link MediaType} that describes the body content.
   *
   * @param body          email body which contains the information about the content's charset
   * @param configCharset the default charset to be used if the content charset and the operation override charset are
   *                      not defined
   * @return the {@link MediaType} that describes the body content.
   */
  public static MediaType getMediaType(EmailBody body, String configCharset) {
    Charset charset = body.getContent()
        .getDataType()
        .getMediaType()
        .getCharset()
        .orElseGet(() -> forName(resolveOverride(configCharset, body.getOverrideEncoding())));

    MediaType mediaType = body.getContentType()
        .orElse(body.getContent().getDataType().getMediaType());

    if (mediaType.equals(ANY)) {
      mediaType = TEXT;
    }

    return mediaType.withCharset(charset);
  }

  public static <T> T resolveOverride(T configValue, T operationValue) {
    return operationValue == null ? configValue : operationValue;
  }
}
