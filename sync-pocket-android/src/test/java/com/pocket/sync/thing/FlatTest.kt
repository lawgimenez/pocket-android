package com.pocket.sync.thing

import com.pocket.sync.spec.Syncable
import com.pocket.sync.test.generated.thing.*
import com.pocket.sync.value.Include
import org.junit.Assert
import org.junit.Test

/**
 * Tests of [Thing.flat]
 */
class FlatTest {

    @Test
    fun `interface of ids`() {
        // given
        val child = InterfaceAllIdentifiableImpl1.Builder().id_i("A").state("state").build()
        val parent = OpenUsages.Builder().interface_of_ids(child).build()

        // then
        parent.assertFlattened(child)
    }

    @Test
    fun `list of interface of ids`() {
        // given
        val child1 = InterfaceAllIdentifiableImpl1.Builder().id_i("1").state("state").build()
        val child2 = InterfaceAllIdentifiableImpl2.Builder().id_i("2").state("state").build()
        val child3 = InterfaceAllIdentifiableImpl2.Builder().id_i("3").state("state").build()
        val parent = OpenUsages.Builder().interface_of_ids_list(listOf(child1, child2, child3)).build()

        // then
        parent.assertFlattened(child1, child2, child3)
    }

    @Test
    fun `id implementation within interface with a mixture of ids and non ids`() {
        // given
        val child = InterfaceMixIdentifiableId.Builder().id("A").state_i("state").build()
        val parent = OpenUsages.Builder().interface_of_mix_ids(child).build()

        // then
        parent.assertFlattened(child)
    }

    @Test
    fun `non id implementation within interface with a mixture of ids and non ids`() {
        // given
        val child = InterfaceMixIdentifiableNon.Builder().state("A").state_i("state").build()
        val parent = OpenUsages.Builder().interface_of_mix_ids(child).build()

        // then
        parent.assertFlattened() // Since it isn't identifiable, shouldn't be found.
    }

    @Test
    fun `id implementation within a list of interface with a mixture of ids and non ids`() {
        // given
        val child1 = InterfaceMixIdentifiableId.Builder().id("1").state_i("state").build()
        val child2 = InterfaceMixIdentifiableNon.Builder().state("2").state_i("state").build()
        val child3 = SomethingWithIdentity.Builder().id("3").state("state").build()
        val child3Wrapper = InterfaceMixIdentifiableDeep.Builder().state_contains_id(NestedIdentity.Builder().nested(child3).build()).build()
        val parent = OpenUsages.Builder().interface_of_mix_ids_list(listOf(child1, child2, child3Wrapper)).build()

        // then
        parent.assertFlattened(child1, child3) // child2 shouldn't be found, since it isn't identifiable.
    }

    /**
     * @param subthings A list of any identifiable things expected to be found within it. (the state of these don't matter, just identity is used internally)
     */
    private fun Thing.assertFlattened(vararg subthings: Thing) {
        val flattened = this.flat()

        // Flat equality should be equal
        Assert.assertTrue(flattened.equals(Thing.Equality.FLAT, this))

        // Json representation should be different (if there are any identifiable things)
        if (subthings.isNotEmpty()) Assert.assertNotEquals(flattened.toJson(Syncable.NO_ALIASES, Include.DANGEROUS), this)

        // Verify all of them are only their identity now.
        val found = FlatUtils.references(flattened)
        found.forEach {
            Assert.assertTrue(it.identity().equals(Thing.Equality.STATE, it)) // Since they are reduced to identity, only fields that are set (and compared here) should be id fields
        }

        // Also make sure there aren't missing/extra things
        Assert.assertEquals(subthings.size, found.size)
        found.removeAll(subthings)
        Assert.assertTrue(found.isEmpty())
    }

}