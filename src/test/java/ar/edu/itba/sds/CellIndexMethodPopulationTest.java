package ar.edu.itba.sds;

import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CellIndexMethodPopulationTest {

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for invalid grid parameters")
    void testConstructorValidation() {
        SizedParticle p = new SizedParticle(2.0f, 2.0f, 0.5f);

        // m < 1
        assertThrows(IllegalArgumentException.class, () -> new CellIndexService<>(0, 10, 1, List.of(p)));
        // l <= 0
        assertThrows(IllegalArgumentException.class, () -> new CellIndexService<>(5, 0, 1, List.of(p)));
        // cellSize (10 / 5 = 2.0) < rc (3.0)
        assertThrows(IllegalArgumentException.class, () -> new CellIndexService<>(5, 10, 3, List.of(p)));
    }

    @Test
    @DisplayName("Single particle fully contained within one cell populates maps correctly")
    void testSingleParticleInSingleCell() {
        // Matrix 2x2 (m=2), L=10 -> cellSize = 5.0
        // Particle at (2.5, 2.5) with r=1.0 -> bounds [1.5, 3.5] x [1.5, 3.5]
        // Fits entirely inside row 0, col 0 -> cellId = 1
        SizedParticle particle = new SizedParticle(2.5f, 2.5f, 1.0f);

        CellIndexService<SizedParticle> service = new CellIndexService<>(2, 10, 1, List.of(particle));

        // Verify cellsWithParticle
        assertTrue(service.cellsWithParticle.containsKey(particle));
        assertEquals(List.of(1L), service.cellsWithParticle.get(particle));

        // Verify particlesInCells
        assertTrue(service.particlesInCells.containsKey(1L));
        assertEquals(1, service.particlesInCells.get(1L).size());
        assertEquals(particle, service.particlesInCells.get(1L).getFirst());
    }

    @Test
    @DisplayName("Particle overlapping the intersection of 4 cells populates all 4 cells")
    void testParticleSpanningFourCells() {
        // Matrix 2x2 (m=2), L=10 -> cellSize = 5.0
        // Particle centered exactly at intersection (5.0, 5.0) with r=1.0
        // Circle overlaps cells 1, 2, 3, 4
        SizedParticle particle = new SizedParticle(5.0f, 5.0f, 1.0f);

        CellIndexService<SizedParticle> service = new CellIndexService<>(2, 10, 1, List.of(particle));

        List<Long> occupiedCells = service.cellsWithParticle.get(particle);
        assertNotNull(occupiedCells);
        assertEquals(4, occupiedCells.size());
        assertTrue(occupiedCells.containsAll(List.of(1L, 2L, 3L, 4L)));

        for (long cellId = 1; cellId <= 4; cellId++) {
            assertTrue(service.particlesInCells.containsKey(cellId));
            assertTrue(service.particlesInCells.get(cellId).contains(particle));
        }
    }

    @Test
    @DisplayName("Geometric filtering: excludes cell when bounding box touches but circular area does not")
    void testBoundingBoxVsCircleIntersection() {
        // Matrix 2x2 (m=2), L=10 -> cellSize = 5.0
        // Cell 1: [0,5]x[0,5], Cell 2: [5,10]x[0,5], Cell 3: [0,5]x[5,10], Cell 4: [5,10]x[5,10]
        // Particle at (3.0, 3.0) with r=2.0 -> bounds [1.0, 5.0] x [1.0, 5.0]
        // Bounding box candidate range includes Cell 4 (via border point 5.0, 5.0),
        // but distance squared from (3,3) to Cell 4's corner (5,5) is (2^2 + 2^2) = 8 > r^2 (4).
        SizedParticle particle = new SizedParticle(3.0f, 3.0f, 2.0f);

        CellIndexService<SizedParticle> service = new CellIndexService<>(2, 10, 1, List.of(particle));

        List<Long> occupiedCells = service.cellsWithParticle.get(particle);
        assertNotNull(occupiedCells);

        // Should occupy Cells 1, 2, and 3, but NOT Cell 4
        assertEquals(3, occupiedCells.size());
        assertTrue(occupiedCells.containsAll(List.of(1L, 2L, 3L)));
        assertFalse(occupiedCells.contains(4L));
        assertFalse(service.particlesInCells.getOrDefault(4L, List.of()).contains(particle));
    }

    @Test
    @DisplayName("Multiple particles occupying the same cell are grouped together")
    void testMultipleParticlesInSameCell() {
        // Matrix 3x3 (m=3), L=9 -> cellSize = 3.0
        // Cell 1 is row 0, col 0 ([0,3]x[0,3])
        SizedParticle p1 = new SizedParticle(1.0f, 1.0f, 0.5f);
        SizedParticle p2 = new SizedParticle(2.0f, 2.0f, 0.4f);

        CellIndexService<SizedParticle> service = new CellIndexService<>(3, 9, 1, List.of(p1, p2));

        assertEquals(List.of(1L), service.cellsWithParticle.get(p1));
        assertEquals(List.of(1L), service.cellsWithParticle.get(p2));

        List<SizedParticle> cell1Particles = service.particlesInCells.get(1L);
        assertNotNull(cell1Particles);
        assertEquals(2, cell1Particles.size());
        assertTrue(cell1Particles.containsAll(List.of(p1, p2)));
    }

    @Test
    @DisplayName("Boundary condition at max domain boundary (x = L, y = L)")
    void testBoundaryConditionAtDomainMax() {
        // Matrix 2x2 (m=2), L=10 -> cellSize = 5.0
        // Particle touching outer boundary at (9.0, 9.0) with r=1.0 -> maxX=10.0, maxY=10.0
        // Should be clamped to max row/col (Cell 4) without IndexOutOfBounds or overflow
        SizedParticle particle = new SizedParticle(9.0f, 9.0f, 1.0f);

        CellIndexService<SizedParticle> service = new CellIndexService<>(2, 10, 1, List.of(particle));

        List<Long> occupied = service.cellsWithParticle.get(particle);
        assertEquals(List.of(4L), occupied);
        assertEquals(1, service.particlesInCells.get(4L).size());
        assertEquals(particle, service.particlesInCells.get(4L).getFirst());
    }
}