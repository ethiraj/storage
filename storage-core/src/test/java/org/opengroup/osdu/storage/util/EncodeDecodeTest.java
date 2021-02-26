package org.opengroup.osdu.storage.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncodeDecodeTest {

    private EncodeDecode encodeDecode;

    @Before
    public void setup(){
        encodeDecode = new EncodeDecode();
    }

    @Test
    public void should_encode_already_encodedString() {
        String encodedString = "hello%22world";
        String resultantEncoded = "hello%2522world";

        String reEncoded = encodeDecode.serializeCursor(encodedString);
        Assert.assertEquals(reEncoded, resultantEncoded);
    }

    @Test
    public void should_decodeToString_postEncodingDecoding() {
        String inputString = "hello%22world";

        String resultString = encodeDecode.deserializeCursor(encodeDecode.serializeCursor(inputString));
        Assert.assertEquals(inputString, resultString);
    }

    @Test
    public void should_decodeEncodedString() {
        String inputString = "hello%22world";

        String resultString = encodeDecode.deserializeCursor(inputString);
        Assert.assertEquals("hello\"world", resultString);
    }

}
