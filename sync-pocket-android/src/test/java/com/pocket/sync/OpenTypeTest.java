package com.pocket.sync;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.value.Dangerous;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.test.generated.Modeller;
import com.pocket.sync.test.generated.thing.OpenDangerousUsages;
import com.pocket.sync.test.generated.thing.OpenUsages;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.StreamingThingParser;
import com.pocket.sync.value.SyncableParser;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.binary.ByteWriter;
import com.pocket.sync.value.protect.StringEncrypter;
import com.pocket.util.java.JsonUtil;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class OpenTypeTest {

    /** For testing purposes, no special Json parsing configuration rules are used here */
    private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

    @Test
    public void json() throws IOException {
        checkJsonParsing(ThingMock.string().openUsages(), OpenUsages.JSON_CREATOR, OpenUsages.STREAMING_JSON_CREATOR);
    }

    @Test
    public void compression() throws IOException {
        checkCompression(ThingMock.thing().openUsages());
    }

    @Test
    public void dangerous() throws IOException {
        OpenDangerousUsages thing = ThingMock.thing().openDangerousUsages();

        // First just confirm without redacting, serialization is working as expected
        checkJsonParsing(ThingMock.string().openDangerousUsages(), OpenDangerousUsages.JSON_CREATOR, OpenDangerousUsages.STREAMING_JSON_CREATOR);
        checkCompression(thing);

        // Then verify redaction works

        // These are the expected values in the test json
        String unredactedString = "dangerous";
        String redactedString = "redacted";
        StringEncrypter encrypter = new StringEncrypter() {
            @Override
            public String encrypt(String value) {
                return redactedString;
            }
            @Override
            public String decrypt(String value) {
                return unredactedString;
            }
        };
        OpenDangerousUsages redacted = thing.redact(encrypter);
        assertRedacted(redacted.dangerous_impl.dangerous);
        assertRedacted(redacted.dangerous_impl.contains.dangerous);
        assertRedacted(redacted.dangerous_impl_list.get(0).dangerous);
        assertRedacted(redacted.dangerous_impl_list.get(0).contains.dangerous);
        assertRedacted(redacted.dangerous_interface._dangerous());
        assertRedacted(redacted.dangerous_interface._contains().dangerous);
        assertRedacted(redacted.dangerous_interface_list.get(0)._dangerous());
        assertRedacted(redacted.dangerous_interface_list.get(0)._contains().dangerous);
    }

    private void assertRedacted(Dangerous value) {
        Assert.assertEquals("redacted", value.value);
    }



    private <T extends Thing> void checkJsonParsing(String json, SyncableParser<T> object, StreamingThingParser<T> streaming) throws IOException {
        Include[] includes = new Include[]{Include.DANGEROUS, Include.OPEN_TYPE};

        // From and To ObjectNode
        ObjectNode tree = (ObjectNode) Modeller.OBJECT_MAPPER.readTree(json);
        try {
            object.create(tree, JSON_CONFIG);
            Assert.fail("Didn't throw an exception for unknown types");
        } catch (RuntimeException expected) {
        }
        T fromTree = object.create(tree, JSON_CONFIG, Allow.UNKNOWN);
        ObjectNode backToJson = fromTree.toJson(JSON_CONFIG, includes);
        if (!tree.equals(backToJson)) {
            List<String> diffs =
                    JsonUtil.diff2(tree, backToJson, JsonUtil.EqualsFlag.ANY_NUMERICAL)
                    .stream()
                    .filter(d -> !d.contains("Unknown"))
                    .collect(Collectors.toList());
            if (!diffs.isEmpty()) {
                System.out.println("Differences found:");
                for (String diff : diffs) {
                    System.out.println(diff);
                }
                Assert.fail();
            }
        }

        // From Stream
        T fromStreaming = streaming.create(Modeller.OBJECT_MAPPER.getFactory().createParser(json), JSON_CONFIG, Allow.UNKNOWN);
        Assert.assertEquals(fromTree, fromStreaming);
        Assert.assertEquals(backToJson, fromStreaming.toJson(JSON_CONFIG, includes));
    }

    private void checkCompression(Thing thing) {
        // To bytes and back
        ByteWriter writer = new ByteWriter();
        thing.compress(writer);
        ByteReader reader = new ByteReader(writer.readByteArray());
        Thing reparsed = (Thing) thing.getByteCreator().create(reader);
        SyncAsserts.equalsState(thing, reparsed);
    }
}
