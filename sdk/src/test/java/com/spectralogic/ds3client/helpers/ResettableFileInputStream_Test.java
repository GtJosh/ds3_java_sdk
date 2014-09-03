/*
 * ******************************************************************************
 *   Copyright 2014 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ****************************************************************************
 */

package com.spectralogic.ds3client.helpers;

import com.spectralogic.ds3client.utils.ResourceUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;

public class ResettableFileInputStream_Test {
    @Test
    public void testReset() throws IOException, URISyntaxException {

        final byte[] expectedBytes = Files.readAllBytes(ResourceUtils.loadFileResource("LoremIpsumTwice.txt").toPath());
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(expectedBytes.length)) {
            final File resourceFile = ResourceUtils.loadFileResource("LoremIpsum.txt");
            try (final InputStream inputStream = new ResettableFileInputStream(new FileInputStream(resourceFile))) {
                IOUtils.copy(inputStream, byteArrayOutputStream);
                inputStream.reset();
                IOUtils.copy(inputStream, byteArrayOutputStream);
            }
            Assert.assertArrayEquals(expectedBytes, byteArrayOutputStream.toByteArray());
        }
    }

}
