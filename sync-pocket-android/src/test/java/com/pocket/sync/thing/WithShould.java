package com.pocket.sync.thing;

import com.pocket.sync.test.generated.thing.InterfaceAllIdentifiableImpl1;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiable;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableDeep;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableId;
import com.pocket.sync.test.generated.thing.InterfaceMixIdentifiableNon;
import com.pocket.sync.test.generated.thing.NestedIdentity;
import com.pocket.sync.test.generated.thing.OpenUsages;
import com.pocket.sync.test.generated.thing.SomethingWithIdentity;
import com.pocket.sync.test.generated.thing.StateHasIdentifiable;
import com.pocket.sync.test.generated.thing.WithTest;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests all of the cases that the code generator writes different with() implementations for
 */
public class WithShould {

	@Test
	public void replace_identifiable() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		WithTest value = new WithTest.Builder().identifiable_thing(start).build();
		
		// when
		WithTest changed = value.with(start::equals, replace);
		
		// then
		Assert.assertEquals(replace.state, changed.identifiable_thing.state);
	}
	
	@Test
	public void replace_within_non_identifiable() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		WithTest value = new WithTest.Builder().non_identifiable_thing(new StateHasIdentifiable.Builder().state(start).build()).build();
		// when
		WithTest changed = value.with(start::equals, replace);
		// then
		Assert.assertEquals(replace.state, changed.non_identifiable_thing.state.state);
	}
	
	@Test
	public void replace_within_list_of_identifiables() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		WithTest value = new WithTest.Builder().list_of_identifiable_thing(Collections.singletonList(start)).build();
		
		// when
		WithTest changed = value.with(start::equals, replace);
		
		// then
		Assert.assertEquals(replace.state, changed.list_of_identifiable_thing.get(0).state);
	}
	
	@Test
	public void replace_within_list_of_non_identifiables() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		WithTest value = new WithTest.Builder().list_of_non_identifiable_thing(Collections.singletonList(new StateHasIdentifiable.Builder().state(start).build())).build();
		
		// when
		WithTest changed = value.with(start::equals, replace);
		
		// then
		Assert.assertEquals(replace.state, changed.list_of_non_identifiable_thing.get(0).state.state);
	}
	
	@Test
	public void replace_within_map_of_identifiables() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		Map<String, SomethingWithIdentity> map = new HashMap<>();
		map.put("key", start);
		WithTest value = new WithTest.Builder().map_of_identifiable_thing(map).build();
		
		// when
		WithTest changed = value.with(start::equals, replace);
		
		// then
		Assert.assertEquals(replace.state, changed.map_of_identifiable_thing.get("key").state);
	}
	
	@Test
	public void replace_within_map_of_non_identifiables() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		Map<String, StateHasIdentifiable> map = new HashMap<>();
		map.put("key", new StateHasIdentifiable.Builder().state(start).build());
		WithTest value = new WithTest.Builder().map_of_non_identifiable_thing(map).build();
		
		// when
		WithTest changed = value.with(start::equals, replace);
		
		// then
		Assert.assertEquals(replace.state, changed.map_of_non_identifiable_thing.get("key").state.state);
	}

	@Test
	public void replace_interface_in_field() {
		// given
		InterfaceAllIdentifiableImpl1 start = new InterfaceAllIdentifiableImpl1.Builder().id_i("A").state("1").build();
		InterfaceAllIdentifiableImpl1 replace = start.builder().state("2").build();
		OpenUsages value = new OpenUsages.Builder().interface_of_ids(start).build();

		// when
		OpenUsages changed = value.with(start::equals, replace);

		// then
		Assert.assertEquals(replace.state, changed.interface_of_ids._state());
	}

	@Test
	public void replace_interface_in_list() {
		// given
		InterfaceAllIdentifiableImpl1 start = new InterfaceAllIdentifiableImpl1.Builder().id_i("A").state("1").build();
		InterfaceAllIdentifiableImpl1 replace = start.builder().state("2").build();
		OpenUsages value = new OpenUsages.Builder().interface_of_ids_list(Collections.singletonList(start)).build();

		// when
		OpenUsages changed = value.with(start::equals, replace);

		// then
		Assert.assertEquals(replace.state, changed.interface_of_ids_list.get(0)._state());
	}

	@Test
	public void replace_interface_in_mixed_field() {
		// given
		InterfaceMixIdentifiableId start = new InterfaceMixIdentifiableId.Builder().id("A").state_i("1").build();
		InterfaceMixIdentifiableId replace = start.builder().state_i("2").build();
		OpenUsages value = new OpenUsages.Builder().interface_of_mix_ids(start).build();

		// when
		OpenUsages changed = value.with(start::equals, replace);

		// then
		Assert.assertEquals(replace.state_i, ((InterfaceMixIdentifiableId) changed.interface_of_mix_ids).state_i);
	}

	@Test
	public void replace_interface_in_mixed_list() {
		// given
		InterfaceMixIdentifiableId start = new InterfaceMixIdentifiableId.Builder().id("A").state_i("1").build();
		InterfaceMixIdentifiableId replace = start.builder().state_i("2").build();
		ArrayList<InterfaceMixIdentifiable> list = new ArrayList<>();
		list.add(start);
		list.add(new InterfaceMixIdentifiableNon.Builder().state("non").build());
		OpenUsages value = new OpenUsages.Builder().interface_of_mix_ids_list(list).build();

		// when
		OpenUsages changed = value.with(start::equals, replace);

		// then
		Assert.assertEquals(replace.state_i, changed.interface_of_mix_ids_list.get(0)._state_i());
	}

	@Test
	public void replace_interface_in_deep_mixed_field() {
		// given
		SomethingWithIdentity start = new SomethingWithIdentity.Builder().id("A").state("1").build();
		SomethingWithIdentity replace = start.builder().state("2").build();
		OpenUsages value = new OpenUsages.Builder()
				.interface_of_mix_ids(new InterfaceMixIdentifiableDeep.Builder()
						.state_contains_id(new NestedIdentity.Builder()
								.nested(start).build()
						).build()
				).build();

		// when
		OpenUsages changed = value.with(start::equals, replace);

		// then
		InterfaceMixIdentifiableDeep i = (InterfaceMixIdentifiableDeep) changed.interface_of_mix_ids;
		Assert.assertEquals(replace.state, i.state_contains_id.nested.state);
	}
	
}
