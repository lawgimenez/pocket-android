package com.pocket.sync.parse.graphql

import com.pocket.sync.parse.ActionData
import com.pocket.sync.parse.FigmentsData
import com.pocket.sync.parse.ReferenceTypeData
import com.pocket.sync.parse.ThingData
import kotlin.test.*

class QueryParserShould {
    private val types = """
        type Query {
            user: User
        }
        type Mutation {
            updateSavedItemFavorite: SavedItem
        }
    """.trimIndent()

    private val operations = """
                query savedItems(§filter: SavedItemsFilter, §sort: SavedItemsSort, §pagination: PaginationInput) {
                    user {
                        savedItems(filter: §filter, sort: §sort, pagination: §pagination) {
                            edges {
                                node {
                                    url
                                }
                            }
                        }
                    }
                }
                
                mutation UpdateSavedItemFavorite(§id: ID!) {
                    updateSavedItemFavorite(id: §id) {
                        url
                    }
                }
                
                query withNestedFragment {
                    user {
                        ...userFields
                    }
                }
                
                query withFragmentDeeper {
                    user {
                        savedItems {
                            ...savedItemFields
                        }
                    }
                }
                
                fragment userFields on User {
                    savedItems {
                        ...savedItemFields
                    }
                }
                
                fragment savedItemFields on SavedItemConnection {
                    edges {
                        node {
                            url
                        }
                    }
                }
            """.trimIndent().replace('§', '$')

    private lateinit var data: FigmentsData
    private val query: ThingData
        get() = data.definitions[0] as ThingData
    private val mutation: ActionData
        get() = data.definitions[1] as ActionData
    private val withNestedFragment: ThingData
        get() = data.definitions[2] as ThingData
    private val withFragmentDeeper: ThingData
        get() = data.definitions[3] as ThingData

    @BeforeTest
    fun setup() {
        val spec = SpecParser().parse(types)
        data = QueryParser(spec).parse(operations)
    }

    @Test fun `find all operations`() {
        assertEquals(4, data.definitions.size, "Incorrect operation count.")
    }

    @Test fun `convert query to thing`() {
        assertIs<ThingData>(data.definitions[0])
    }

    @Test fun `parse query name`() {
        assertEquals("savedItems", query.definition.name, "Incorrect query name.")
    }

    @Test fun `parse query arguments`() {
        val arguments = query.syncable.fields.filter { it.identifying }
        assertEquals(3, arguments.size, "Incorrect argument count.")

        assertContentEquals(
            listOf("filter", "sort", "pagination"),
            arguments.map { it.name },
            "Incorrect argument names.",
        )
        assertTrue { arguments.all { it.type is ReferenceTypeData } }
        assertContentEquals(
            listOf("SavedItemsFilter", "SavedItemsSort", "PaginationInput"),
            arguments.map { (it.type as ReferenceTypeData).definition },
            "Incorrect argument types."
        )
    }

    @Test fun `parse query return type`() {
        val `return` = query.syncable.fields.single { it.root }
        assertIs<ReferenceTypeData>(`return`.type)
        assertEquals("User", (`return`.type as ReferenceTypeData).definition)
    }

    @Test fun `convert mutation to action`() {
        assertIs<ActionData>(data.definitions[1])
    }

    @Test fun `parse mutation name`() {
        assertEquals("UpdateSavedItemFavorite", mutation.definition.name, "Incorrect mutation name.")
    }

    @Test fun `parse mutation arguments`() {
        val arguments = mutation.syncable.fields.filter { it.identifying }
        assertEquals(1, arguments.size, "Incorrect argument count.")

        assertContentEquals(
            listOf("id"),
            arguments.map { it.name },
            "Incorrect argument names.",
        )
        assertTrue { arguments.all { it.type is ReferenceTypeData } }
        assertContentEquals(
            listOf("ID"),
            arguments.map { (it.type as ReferenceTypeData).definition },
            "Incorrect argument types."
        )
    }

    @Test fun `parse mutation return type`() {
        val `return` = mutation.resolves
        assertIs<ReferenceTypeData>(`return`!!.type)
        assertEquals("SavedItem", (`return`.type as ReferenceTypeData).definition)
    }

    @Test fun `include a nested fragment`() {
        assertEquals(
            """
                query withNestedFragment {
                  user {
                    ...userFields
                  }
                }
                
                fragment userFields on User {
                  savedItems {
                    ...savedItemFields
                  }
                }
                fragment savedItemFields on SavedItemConnection {
                  edges {
                    node {
                      url
                    }
                  }
                }
                
            """.trimIndent(),
            withNestedFragment.syncable.operation
        )
    }

    @Test fun `include a fragment found deeper in the selection`() {
        assertEquals(
            """
                query withFragmentDeeper {
                  user {
                    savedItems {
                      ...savedItemFields
                    }
                  }
                }
                
                fragment savedItemFields on SavedItemConnection {
                  edges {
                    node {
                      url
                    }
                  }
                }
                
            """.trimIndent(),
            withFragmentDeeper.syncable.operation
        )
    }
}
