package com.pocket.sync.source.protocol.graphql;

import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.sync.source.JsonConfig
import com.pocket.sync.source.result.Status
import com.pocket.sync.test.generated.SyncTestsRemoteStyle
import com.pocket.sync.test.generated.thing.GraphQlQueryReturnsScalar
import com.pocket.sync.test.generated.thing.GraphQlQueryReturnsThing
import org.apache.commons.io.IOUtils
import org.junit.Test
import java.io.InputStream
import kotlin.test.assertNotNull

class GraphQlSourceTest {

    private fun handlerMock(expectedResult: String, httpStatus: Int = 200, error: Throwable? = null) : GraphQlSource.HttpHandler {
        return object: GraphQlSource.HttpHandler {
            override fun execute(request: ObjectNode, response: (body: InputStream?, httpStatus: Int?, error: Throwable?) -> Unit) {
                response(IOUtils.toInputStream(expectedResult), httpStatus, error)
            }
        }
    }

    @Test
    fun `thing return`() {
        val expected = """
            {
            	"data": {
            		"thing": {
            			"scalar": "a scalar",
            			"object": {
            				"scalar": "object scalar"
            			}
            		}
            	}
            }
        """
        val source = GraphQlSource(handlerMock(expected), JsonConfig(SyncTestsRemoteStyle.CLIENT_API, true), false)
        val returned = source.syncFull(GraphQlQueryReturnsThing.Builder().build()).returned_t

        assert(returned.thing?.scalar.equals("a scalar"))
        assert(returned.thing?.`object`?.scalar.equals("object scalar"))
    }

    @Test
    fun `scalar return`() {
        val expected = """
            {
            	"data": {
            		"scalar": "this is it"
            	}
            }
        """
        val source = GraphQlSource(handlerMock(expected), JsonConfig(SyncTestsRemoteStyle.CLIENT_API, true), false)
        val returned = source.syncFull(GraphQlQueryReturnsScalar.Builder().build()).returned_t
        assert(returned.scalar.equals("this is it"))
    }

    @Test
    fun `error return`() {
        val expected = """
            {
            	"data": {
            		"returned": "this is it"
            	}
            }
        """
        val source = GraphQlSource(handlerMock(expected, 400, UnknownError()), JsonConfig(SyncTestsRemoteStyle.CLIENT_API, true), false)
        val result = source.syncFull(GraphQlQueryReturnsScalar.Builder().build())
        assert(result.hasFailures())
        assert(result.result_t.status == Status.FAILED)
        assert(result.returned_t == null)
    }

    @Test
    fun `bad server result`() {
        val source = GraphQlSource(handlerMock("{ nonsense }"), JsonConfig(SyncTestsRemoteStyle.CLIENT_API, true), false)
        val result = source.syncFull(GraphQlQueryReturnsScalar.Builder().build())
        assert(result.hasFailures())
        assert(result.result_t.status == Status.FAILED)
        assert(result.returned_t == null)
    }
}