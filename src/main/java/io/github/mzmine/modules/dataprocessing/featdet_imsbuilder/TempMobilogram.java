package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class TempMobilogram {

  private static Logger logger = Logger.getLogger(TempMobilogram.class.getName());

  protected final TreeMap<Integer, RetentionTimeMobilityDataPoint> datapoints = new TreeMap<>();
  protected double lowestMz = Double.MAX_VALUE;
  protected double highestMz = Double.MIN_VALUE;
  protected double centerMz;

  public TempMobilogram() {

  }


  /**
   * Adds a data point if no data point of the same scan number is contained in this trace.
   *
   * @param dp
   * @return
   */
  public RetentionTimeMobilityDataPoint tryToAddDataPoint(RetentionTimeMobilityDataPoint dp) {
    var currentValue = datapoints.putIfAbsent(dp.getMobilityScan().getMobilityScanNumber(), dp);
    if (currentValue == null) {
      updateValues();
    }
    return currentValue;
  }

  /**
   * @param dp
   * @return The replaced data point
   */
  public RetentionTimeMobilityDataPoint replaceDataPoint(RetentionTimeMobilityDataPoint dp) {

    final RetentionTimeMobilityDataPoint replaced = datapoints
        .put(dp.getMobilityScan().getMobilityScanNumber(), dp);
    if (replaced == null) {
      logger.fine(() -> "Data point did not replace another data point");
    }
    updateValues();
    return replaced;
  }

  public double getLowestMz() {
    return lowestMz;
  }

  public void setLowestMz(double lowestMz) {
    this.lowestMz = lowestMz;
  }

  public double getHighestMz() {
    return highestMz;
  }

  public void setHighestMz(double highestMz) {
    this.highestMz = highestMz;
  }

  private void updateValues() {
    centerMz = 0d;
    double summedIntensities = 0d;
    for (RetentionTimeMobilityDataPoint value : datapoints.values()) {
      final double intensity = value.getIntensity();
      final double mz = value.getMZ();

      if (mz > highestMz) {
        highestMz = mz;
      }
      if (mz < lowestMz) {
        lowestMz = mz;
      }

      centerMz += mz * intensity;
      summedIntensities += intensity;
    }
    centerMz /= summedIntensities;
  }

  public RetentionTimeMobilityDataPoint keepBetterFittingDataPoint(
      RetentionTimeMobilityDataPoint dp) {
    final RetentionTimeMobilityDataPoint current = tryToAddDataPoint(dp);
    if (current == null) {
      return null;
    }

    final double currentDelta = Math.abs(centerMz - current.getMZ());
    final double proposedDelta = Math.abs(centerMz - dp.getMZ());
    if (currentDelta > proposedDelta) {
//      var ceilingEntry = datapoints.ceilingEntry(dp.getMobilityScan().getMobilityScanNumber());
//      var floorEntry = datapoints.floorEntry(dp.getMobilityScan().getMobilityScanNumber());
//      final double ceilingIntensity = ceilingEntry.getValue().getIntensity();
//      final double floorIntensity = floorEntry.getValue().getIntensity();

      return replaceDataPoint(dp);
    }
    return dp;
  }

  public BuildingIonMobilitySeries toBuildingSeries(@Nullable MemoryMapStorage storage) {
    final int numValues = datapoints.size();
    double[] mzs = new double[numValues];
    double[] intensities = new double[numValues];
    List<MobilityScan> scans = new ArrayList<>();

    int i = 0;
    for (RetentionTimeMobilityDataPoint value : datapoints.values()) {
      mzs[i] = value.getMZ();
      intensities[i] = value.getIntensity();
      scans.add(value.getMobilityScan());
      i++;
    }
    return new BuildingIonMobilitySeries(storage, mzs, intensities, scans);
  }
}
