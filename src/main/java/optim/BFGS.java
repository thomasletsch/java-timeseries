/*
 * Copyright (c) 2016 Jacob Rachiele
 * 
 */
package optim;

import linear.doubles.Matrices;
import linear.doubles.Matrix;
import linear.doubles.Vector;
import optim.doubles.QuasiNewtonLineFunction;
import optim.doubles.StrongWolfeLineSearch;

/**
 * An implementation of the Broyden-Fletcher-Goldfarb-Shanno (BFGS) algorithm for unconstrained
 * nonlinear optimization.
 * @author Jacob Rachiele
 *
 */
public final class BFGS {

  private static final double c1 = 1E-4;
  private static final double c2 = 0.9;

  private final AbstractMultivariateFunction f;
  private Vector iterate;
  private Vector nextIterate;
  private Vector gradient;
  private Vector nextGradient;
  private Vector searchDirection;
  private double functionValue = 0.0;
  private double stepSize = 0.0;
  private double rho = 0.0;
  private Vector s;
  private Vector y;
  private Matrix H;
  private final Matrix identity;
  
  /**
   * Create a new BFGS object with the given information. The identity matrix will be used for
   * initial inverse Hessian approximation.
   * @param f the function to be minimized.
   * @param startingPoint the initial guess of the minimum.
   * @param gradientTolerance the tolerance for the norm of the gradient of the function.
   * @param functionChangeTolerance the tolerance for the change in function value.
   */
  public BFGS(final AbstractMultivariateFunction f, final Vector startingPoint, final double gradientTolerance,
          final double functionChangeTolerance) {
    this(f, startingPoint, gradientTolerance, functionChangeTolerance, Matrices.identity(startingPoint.size()));
  }

  /**
   * Create a new BFGS object with the given information.
   * @param f the function to be minimized.
   * @param startingPoint the initial guess of the minimum.
   * @param gradientTolerance the tolerance for the norm of the gradient of the function.
   * @param functionChangeTolerance the tolerance for the change in function value.
   * @param initialHessian The initial guess for the inverse Hessian approximation.
   */
  public BFGS(final AbstractMultivariateFunction f, final Vector startingPoint, final double gradientTolerance,
          final double functionChangeTolerance, final Matrix initialHessian) {
    this.f = f;
    this.identity = Matrices.identity(startingPoint.size());
    this.H = initialHessian;
    this.iterate = startingPoint;
    int k = 0;
    double priorFunctionValue = Double.POSITIVE_INFINITY;
    functionValue = f.at(startingPoint);
    double absoluteChange = Double.MAX_VALUE;
    double relativeChange = Double.MAX_VALUE;
    gradient = f.gradientAt(startingPoint, functionValue);
    if (startingPoint.size() > 0) {
      while (gradient.norm() > gradientTolerance && absoluteChange > functionChangeTolerance
              && relativeChange > functionChangeTolerance) {
        searchDirection = (H.times(gradient).scaledBy(-1.0));
        stepSize = updateStepSize(k, functionValue);
        nextIterate = iterate.plus(searchDirection.scaledBy(stepSize));
        s = nextIterate.minus(iterate);
        priorFunctionValue = functionValue;
        functionValue = f.at(nextIterate);
        absoluteChange = Math.abs(priorFunctionValue - functionValue);
        relativeChange = Math.abs((priorFunctionValue - functionValue) / priorFunctionValue);
        nextGradient = f.gradientAt(nextIterate, functionValue);
        y = nextGradient.minus(gradient);
        rho = 1 / y.dotProduct(s);
        H = updateHessian(k);
        iterate = nextIterate;
        gradient = nextGradient;
        k += 1;
      }
    }
  }

  private final double updateStepSize(final int k, final double functionValue) {
    final double slope0 = gradient.dotProduct(searchDirection);
    final QuasiNewtonLineFunction lineFunction = new QuasiNewtonLineFunction(this.f, iterate, searchDirection);
    StrongWolfeLineSearch lineSearch = StrongWolfeLineSearch.newBuilder(lineFunction, functionValue, slope0).c1(c1)
            .c2(c2).alphaMax(100).alpha0(1.0).build();
    return lineSearch.search();
  }

  private final Matrix updateHessian(final int k) {
    Matrix a = identity.minus(s.outerProduct(y).scaledBy(rho));
    Matrix b = identity.minus(y.outerProduct(s).scaledBy(rho));
    Matrix c = s.outerProduct(s).scaledBy(rho);
    return a.times(H).times(b).plus(c);
  }

  /**
   * Return the final value of the target function.
   * @return the final value of the target function.
   */
  public final double functionValue() {
    return this.functionValue;
  }

  /**
   * Return the final, optimized input parameters.
   * @return the final, optimized input parameters.
   */
  public final Vector parameters() {
    return this.iterate;
  }

  /**
   * Return the final approximation to the inverse Hessian.
   * @return the final approximation to the inverse Hessian.
   */
  public final Matrix inverseHessian() {
    return this.H;
  }

}
