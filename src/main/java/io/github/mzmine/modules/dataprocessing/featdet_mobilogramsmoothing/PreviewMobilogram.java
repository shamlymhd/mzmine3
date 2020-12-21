/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.LabelTextProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.SimpleMobilogram;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PreviewMobilogram extends SimpleMobilogram implements PlotDatasetProvider {

  private final String seriesKey;
  private final Color awt;

  private final List<Double> xValues;
  private final List<Double> yValues;

  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();

  private List<MobilityDataPoint> sortedDps;


  public PreviewMobilogram(Mobilogram mobilogram, final String seriesKey) {
    super(mobilogram.getMobilityType(), mobilogram.getRawDataFile());
    this.awt = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColorAWT();
    this.seriesKey = seriesKey;
    mobilogram.getDataPoints().forEach(this::addDataPoint);
    yValues = new ArrayList<>();
    xValues = new ArrayList<>();
    calc();
  }

  @Override
  public Color getAWTColor() {
    return awt;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format(sortedDps.get(index).getMZ());
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Mobility scan #" + sortedDps.get(itemIndex).getScanNum()
        + "\nm/z " + mzFormat.format(sortedDps.get(itemIndex).getMZ())
        + "\nIntensity: " + intensityFormat.format(yValues.get(itemIndex))
        + "\nMobility: " + mobilityFormat.format(xValues.get(itemIndex)) + " " + getMobilityType()
        .getUnit();
  }

  @Override
  public void calc() {
    super.calc();

    sortedDps = getDataPoints().stream()
        .sorted(Comparator.comparingDouble(MobilityDataPoint::getMobility))
        .collect(Collectors.toList());

    for (MobilityDataPoint dp : sortedDps) {
      xValues.add(dp.getMobility());
      yValues.add(dp.getIntensity());
    }
  }

  @Override
  public List<Double> getDomainValues() {
    return xValues;
  }

  @Override
  public List<Double> getRangeValues() {
    return yValues;
  }

  @Override
  public int getValueCount() {
    return xValues.size();
  }

}
