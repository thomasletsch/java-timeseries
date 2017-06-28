package timeseries.models.arima;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertArrayEquals;

import timeseries.TestData;
import timeseries.operators.LagPolynomial;

public class KalmanFilterSpec {

  @Test
  public void whenStateSpaceArmaInitializedThenDataSetProperly() throws Exception {
    LagPolynomial arPoly = LagPolynomial.autoRegressive(0.3114114);
    LagPolynomial diffPoly = LagPolynomial.firstDifference();
    LagPolynomial arDiff = arPoly.times(diffPoly);
    double[] ar = arDiff.inverseParams();
    double[] ma = {-0.8373430,  0.0,  0.0, 0.3854193, -0.3227282};
    double[] y = TestData.ukcars.asArray();
    StateSpaceARMA ss = new StateSpaceARMA(y, ar, ma);
    assertThat(ss.stateEffectsVector(), is(new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}));
  }

  /*
  printArima <- function(phi = numeric(), theta = numeric()) {
    model <- makeARIMA(phi = phi, theta = theta, Delta = numeric())
    cat(paste(round(model$Pn[lower.tri(model$Pn, diag = TRUE)], 6), collapse = ", "))
  }
  */
  @Test
  public void whenManyMoreARThanMACoeffsThenStarmaOutputCorrect() {
    double[] phi = {0.5, 0.2, -0.3, 0.1};
    double[] theta = {0.7};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    double[] expected = {3.201238, 0.854377, -0.610805, 0.2455, 0.546552, -0.03122, 0.002944,
        0.172824, -0.071487, 0.032012};
    assertArrayEquals(expected, P, 1E-6);
  }

  @Test
  public void whenManyMoreMAThanARCoeffsThenStarmaOutputCorrect() {
    double[] theta = {0.5, 0.2, -0.3, 0.1};
    double[] phi = {0.7};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    // makeARIMA(phi = c(0.5, 0.2, -0.3, 0.1), theta = c(0.7), Delta = numeric())$Pn
    double[] expected = {4.017882, 0.4708, -0.056, -0.18, 0.1, 0.39, 0.01, -0.13, 0.05, 0.14,
        -0.09, 0.02, 0.1, -0.03, 0.01};
    assertArrayEquals(expected, P, 1E-6);
  }

  @Test
  public void whenMoreARThanMACoeffsThenStarmaOutputCorrect() {
    double[] phi = {0.5, 0.2};
    double[] theta = {0.7};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    // makeARIMA(phi = c(0.5, 0.2, -0.3, 0.1), theta = c(0.7), Delta = numeric())$Pn
    double[] expected = {4.042735, 1.380342, 0.651709};
    assertArrayEquals(expected, P, 1E-6);
  }

  @Test
  public void whenMoreMAThanARCoeffsThenStarmaOutputCorrect() {
    double[] theta = {0.5, 0.2};
    double[] phi = {0.7};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    // makeARIMA(phi = c(0.5, 0.2, -0.3, 0.1), theta = c(0.7), Delta = numeric())$Pn
    double[] expected = {4.560784, 0.74, 0.2, 0.29, 0.1, 0.04};
    assertArrayEquals(expected, P, 1E-6);
    double[] full = ArmaKalmanFilter.unpack(P);
  }

  @Test
  public void whenNumberOfMAAndARCoeffsEqualThenStarmaOutputCorrect() {
    double[] phi = {0.5, 0.2};
    double[] theta = {0.7, -0.3};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    // makeARIMA(phi = c(0.5, 0.2, -0.3, 0.1), theta = c(0.7), Delta = numeric())$Pn
    double[] expected = {3.222222, 0.827778, -0.3, 0.588889, -0.21, 0.09};
    assertArrayEquals(expected, P, 1E-6);
  }

  @Test
  public void whenOnlyOneARCoeffThenStarmaOutputCorrect() {
    double[] phi = {0.3};
    double[] theta = {};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    double[] expected = {1.098901};
    assertArrayEquals(expected, P, 1E-6);
  }
  
  @Test
  public void whenOnlyOneMACoeffThenStarmaOutputCorrect() {
    double[] phi = {};
    double[] theta = {0.3};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    double[] expected = {1.09, 0.3, 0.09};
    assertArrayEquals(expected, P, 1E-6);
  }

  @Test
  public void whenNoCoeffsThenStarmaOutputTheNumberOne() {
    double[] phi = {};
    double[] theta = {};
    double[] P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
    double[] expected = {1.0};
    assertArrayEquals(expected, P, 1E-6);

//    phi = new double[] {-0.12721, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.197255, -0.025093};
//    theta = new double[] {-0.645583, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.407286, 0.262937};
//    P = ArmaKalmanFilter.getInitialStateCovariance(phi, theta);
//    double[] Pnew = ArmaKalmanFilter.unpack(P);
//    System.out.println(Arrays.toString(P));
  }
}
