/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.tools.msmsscore.MSMSIntensityScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LibraryExportQualityParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> minNumSignals = new OptionalParameter<>(
      new IntegerParameter("Minimum number of signals",
          "Number of signals an MS2 must contain to be exported.", 3), true);

  public static final OptionalParameter<PercentParameter> minExplainedSignals = new OptionalParameter<>(
      new PercentParameter("Minimum explained signals (%)",
          "Minimum number of explained signals in an MSMS.", 0.4d), false);

  public static final OptionalParameter<PercentParameter> minExplainedIntensity = new OptionalParameter<>(
      new PercentParameter("Minimum explained intensity (%)",
          "Minimum explained intensity in an MSMS.", 0.4d), false);

  public static final MZToleranceParameter formulaTolerance = new MZToleranceParameter(
      "Formula m/z tolerance", "m/z tolerance to assign MSMS signals to a formula.", 0.003,
      10.0d);

  public static final BooleanParameter exportExplainedSignalsOnly = new BooleanParameter(
      "Export explained signals only",
      "Only explained signals will be exported to the library spectrum.", false);

  public static final BooleanParameter exportFlistNameMatchOnly = new BooleanParameter(
      "Match compound and feature list name",
      "Only export MS/MS spectra if the feature list name contains the compound name.");

  public LibraryExportQualityParameters() {
    super(new Parameter[]{minNumSignals, minExplainedSignals, minExplainedIntensity, formulaTolerance,
        exportExplainedSignalsOnly, exportFlistNameMatchOnly});
  }

  /**
   * @param msmsScan   The msms scan to evaluate
   * @param annotation The annotation to base the evaluation on
   * @return The list of explained signals. Null if this spectrum did not match the quality
   * parameters. Empty list if formula parameters are disabled but the number of signals matched the
   * requirements.
   */
  public List<DataPoint> matchAndGetExplainedSignals(final Scan msmsScan, final FeatureAnnotation annotation,
      FeatureListRow f) {

    if (getValue(LibraryExportQualityParameters.minNumSignals)
        && msmsScan.getNumberOfDataPoints() < getParameter(
        LibraryExportQualityParameters.minNumSignals).getEmbeddedParameter().getValue()) {
      return null;
    }

    final String formula = annotation != null ? annotation.getFormula() : null;
    if (formula == null || annotation.getCompoundName() == null) {
      return null;
    }

    if (getValue(LibraryExportQualityParameters.exportFlistNameMatchOnly) && !f.getFeatureList()
        .getName().contains(annotation.getCompoundName())) {
      return null;
    }

    final MZTolerance msmsFormulaTolerance = getValue(
        LibraryExportQualityParameters.formulaTolerance);
    final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        formula, SilentChemObjectBuilder.getInstance());
    final List<DataPoint> explainedSignals = new ArrayList<>();

    try {
      FormulaUtils.replaceAllIsotopesWithoutExactMass(molecularFormula);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final IonType adductType = annotation.getAdductType();
    if (adductType.getCDKFormula() != null) {
      molecularFormula.add(adductType.getCDKFormula());
    }

    final DataPoint[] dataPoints = ScanUtils.extractDataPoints(msmsScan);

    if (getValue(LibraryExportQualityParameters.minExplainedIntensity)) {
      MSMSScore intensityFormulaScore = MSMSIntensityScoreCalculator.evaluateMSMS(
          msmsFormulaTolerance, molecularFormula, dataPoints, msmsScan.getPrecursorMz(),
          msmsScan.getPrecursorCharge(), dataPoints.length);
      if (intensityFormulaScore == null || intensityFormulaScore.getScore() < getParameter(
          LibraryExportQualityParameters.minExplainedIntensity).getEmbeddedParameter().getValue()
          .floatValue()) {
        return null;
      }
      explainedSignals.addAll(intensityFormulaScore.getAnnotation().keySet());
    }

    if (getValue(LibraryExportQualityParameters.minExplainedSignals)) {
      MSMSScore peakFormulaScore = MSMSScoreCalculator.evaluateMSMS(msmsFormulaTolerance,
          molecularFormula, dataPoints, msmsScan.getPrecursorMz(), msmsScan.getPrecursorCharge(),
          dataPoints.length);
      if (peakFormulaScore == null || peakFormulaScore.getScore() < getParameter(
          LibraryExportQualityParameters.minExplainedSignals).getEmbeddedParameter().getValue()
          .floatValue()) {
        return null;
      }
      explainedSignals.clear(); // clear if we have previous annotations, they are the same
      explainedSignals.addAll(peakFormulaScore.getAnnotation().keySet());
    }

    // double check if we still match the minimum peaks if we export explained only
    if (getValue(LibraryExportQualityParameters.minNumSignals) && getValue(
        LibraryExportQualityParameters.exportExplainedSignalsOnly)
        && explainedSignals.size() < getParameter(
        LibraryExportQualityParameters.minNumSignals).getEmbeddedParameter().getValue()) {
      return null;
    }

    return explainedSignals;
  }
}
