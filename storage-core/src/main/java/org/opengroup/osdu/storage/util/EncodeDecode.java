package org.opengroup.osdu.storage.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

}
