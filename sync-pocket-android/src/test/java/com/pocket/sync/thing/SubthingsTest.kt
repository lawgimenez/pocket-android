package com.pocket.sync.thing

import com.pocket.sync.test.generated.thing.*
import org.junit.Assert
import org.junit.Test

/**
 * Tests of [Thing.subthings]
 */
class SubthingsTest {

    @Test
    fun `interface of ids only`() {
        // given
        val needle = InterfaceAllIdentifiableImpl1.Builder().id_i("A").build()
        val haystack = OpenUsages.Builder().interface_of_ids(needle).build()

        // then
        haystack.assertOnlyHasSubthingsOf(needle)
    }

    @Test
    fun `list of interface of ids only`() {
        // given
        val needle1 = InterfaceAllIdentifiableImpl1.Builder().id_i("1").build()
        val needle2 = InterfaceAllIdentifiableImpl2.Builder().id_i("2").build()
        val needle3 = InterfaceAllIdentifiableImpl2.Builder().id_i("3").build()
        val haystack = OpenUsages.Builder().interface_of_ids_list(listOf(needle1, needle2, needle3)).build()

        // then
        haystack.assertOnlyHasSubthingsOf(needle1, needle2, needle3)
    }

    @Test
    fun `id implementation within interface with a mixture of ids and non ids`() {
        // given
        val needle = InterfaceMixIdentifiableId.Builder().id("A").build()
        val haystack = OpenUsages.Builder().interface_of_mix_ids(needle).build()

        // then
        haystack.assertOnlyHasSubthingsOf(needle)
    }

    @Test
    fun `non id implementation within interface with a mixture of ids and non ids`() {
        // given
        val needle = InterfaceMixIdentifiableNon.Builder().state("A").build()
        val haystack = OpenUsages.Builder().interface_of_mix_ids(needle).build()

        // then
        haystack.assertOnlyHasSubthingsOf() // Since it isn't identifiable, shouldn't be found.
    }

    @Test
    fun `id implementation within a list of interface with a mixture of ids and non ids`() {
        // given
        val needle1 = InterfaceMixIdentifiableId.Builder().id("1").build()
        val needle2 = InterfaceMixIdentifiableNon.Builder().state("2").build()
        val needle3 = SomethingWithIdentity.Builder().id("3").build()
        val needle3Wrapper = InterfaceMixIdentifiableDeep.Builder().state_contains_id(NestedIdentity.Builder().nested(needle3).build()).build()
        val haystack = OpenUsages.Builder().interface_of_mix_ids_list(listOf(needle1, needle2, needle3Wrapper)).build()

        // then
        haystack.assertOnlyHasSubthingsOf(needle1, needle3) // needle2 shouldn't be found, since it isn't identifiable.
    }

    private fun Thing.assertOnlyHasSubthingsOf(vararg things: Thing) {
        val found = FlatUtils.references(this)
        Assert.assertEquals(things.size, found.size)
        found.removeAll(things)
        Assert.assertTrue(found.isEmpty())
    }

}