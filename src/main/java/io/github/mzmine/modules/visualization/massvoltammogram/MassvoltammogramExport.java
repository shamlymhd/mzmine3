package io.github.mzmine.modules.visualization.massvoltammogram;

import io.github.mzmine.util.javafx.FxThreadUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FilenameUtils;

public class MassvoltammogramExport {

  public static void exportPlot(ExtendedPlot3DPanel plot) {

    //Initializing a file chooser and a file to save the selected path to.
    final FileChooser fileChooser = new FileChooser();
    final AtomicReference<File> file = new AtomicReference<>(null);
    final FileChooser.ExtensionFilter extensionFilterPNG = new ExtensionFilter(
        "Portable Network Graphics", ".png");
    final FileChooser.ExtensionFilter extensionFilterCSV = new ExtensionFilter("XY-File", ".xy");
    fileChooser.getExtensionFilters().add(extensionFilterCSV);
    fileChooser.getExtensionFilters().add(extensionFilterPNG);

    //Opening dialog to choose the path to save the png files to.
    FxThreadUtil.runOnFxThreadAndWait(() -> file.set(fileChooser.showSaveDialog(null)));
    if (file.get() == null) {
      return;
    }
    final String selectedFormat = FilenameUtils.getExtension(file.get().getName());

    if (selectedFormat.equals("xy")) {
      MassvoltammogramExport.toCSV(plot, file.get());
    } else if (selectedFormat.equals("png")) {
      MassvoltammogramExport.toPNG(plot, file.get());
    }
  }

  public static void toPNG(ExtendedPlot3DPanel plot, File file) {

    //Saving the rendered picture to a png file.
    try {
      plot.toGraphicFile(file);
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

  public static void toCSV(ExtendedPlot3DPanel plot, File file) {

    //Getting the file name and path.
    final String fileName = FilenameUtils.removeExtension(file.getName());
    final String folderPath = FilenameUtils.removeExtension(file.getAbsolutePath());

    //Creating a new folder at the selected directory.
    try {
      Files.createDirectory(Paths.get(folderPath));
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }

    //Getting the data to export from the PlotPanel.
    final List<double[][]> scans = plot.getRawScansInMzRange();

    //Exporting the data to csv files.
    for (double[][] scan : scans) {

      //Initializing a file writer to export the csv file and naming the file.
      try (FileWriter writer = new FileWriter(
          folderPath + "\\" + fileName + "_" + scan[0][2] + "_mV.xy")) {

        //Filling the csv file with all data from the scan.
        for (double[] dataPoint : scan) {
          writer.append(String.valueOf(dataPoint[0])); //m/z-value
          writer.append(" ");
          writer.append(String.valueOf(dataPoint[1])); //intensity-value
          writer.append("\n");
        }
        writer.flush();
      }
      //Handling the exception from the file writer.
      catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }
}
