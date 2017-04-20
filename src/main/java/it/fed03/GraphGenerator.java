package it.fed03;

import it.unifi.cassandra.scheduling.Problem;
import it.unifi.cassandra.scheduling.ScheduleGenerator;
import it.unifi.cassandra.scheduling.model.Allocation;
import it.unifi.cassandra.scheduling.model.Person;
import it.unifi.cassandra.scheduling.model.Requirement;
import it.unifi.cassandra.scheduling.solver.FEDScheduleGenerator;
import it.unifi.cassandra.scheduling.solver.edf.EDFSchedulabilityAnalysis;
import it.unifi.cassandra.scheduling.solver.edf.EDFScheduleGenerator;
import it.unifi.cassandra.scheduling.util.ScheduleManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraphGenerator {
    public static void generateDiagramFor(String folder, Set<Requirement> requirements, String type) {
        ScheduleGenerator alg = null;
        if (type.equals("edf")) {
            alg = new EDFScheduleGenerator();
        } else if (type.equals("fed")) {
            alg = new FEDScheduleGenerator();
        }

        Problem p = new Problem(requirements, 8, new EDFSchedulabilityAnalysis(), alg);
        List<Allocation> allocations = new ArrayList<>(p.generateSchedule());

        allocations.sort(Comparator.comparing(Allocation::getDay));
        List<Allocation> allocationsSplit = allocations.subList(0, (int) Math.ceil(allocations.size() * 0.50));
        List<Person> people = allocationsSplit.stream().map(Allocation::getPerson).distinct().collect(Collectors.toList());
        people.forEach(person -> {
            JFreeChart chart = generatePersonWorkedHours(allocationsSplit, person, type);
            saveChartToImage(chart, Paths.get(folder, type), 896, 672);
        });

        requirements.forEach(requirement -> {
            JFreeChart chart = generateRequirementWorkedHours(allocations, requirement, type);
            saveChartToImage(chart, Paths.get(folder, type), 896, 672);
        });

        JFreeChart chart = generateTotalWorkedHours(allocations, type);
        saveChartToImage(chart, Paths.get(folder, type), 896, 574);
    }

    private static void saveChartToImage(JFreeChart chart, Path folderPath, int width, int height) {
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String imageName = chart.getTitle().getText().toLowerCase().replace(" ", "_");
        imageName = imageName.substring(0, imageName.length() - 6);
        File image = Paths.get(folderPath.toString(), imageName + ".jpg").toFile();
        try {
            ChartUtilities.saveChartAsJPEG(image, 1f, chart, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JFreeChart generateTotalWorkedHours(List<Allocation> allocations, String type) {
        ScheduleManager scheduleManager = new ScheduleManager(allocations);
        Map<Person, Map<LocalDate, Integer>> peopleWorkedHours = scheduleManager.peopleTotalWorkedHours();
        Map<LocalDate, Integer> totalWorkedHours = scheduleManager.totalWorkedHours();

        TimeSeriesCollection peopleWorkedHoursDataset = generateDataset(Person::getIdentifier, peopleWorkedHours);
        TimeSeries totalWorkedHoursDataset = createTimeSeries("Total amount", totalWorkedHours);

        return buildChart("Total worked hours by researcher (" + type.toUpperCase() + ")", peopleWorkedHoursDataset, totalWorkedHoursDataset, new DateTickUnit(DateTickUnitType.MONTH, 1));
    }

    private static JFreeChart generateRequirementWorkedHours(List<Allocation> allocations, Requirement requirement, String type) {
        ScheduleManager scheduleManager = new ScheduleManager(allocations);
        Map<Person, Map<LocalDate, Integer>> peopleWorkedHours = scheduleManager.peopleWorkedHoursByRequirement(requirement);
        Map<LocalDate, Integer> totalWorkedHoursByRequirement = scheduleManager.totalWorkedHoursByRequirement(requirement);

        TimeSeriesCollection peopleWorkedHoursDataset = generateDataset(Person::getIdentifier, peopleWorkedHours);
        TimeSeries totalWorkedHoursDataset = createTimeSeries(requirement.name(), totalWorkedHoursByRequirement);

        return buildChart(requirement.name() + " worked hours by researcher (" + type.toUpperCase() + ")", peopleWorkedHoursDataset, totalWorkedHoursDataset);
    }

    private static JFreeChart generatePersonWorkedHours(List<Allocation> allocations, Person person, String type) {
        ScheduleManager scheduleManager = new ScheduleManager(allocations);
        Map<Requirement, Map<LocalDate, Integer>> requirementsWorkedHours = scheduleManager.peopleWorkedHoursGroupedByRequirement(person);
        Map<LocalDate, Integer> totalWorkedHoursByPerson = scheduleManager.personWorkedHours(person);

        TimeSeriesCollection dataset = generateDataset(Requirement::name, requirementsWorkedHours);
        TimeSeries totalPersonAmount = createTimeSeries(person.getIdentifier(), totalWorkedHoursByPerson);

        return buildChart("Requirements worked hours by " + person.getIdentifier() + " (" + type.toUpperCase() + ")", dataset, totalPersonAmount, new DateTickUnit(DateTickUnitType.MONTH, 1));
    }

    private static <E> TimeSeriesCollection generateDataset(Function<E, String> seriesName, Map<E, Map<LocalDate, Integer>> workedHours) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        workedHours.forEach((entity, workedHoursList) -> {
            dataset.addSeries(createTimeSeries(seriesName.apply(entity), workedHoursList));
        });
        return dataset;
    }

    private static JFreeChart buildChart(String chartTitle, TimeSeriesCollection dataset, TimeSeries totalAmountSeries) {
        DateTickUnit tickUnit = new DateTickUnit(DateTickUnitType.DAY, 14);
        return buildChart(chartTitle, dataset, totalAmountSeries, tickUnit);
    }

    private static JFreeChart buildChart(String chartTitle, TimeSeriesCollection dataset, TimeSeries totalAmountSeries, DateTickUnit tickUnit) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Date", "Worked hours", dataset);
        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(219, 221, 226));
        plot.setRangeGridlinePaint(Color.gray);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(1.75f));
        }

        buildTotalHoursChart(plot, totalAmountSeries);

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("d/MM/yy"));
        domainAxis.setVerticalTickLabels(true);
        domainAxis.setTickUnit(tickUnit);
        return chart;
    }

    private static void buildTotalHoursChart(XYPlot plot, TimeSeries totalAmountSeries) {
        XYItemRenderer clonedRender = null;
        try {
            clonedRender = (XYItemRenderer) ((XYLineAndShapeRenderer) plot.getRenderer()).clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        clonedRender.setSeriesStroke(0, new BasicStroke(5f));
        clonedRender.setSeriesPaint(0, new Color(255, 230, 171));

        plot.setDataset(1, new TimeSeriesCollection(totalAmountSeries));
        plot.setRenderer(1, clonedRender);
    }

    private static TimeSeries createTimeSeries(String seriesName, Map<LocalDate, Integer> allocationsList) {
        TimeSeries series = new TimeSeries(seriesName);
        allocationsList.forEach((date, value) -> {
            series.add(convertToTime(date), value);
        });

        return series;
    }

    private static RegularTimePeriod convertToTime(LocalDate date) {
        return new Day(convertToDate(date));
    }

    private static Date convertToDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }
}
