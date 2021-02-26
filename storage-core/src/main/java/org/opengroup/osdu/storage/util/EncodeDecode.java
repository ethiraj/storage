package org.opengroup.osdu.storage.util;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class EncodeDecode {

    public String deserializeCursor(String cursor) {
        if(StringUtils.isEmpty(cursor)) {
            return cursor;
        }
        try {
            return URLDecoder.decode(cursor, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException encodingException) {
            throw this.getInvalidCursorException();
        }
    }

    public String serializeCursor(String continuationToken) {
        if(StringUtils.isEmpty(continuationToken)) {
            return continuationToken;
        }
        try {
            return URLEncoder.encode(continuationToken, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException encodingException) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed serializing the cursor.",
                    encodingException.getMessage(), encodingException);
        }
    }

    private AppException getInvalidCursorException() {
        return new AppException(HttpStatus.SC_BAD_REQUEST, "Cursor invalid",
                "The requested cursor does not exist or is invalid");
    }

}
