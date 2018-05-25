package de.fraunhofer.iais.eis.usage;

import de.fraunhofer.iais.eis.general.TestUtil;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import de.fraunhofer.iais.eis.util.VocabUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import de.fraunhofer.iais.eis.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.GregorianCalendar;


/**
 * Created by christian on 22.05.17.
 */
public class DataTransferUsageTest {

    @Test
    public void dataTransferSerialization() throws Exception {
        String serializedHeader = createDataTransfer().toRdf();

        Model model = TestUtil.createModelFromRdf(serializedHeader);
        Resource transfer = model.createResource("http://example.org/transfer/1");
        Property authTokenProperty = model.createProperty("https://schema.industrialdataspace.org/dataTransfer/authToken");
        Property hashFuncProperty = model.createProperty("https://schema.industrialdataspace.org/dataTransfer/hashFunction");
        Property recvProperty = model.createProperty("https://schema.industrialdataspace.org/dataTransfer/receiver");
        Property sendProperty = model.createProperty("https://schema.industrialdataspace.org/dataTransfer/sender");
        Property creatProperty = model.createProperty("https://schema.industrialdataspace.org/dataTransfer/transferCreatedAt");

        for (Property property : Arrays.asList(authTokenProperty, hashFuncProperty, recvProperty, sendProperty, creatProperty)) {
            Assert.assertTrue(model.listObjectsOfProperty(transfer, property).hasNext());
        }
    }

    private DataTransfer createDataTransfer() throws Exception {

        HashFunction hashFunction = HashFunction.SHA_512;
        return new DataTransferBuilder(new URL("http://example.org/transfer/1"))
            .sender(new URL("http://www.fraunhofer.de/Broker"))
            .receiver(new URL("http://www.fraunhofer.de/Broker"))
            .transferCreatedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()))
            .hashFunction(hashFunction)
            .payloadDigest(createDigest())
            .authToken(new AuthTokenBuilder().tokenValue("token").build())
            .build();
    }

    @Test(expected = ConstraintViolationException.class)
    public void InvalidDataTransfer() throws ConstraintViolationException {
        new DataTransferBuilder().build();
    }

    @Test
    public void dataTransferDeserialization() throws Exception {
        String serializedHeader = createDataTransfer().toRdf();
        DataTransferImpl obj = (DataTransferImpl) VocabUtil.fromRdf(serializedHeader);

        Assert.assertEquals(obj.getAuthToken().getTokenValue(), "token");
        Assert.assertEquals(obj.getHashFunction(), HashFunction.SHA_512);
        Assert.assertEquals(obj.getReceiver().toString(), "http://www.fraunhofer.de/Broker");
        Assert.assertEquals(obj.getSender().toString(), "http://www.fraunhofer.de/Broker");

        XMLGregorianCalendar now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        Assert.assertEquals(obj.getTransferCreatedAt().compare(now), -1);
        Assert.assertArrayEquals(createDigest(), obj.getPayloadDigest());
    }

    @Test
    public void preserveInformationThroughDeserialization()
            throws Exception {
        DataTransfer original = createDataTransfer();
        DataTransfer deserialized = (DataTransfer) VocabUtil.fromRdf(original.toRdf());

        Model originalModel = ModelFactory.createDefaultModel();
        originalModel.read(new ByteArrayInputStream(original.toRdf().getBytes()), null, "TURTLE");

        Model deserializedModel = ModelFactory.createDefaultModel();
        deserializedModel.read(new ByteArrayInputStream(deserialized.toRdf().getBytes()), null, "TURTLE");

        Assert.assertTrue(originalModel.isIsomorphicWith(deserializedModel));
        ReflectionAssert.assertReflectionEquals(original, deserialized);
    }

    private byte[] createDigest() throws Exception{
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update("salt".getBytes("UTF-8"));
        return md.digest("mymessage".getBytes("UTF-8"));
    }

}
