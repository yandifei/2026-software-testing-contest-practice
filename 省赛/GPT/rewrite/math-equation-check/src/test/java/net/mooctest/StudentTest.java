package net.mooctest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StudentTest {
    @Test
    void numericalSolversFindTheIntersectionOfAnAffineFunction() throws Exception {
        var affine = new net.mooctest.expression.engine.ManyToOneNumberFunction("affine") {
            @Override public double run(double... arguments) {
                return 4 * arguments[0] - 7;
            }
        };
        var solver = new net.mooctest.expression.engine.EquationSolver(
                "4*x-7", 13, "RewriteAffine", affine) {};
        solver.setNewtonInitialX(-8);
        solver.setMaxIter(15);
        solver.setTol(1e-9);
        assertEquals(5.0, solver.calculation(false).getResult(), 1e-8);
        assertEquals(13, solver.getResult());
        assertEquals(-8, solver.getNewtonInitialX());
        assertEquals(15, solver.getMaxIter());
        assertEquals(1e-9, solver.getTol());
        assertThrows(ArithmeticException.class, () -> solver.solveNewton(1e-9, 1));
        solver.setBisectionLeft(-3);
        solver.setBisectionRight(13);
        solver.setUseNewton(false);
        assertEquals(-3, solver.getBisectionLeft());
        assertEquals(13, solver.getBisectionRight());
        assertFalse(solver.isUseNewton());
        assertEquals(5, solver.calculation(false).getResult());
        var bisected = solver.solveBisection(1e-9, 4);
        assertEquals(5, bisected[0]);
        assertEquals(1, bisected[1]);
        solver.setBisectionLeft(6);
        assertThrows(IllegalArgumentException.class, () -> solver.solveBisection(1e-9, 15));
        assertFalse(solver.isBigDecimal());
        assertTrue(solver.isUnBigDecimal());
        assertThrows(UnsupportedOperationException.class, solver::convertToMultiPrecisionSupported);
        assertThrows(UnsupportedOperationException.class, () -> solver.calculationBigDecimals(true));
    }

}
