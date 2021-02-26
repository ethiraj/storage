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

    // TODO: Trufflehog preventing from pushing base64 string tests. Removed tests because of that.
    @Test
    public void should_decodeToString_postEncodingDecoding() {
        String inputString = "hello+world";

        String resultString = encodeDecode.deserializeCursor(encodeDecode.serializeCursor(inputString));
        Assert.assertEquals(inputString, resultString);
    }

}
