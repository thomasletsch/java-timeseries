package timeseries;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.None;
import org.knowm.xchart.style.markers.SeriesMarkers;

import data.DataSet;
import data.DoubleFunctions;

/**
 * A sequence of observations taken at regular time intervals.
 * 
 * @author Jacob Rachiele
 *
 */
public class TimeSeries extends DataSet {

  private final TemporalUnit timeScale;
  private final int n;
  private final double mean;
  private final double[] series;
  private String name = "Time Series";
  private final List<OffsetDateTime> observationTimes;
  private final long periodLength;

  /**
   * Construct a new TimeSeries from the given data counting from year 1. Use this constructor if the dates and/or times
   * associated with the observations do not matter.
   * 
   * @param series the time series of observations.
   */
  public TimeSeries(final double... series) {
    this(OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0)), series);
  }

  /**
   * Construct a new TimeSeries object with the given parameters.
   * 
   * @param timeScale The scale of time at which observations are made (or aggregated). Time series observations are
   *        commonly made (or aggregated) on a yearly, monthly, weekly, daily, hourly, etc... basis.
   * @param startTime The time at which the first observation was made. Usually a rough approximation.
   * @param periodLength The length of time between observations measured in the units given by the
   *        <code>timeScale</code> argument. For example, quarterly data could be provided with a timeScale of
   *        {@link ChronoUnit#MONTHS} and a periodLength of 3.
   * @param series The data constituting this TimeSeries.
   */
  public TimeSeries(final TemporalUnit timeScale, final OffsetDateTime startTime, final long periodLength,
      final double... series) {
    super(series);
    this.series = series.clone();
    this.n = series.length;
    this.mean = super.mean();
    super.setName(this.name);
    this.timeScale = timeScale;
    this.periodLength = periodLength;
    this.observationTimes = new ArrayList<>(series.length);
    observationTimes.add(startTime);
    for (int i = 1; i < series.length; i++) {
      observationTimes.add(observationTimes.get(i - 1).plus(periodLength, timeScale));
    }
  }

  /**
   * Construct a new TimeSeries from the given data with the supplied start time.
   * 
   * @param startTime the time of the first observation.
   * @param series the observations.
   */
  /* package */ TimeSeries(final OffsetDateTime startTime, final double... series) {
    this(ChronoUnit.MONTHS, startTime, 1L, series);
  }

  private TimeSeries(final TemporalUnit timeScale, final List<OffsetDateTime> observationTimes, final long periodLength,
      final double... series) {
    super(series);
    this.series = series.clone();
    this.n = series.length;
    this.mean = super.mean();
    super.setName(this.name);
    this.timeScale = timeScale;
    this.periodLength = periodLength;
    this.observationTimes = new ArrayList<>(observationTimes);
  }

  private TimeSeries(final TimeSeries original) {
    super(original);
    this.mean = original.mean;
    this.n = original.n;
    this.name = original.name;
    // Note OffsetDateTime is immutable.
    this.observationTimes = new ArrayList<>(original.observationTimes);
    this.periodLength = original.periodLength;
    this.series = original.series.clone();
    this.timeScale = original.timeScale;
  }

  public final TimeSeries aggregate() {
    return null;
  }

  public final TimeSeries aggregate(final TemporalUnit time) {
    return aggregate(time, 1);
  }

  public final TimeSeries aggregate(final TemporalUnit time, final int periodLength) {
    final int period = (int) ((time.getDuration().getSeconds() * periodLength)
        / (timeScale.getDuration().getSeconds() * this.periodLength));
    final List<OffsetDateTime> obsTimes = new ArrayList<>();
    double[] aggregated = new double[series.length / period];
    double sum = 0.0;
    for (int i = 0; i < aggregated.length; i++) {
      sum = 0.0;
      for (int j = 0; j < period; j++) {
        sum += series[j + period * i];
      }
      aggregated[i] = sum;
      obsTimes.add(this.observationTimes.get(i * period));
    }
    TimeSeries series = new TimeSeries(time, obsTimes, periodLength, aggregated);
    series.setName("Aggregated " + this.name);
    return series;
  }

  /**
   * Return the value of the time series at the given index.
   * 
   * @param index the index of the value to return.
   * @return the value of the time series at the given index.
   */
  public final double at(final int index) {
    return this.series[index];
  }

  /**
   * The correlation of this series with itself at lag k.
   * 
   * @param k the lag to compute the autocorrelation at.
   * @return the correlation of this series with itself at lag k.
   */
  public final double autoCorrelationAtLag(final int k) {
    final double variance = autoCovarianceAtLag(0);
    return autoCovarianceAtLag(k) / variance;
  }

  /**
   * Every correlation coefficient of this series with itself up to the given lag.
   * 
   * @param k the maximum lag to compute the autocorrelation at.
   * @return every correlation coefficient of this series with itself up to the given lag.
   */
  public final double[] autoCorrelationUpToLag(final int k) {
    final double[] autoCorrelation = new double[Math.min(k + 1, n)];
    for (int i = 0; i < Math.min(k + 1, n); i++) {
      autoCorrelation[i] = autoCorrelationAtLag(i);
    }
    return autoCorrelation;
  }

  /**
   * The covariance of this series with itself at lag k.
   * 
   * @param k the lag to compute the autocovariance at.
   * @return the covariance of this series with itself at lag k.
   */
  public final double autoCovarianceAtLag(final int k) {
    double sumOfProductOfDeviations = 0.0;
    for (int t = 0; t < n - k; t++) {
      sumOfProductOfDeviations += (series[t] - mean) * (series[t + k] - mean);
    }
    return sumOfProductOfDeviations / n;
  }

  /**
   * Every covariance measure of this series with itself up to the given lag.
   * 
   * @param k the maximum lag to compute the autocovariance at.
   * @return every covariance measure of this series with itself up to the given lag.
   */
  public final double[] autoCovarianceUpToLag(final int k) {
    final double[] acv = new double[Math.min(k + 1, n)];
    for (int i = 0; i < Math.min(k + 1, n); i++) {
      acv[i] = autoCovarianceAtLag(i);
    }
    return acv;
  }

  /**
   * Perform the inverse of the Box-Cox transformation on this series and return the result in a new TimeSeries.
   * 
   * @param boxCoxLambda the Box-Cox transformation parameter to use for the inversion.
   * @return a new TimeSeries with the inverse Box-Cox transformation applied.
   */
  public final TimeSeries backTransform(final double boxCoxLambda) {
    if (boxCoxLambda > 2 || boxCoxLambda < -1) {
      throw new IllegalArgumentException("The BoxCox parameter must lie between"
          + " -1 and 2, but the provided parameter was equal to " + boxCoxLambda);
    }
    final double[] invBoxCoxed = DoubleFunctions.inverseBoxCox(this.series, boxCoxLambda);
    return new TimeSeries(this.timeScale, this.observationTimes, this.periodLength, invBoxCoxed);
  }

  public final TimeSeries centeredMovingAverage(final int m) {
    if (m % 2 == 1)
      return movingAverage(m);
    TimeSeries firstAverage = movingAverage(m);
    final int k = m / 2;
    final List<OffsetDateTime> times = this.observationTimes.subList(k, n - k);
    double[] secondAverage = firstAverage.movingAverage(2).series;
    return new TimeSeries(this.timeScale, times, this.periodLength, secondAverage);
  }

  /**
   * Return a new deep copy of this TimeSeries.
   */
  public final TimeSeries copy() {
    return new TimeSeries(this);
  }
  
  public final TimeSeries difference(final int lag, final int differences) {
    TimeSeries diffed = difference(lag);
    for (int i = 1; i < differences; i++) {
      diffed = diffed.difference(lag);
    }
    return diffed;
  }
  
  public final TimeSeries difference(final int lag) {
    double[] diffed = differenceArray(lag);
    final List<OffsetDateTime> obsTimes = this.observationTimes.subList(lag, n);
    return new TimeSeries(this.timeScale, obsTimes, this.periodLength, diffed);
  }
  
  public final TimeSeries difference() {
    return difference(1);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TimeSeries other = (TimeSeries) obj;
    if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean)) {
      return false;
    }
    if (n != other.n) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (observationTimes == null) {
      if (other.observationTimes != null) {
        return false;
      }
    } else if (!observationTimes.equals(other.observationTimes)) {
      return false;
    }
    if (periodLength != other.periodLength) {
      return false;
    }
    if (!Arrays.equals(series, other.series)) {
      return false;
    }
    if (timeScale == null) {
      if (other.timeScale != null) {
        return false;
      }
    } else if (!timeScale.equals(other.timeScale)) {
      return false;
    }
    return true;
  }

  @Override
  public final String getName() {
    return this.name;
  }

  // ********** Plots ********** //

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(mean);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + n;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((observationTimes == null) ? 0 : observationTimes.hashCode());
    result = prime * result + (int) (periodLength ^ (periodLength >>> 32));
    result = prime * result + Arrays.hashCode(series);
    result = prime * result + ((timeScale == null) ? 0 : timeScale.hashCode());
    return result;
  }

  /**
   * Compute a moving average of order m.
   * 
   * @param m the order of the moving average. The number of neighboring values to use for averaging.
   * @return a new TimeSeries with the smoothed data.
   */
  public final TimeSeries movingAverage(final int m) {
    final int c = m % 2;
    final int k = (m - c) / 2;
    final double[] average;
    average = new double[this.n - m + 1];
    double sum;
    for (int t = 0; t < average.length; t++) {
      sum = 0;
      for (int j = -k; j < k + c; j++) {
        sum += series[t + k + j];
      }
      average[t] = sum / m;
    }
    final List<OffsetDateTime> times = this.observationTimes.subList(k + c - 1, n - k);
    return new TimeSeries(this.timeScale, times, this.periodLength, average);
  }

  public final List<OffsetDateTime> observationTimes() {
    return this.observationTimes;
  }

  public final long periodLength() {
    return this.periodLength;
  }

  // ********** Plots ********** //

  /**
   * Display a line plot connecting the observation times to the observation values.
   */
  @Override
  public final void plot() {

    new Thread(() -> {
      final List<Date> xAxis = new ArrayList<>(this.observationTimes.size());
      for (OffsetDateTime dateTime : this.observationTimes) {
        xAxis.add(Date.from(dateTime.toInstant()));
      }
      final List<Double> seriesList = com.google.common.primitives.Doubles.asList(this.series);
      for (int t = 0; t < seriesList.size(); t++) {
        if (seriesList.get(t).isInfinite()) {
          seriesList.set(t, Double.NaN);
        }
      }
      final XYChart chart = new XYChartBuilder().theme(ChartTheme.XChart).height(480).width(960).title(this.name).build();
      final XYSeries xySeries = chart.addSeries(this.name, xAxis, seriesList)
          .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);
      xySeries.setLineWidth(0.75f);
      xySeries.setMarker(new None()).setLineColor(Color.BLUE);
      final JPanel panel = new XChartPanel<>(chart);
      final JFrame frame = new JFrame(this.name);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setVisible(true);
    }).run();

  }

  /**
   * Display a plot of the sample autocorrelations up to the given lag.
   * 
   * @param k the maximum lag to include in the acf plot.
   */
  public final void plotAcf(final int k) {

    final double[] acf = autoCorrelationUpToLag(k);
    final double[] lags = new double[k + 1];
    for (int i = 1; i < lags.length; i++) {
      lags[i] = i;
    }
    final double upper = (-1 / series.length) + (2 / Math.sqrt(series.length));
    final double lower = (-1 / series.length) - (2 / Math.sqrt(series.length));
    final double[] upperLine = new double[lags.length];
    final double[] lowerLine = new double[lags.length];
    for (int i = 0; i < lags.length; i++) {
      upperLine[i] = upper;
    }
    for (int i = 0; i < lags.length; i++) {
      lowerLine[i] = lower;
    }

    new Thread(() -> {
      XYChart chart = new XYChartBuilder().theme(ChartTheme.GGPlot2).height(800).width(1200)
          .title("Autocorrelations By Lag").build();
      XYSeries series = chart.addSeries("Autocorrelation", lags, acf);
      XYSeries series2 = chart.addSeries("Upper Bound", lags, upperLine);
      XYSeries series3 = chart.addSeries("Lower Bound", lags, lowerLine);
      chart.getStyler().setChartFontColor(Color.BLACK)
          .setSeriesColors(new Color[] { Color.BLACK, Color.BLUE, Color.BLUE });

      series.setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
      series2.setXYSeriesRenderStyle(XYSeriesRenderStyle.Line).setMarker(SeriesMarkers.NONE)
          .setLineStyle(SeriesLines.DASH_DASH);
      series3.setXYSeriesRenderStyle(XYSeriesRenderStyle.Line).setMarker(SeriesMarkers.NONE)
          .setLineStyle(SeriesLines.DASH_DASH);
      JPanel panel = new XChartPanel<>(chart);
      JFrame frame = new JFrame("Autocorrelation by Lag");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setVisible(true);
    }).run();

  }

  public final void print() {
    System.out.println(this.toString());
  }

  /**
   * The time series of observations.
   * 
   * @return the time series of observations.
   */
  public final double[] series() {
    return this.series.clone();
  }

  @Override
  public final void setName(final String newName) {
    this.name = newName;
    super.setName(newName);
  }

  /**
   * Return a slice of this time series from start (inclusive) to end (exclusive).
   * 
   * @param start the beginning index of the slice. The value at the index is included in the returned TimeSeries.
   * @param end the ending index of the slice. The value at the index is <i>not</i> included in the returned TimeSeries.
   * @return a slice of this time series from start (inclusive) to end (exclusive).
   */
  public final TimeSeries slice(final int start, final int end) {
    final double[] sliced = new double[end - start + 1];
    System.arraycopy(series, start, sliced, 0, end - start + 1);
    final List<OffsetDateTime> obsTimes = new ArrayList<>(this.observationTimes.subList(start, end + 1));
    return new TimeSeries(this.timeScale, obsTimes, this.periodLength, sliced);
  }
  
  public final TimeSeries timeSlice(final int start, final int end) {
    final double[] sliced = new double[end - start + 1];
    System.arraycopy(series, start - 1, sliced, 0, end - start + 1);
    final List<OffsetDateTime> obsTimes = new ArrayList<>(this.observationTimes.subList(start - 1, end));
    return new TimeSeries(this.timeScale, obsTimes, this.periodLength, sliced);
  }

  public final TemporalUnit timeScale() {
    return this.timeScale;
  }

  @Override
  public String toString() {
    DateTimeFormatter dateFormatter;
    switch ((ChronoUnit) timeScale) {
    case HOURS:
    case MINUTES:
    case SECONDS:
    case MILLIS:
    case MICROS:
    case NANOS:
      dateFormatter = DateTimeFormatter.ISO_TIME;
      break;
    default:
      dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    NumberFormat numFormatter = new DecimalFormat("#0.0000");
    StringBuilder builder = new StringBuilder();
    builder.append("n: ").append(n).append("\nmean: ").append(numFormatter.format(mean)).append("\nseries: ");
    if (series.length > 6) {
      for (double d : DoubleFunctions.slice(series, 0, 3)) {
        builder.append(numFormatter.format(d)).append(", ");
      }
      builder.append("..., ");
      for (double d : DoubleFunctions.slice(series, n - 3, n - 1)) {
        builder.append(numFormatter.format(d)).append(", ");
      }
      builder.append(numFormatter.format(series[n - 1]));
    } else {
      for (int i = 0; i < series.length - 1; i++) {
        builder.append(numFormatter.format(series[i])).append(", ");
      }
      builder.append(numFormatter.format(series[n - 1]));
    }
    builder.append("\nname: ").append(name).append("\nobservationTimes: ");
    if (series.length > 6) {
      for (OffsetDateTime date : observationTimes.subList(0, 3)) {
        builder.append(date.format(dateFormatter)).append(", ");
      }
      builder.append("..., ");
      for (OffsetDateTime date : observationTimes.subList(n - 3, n - 1)) {
        builder.append(date.format(dateFormatter)).append(", ");
      }
      builder.append(observationTimes.get(n - 1).format(dateFormatter));
    } else {
      for (int i = 0; i < observationTimes.size() - 1; i++) {
        builder.append(observationTimes.get(i).format(dateFormatter)).append(", ");
      }
      builder.append(observationTimes.get(n - 1).format(dateFormatter));
    }
    builder.append("\nperiodLength: ").append(periodLength).append(" " + timeScale).append("\ntimeScale: ")
        .append(timeScale);
    return builder.toString();
  }

  /**
   * Transform the series using a Box-Cox transformation with the given parameter value. Setting boxCoxLambda equal to 0
   * corresponds to the natural logarithm while values other than 0 correspond to power transforms. See the definition
   * given
   * <a target=_blank href="https://en.wikipedia.org/wiki/Power_transform#Box.E2.80.93Cox_transformation">here.</a>
   * 
   * @param boxCoxLambda ahe parameter to use for the transformation.
   * @return a new TimeSeries transformed using the given Box-Cox parameter.
   * @throws IllegalArgumentException if boxCoxLambda is not strictly between -1 and 2.
   */
  public final TimeSeries transform(final double boxCoxLambda) {
    if (boxCoxLambda > 2 || boxCoxLambda < -1) {
      throw new IllegalArgumentException("The BoxCox parameter must lie between"
          + " -1 and 2, but the provided parameter was equal to " + boxCoxLambda);
    }
    final double[] boxCoxed = DoubleFunctions.boxCox(this.series, boxCoxLambda);
    final TimeSeries transformed = new TimeSeries(this.timeScale, this.observationTimes, this.periodLength, boxCoxed);
    transformed.setName(this.name + "\nTransformed (Lambda = " + boxCoxLambda + ")");
    return transformed;
  }
  
  private final double[] differenceArray(final int lag) {
    double[] differenced = new double[series.length - lag];
    for (int i = 0; i < differenced.length; i++) {
      differenced[i] = series[i + lag] - series[i];
    }
    return differenced;
  }

}
