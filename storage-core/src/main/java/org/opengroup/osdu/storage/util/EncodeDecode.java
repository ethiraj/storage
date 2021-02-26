package org.opengroup.osdu.storage.util;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncodeDecode {

    public String deserializeCursor(String cursor) {
        if(StringUtils.isEmpty(cursor)) {
            return cursor;
        }
        return new String(Base64.getDecoder().decode(cursor));
    }

    public String serializeCursor(String continuationToken) {
        if(StringUtils.isEmpty(continuationToken)) {
            return continuationToken;
        }
        return Base64.getEncoder().encodeToString(continuationToken.getBytes());
    }

    private AppException getInvalidCursorException() {
        return new AppException(HttpStatus.SC_BAD_REQUEST, "Cursor invalid",
                "The requested cursor does not exist or is invalid");
    }

}
