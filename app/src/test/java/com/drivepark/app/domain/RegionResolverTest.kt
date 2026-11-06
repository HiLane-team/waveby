package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionResolverTest {
    private val resolver = RegionResolver()

    @Test
    fun resolvesKnownTowns() {
        assertEquals("hoengseong", resolver.resolve(LatLng(37.49, 127.98))?.id)
        assertEquals("boseong", resolver.resolve(LatLng(34.77, 127.08))?.id)
        assertEquals("jeju", resolver.resolve(LatLng(33.50, 126.53))?.id)
        assertEquals("gangneung", resolver.resolve(LatLng(37.75, 128.88))?.id)
        assertEquals("yeongdeok", resolver.resolve(LatLng(36.41, 129.37))?.id)
    }

    @Test
    fun outsideAll_returnsNull() {
        assertNull(resolver.resolve(LatLng(37.57, 126.98)))
    }

    @Test
    fun boundary_isInclusive() {
        assertEquals("hoengseong", resolver.resolve(LatLng(37.35, 127.85))?.id)
        assertEquals("hoengseong", resolver.resolve(LatLng(37.65, 128.25))?.id)
    }
}
