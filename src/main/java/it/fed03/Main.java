package it.fed03;

import it.unifi.cassandra.scheduling.Problem;
import it.unifi.cassandra.scheduling.model.*;
import it.unifi.cassandra.scheduling.solver.FEDScheduleGenerator;
import it.unifi.cassandra.scheduling.solver.edf.EDFSchedulabilityAnalysis;
import it.unifi.cassandra.scheduling.util.TimeInterval;
import it.unifi.cassandra.scheduling.util.WorkingDays;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        Set<Requirement> requirements = highDemand();
        highDemandStatistics(requirements);

//        allocStatistics(requirements);

//        GraphGenerator.generateDiagramFor(args[0], highDemand(), "edf");
//        GraphGenerator.generateDiagramFor(args[0], highDemand(), "fed");
    }

    private static void allocStatistics(Set<Requirement> requirements) {
        Problem p = new Problem(requirements, 8, new EDFSchedulabilityAnalysis(), new FEDScheduleGenerator());
        Map<Person, List<Allocation>> alloc = p.generateSchedule().stream().sorted(Comparator.comparing(Allocation::getDay)).collect(Collectors.groupingBy(Allocation::getPerson, Collectors.toList()));

        alloc.forEach(((person, allocations) -> {
            if (Objects.equals(person.getIdentifier(), "researcher2")) {
                Map<Assertion, List<Allocation>> allocByReq = allocations.stream().collect(Collectors.groupingBy(Allocation::assertion));
                for (Assertion assertion : allocByReq.keySet().stream().sorted(Comparator.comparing(assertion -> assertion.requirement().name())).collect(Collectors.toList())) {
                    List<Allocation> allocations1 = allocByReq.get(assertion);
                    System.out.println(assertion.requirement().name() + ": " + assertion.computationTime());

                    int totalCapacity = allocations1.size() * 8;
                    int workedHours = allocations1.stream().mapToInt(Allocation::getHoursAmount).sum();
                    double load = ((double) workedHours / totalCapacity);
                    System.out.println("Load for " + assertion.requirement().name() + ": " + load);

                    double wd = ((double) allocations1.size() / assertion.requirement().timeInterval().workingDays());

                    System.out.println("WD for " + assertion.requirement().name() + ": " + wd);
                }

            }
        }));

        p.generateSchedule();

        requirements.forEach(requirement -> {
            if (requirement.name().equals("proj07")) {
                Map<Person, List<Assertion>> assertionsByPerson = requirement.assertions().stream().collect(Collectors.groupingBy(Assertion::assignedPerson));
                assertionsByPerson.forEach((person, assertions) -> {
                    Assertion assertion = assertions.get(0);
                    Set<Allocation> allocations = assertion.allocations();
                    int totalCapacity = allocations.size() * 8;
                    int workedHours = allocations.stream().mapToInt(Allocation::getHoursAmount).sum();
                    double load = ((double) workedHours / totalCapacity);
                    System.out.println(assertion.computationTime());
                    System.out.println("Load for " + person.getIdentifier() + ": " + load);

                    double wd = ((double) allocations.size() / assertion.requirement().timeInterval().workingDays());

                    System.out.println("WD for " + assertion.requirement().name() + ": " + wd);
                });
            }
        });
    }

    private static void highDemandStatistics(Set<Requirement> requirements) {
        Map<Person, List<Assertion>> assertionByPerson = requirements.stream().flatMap(requirement -> requirement.assertions().stream()).collect(Collectors.groupingBy(Assertion::assignedPerson));
        assertionByPerson.forEach((person, assertions) -> {
            System.out.println("--------------------------------------------------");
            System.out.println(person.getIdentifier());
            LocalDate min = assertions.stream().map(Assertion::releaseTime).distinct().min(LocalDate::compareTo).get();
            LocalDate max = assertions.stream().map(Assertion::deadline).distinct().max(LocalDate::compareTo).get();

            System.out.println("Min: " + min);
            System.out.println("Max: " + max);
            int workingDays = WorkingDays.between(min, max);
            int toBeAllocated = assertions.stream().mapToInt(Assertion::computationTime).sum();
            System.out.println("Working Days: " + workingDays);
            System.out.println("To be allocated: " + toBeAllocated);
            System.out.println("Working rate: " + (double) toBeAllocated / (workingDays * 8));


            int intersections = 0;
            int intersectionsSize = 0;
            List<Assertion> sorted = new ArrayList<>(assertions);
            sorted.sort(Comparator.comparing(Assertion::releaseTime));
            Assertion toCheck = sorted.remove(0);
            while (sorted.size() > 0) {
                for (Assertion assertion : sorted) {
                    TimeInterval intersectInterval = toCheck.timeInterval().intersect(assertion.timeInterval());
                    if (intersectInterval != null) {
                        intersections++;
                        intersectionsSize += intersectInterval.workingDays();
                    }
                }
                toCheck = sorted.remove(0);
            }

            System.out.println("Intersections: " + intersections);
            System.out.println("AVG Intersections size: " + (double) intersectionsSize / (double) intersections);
            System.out.println("AVG Intersections per assertion: " + (double) intersections / (double) assertions.size());
        });
    }

    private static void highDemandTexFile(Set<Requirement> requirements) {
        String result = "\\begin{itemize}\n";
        int i = 1;
        for (Requirement requirement : requirements.stream().sorted(Comparator.comparing(Requirement::name)).collect(Collectors.toList())) {
            List<String> peopleName = requirement.assignedPeople().stream().map(Person::getIdentifier).sorted().collect(Collectors.toList());
            result = result.concat(String.format("\\item Requirement %s - %s - %s ore - [%s;%s) - assegnato a %s\\\\\n",
                    i,
                    requirement.name(), requirement.computationTime(),
                    requirement.timeInterval().getStartInclusive(), requirement.timeInterval().getEndExclusive(),
                    String.join(", ", peopleName)
            ));
            result = result.concat("Assertion:\n\\begin{itemize}\n");
            for (Assertion assertion : requirement.assertions()) {
                result = result.concat(String.format("\\item %s ore - [%s;%s) - assegnato a %s\n", assertion.computationTime(), assertion.releaseTime(), assertion.deadline(), assertion.assignedPerson().getIdentifier()));
            }
            result = result.concat("\\end{itemize}\n");
            i++;
        }
        result = result.concat("\\end{itemize}");

        try {
            Files.write(Paths.get("C:\\Users\\templ\\Google Drive\\tesi\\thesis-template\\files\\highDemandEx.tex"),
                    result.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<Requirement> highDemand() {
        Person researcher1 = new Researcher("researcher1");
        Person researcher2 = new Researcher("researcher2");
        Person researcher3 = new Researcher("researcher3");

        // proj1
        Requirement proj1 = new Requirement(
                "proj01",
                986,
                LocalDate.of(2017, 3, 1),
                LocalDate.of(2017, 11, 23),
                Arrays.asList(researcher1, researcher2)
        );
        Assertion proj1A1 = new Assertion(
                proj1,
                800,
                LocalDate.of(2017, 4, 3),
                LocalDate.of(2017, 11, 15),
                researcher1
        );
        Assertion proj1A2 = new Assertion(
                proj1,
                186,
                LocalDate.of(2017, 3, 31),
                LocalDate.of(2017, 5, 25),
                researcher2
        );
        AssertionCollection<Assertion> proj1Assertion = new AssertionCollection<>();
        proj1Assertion.addAll(Arrays.asList(proj1A1, proj1A2));
        proj1.setAssertions(proj1Assertion);

        // proj2
        Requirement proj2 = new Requirement(
                "proj02",
                403,
                LocalDate.of(2017, 4, 1),
                LocalDate.of(2017, 8, 31),
                Arrays.asList(researcher1)
        );
        Assertion proj2A1 = new Assertion(
                proj2,
                403,
                LocalDate.of(2017, 4, 24),
                LocalDate.of(2017, 8, 2),
                researcher1
        );
        AssertionCollection<Assertion> proj2Assertion = new AssertionCollection<>();
        proj2Assertion.addAll(Arrays.asList(proj2A1));
        proj2.setAssertions(proj2Assertion);

        // proj3
        Requirement proj3 = new Requirement(
                "proj03",
                765,
                LocalDate.of(2017, 7, 1),
                LocalDate.of(2018, 8, 31),
                Arrays.asList(researcher1, researcher2, researcher3)
        );
        Assertion proj3A1 = new Assertion(
                proj3,
                307,
                LocalDate.of(2017, 7, 19),
                LocalDate.of(2018, 8, 13),
                researcher1
        );
        Assertion proj3A2 = new Assertion(
                proj3,
                323,
                LocalDate.of(2017, 11, 30),
                LocalDate.of(2018, 7, 7),
                researcher2
        );
        Assertion proj3A3 = new Assertion(
                proj3,
                135,
                LocalDate.of(2018, 1, 30),
                LocalDate.of(2018, 6, 1),
                researcher3
        );
        AssertionCollection<Assertion> proj3Assertion = new AssertionCollection<>();
        proj3Assertion.addAll(Arrays.asList(proj3A1, proj3A2, proj3A3));
        proj3.setAssertions(proj3Assertion);

        // proj4
        Requirement proj4 = new Requirement(
                "proj04",
                1440,
                LocalDate.of(2017, 5, 1),
                LocalDate.of(2018, 5, 31),
                Arrays.asList(researcher1, researcher2)
        );
        Assertion proj4A1 = new Assertion(
                proj4,
                358,
                LocalDate.of(2017, 10, 19),
                LocalDate.of(2018, 5, 12),
                researcher1
        );
        Assertion proj4A2 = new Assertion(
                proj4,
                1082,
                LocalDate.of(2017, 5, 2),
                LocalDate.of(2018, 4, 29),
                researcher2
        );
        AssertionCollection<Assertion> proj4Assertion = new AssertionCollection<>();
        proj4Assertion.addAll(Arrays.asList(proj4A1, proj4A2));
        proj4.setAssertions(proj4Assertion);

        // proj5
        Requirement proj5 = new Requirement(
                "proj05",
                264,
                LocalDate.of(2017, 10, 1),
                LocalDate.of(2017, 12, 31),
                Arrays.asList(researcher1)
        );
        Assertion proj5A1 = new Assertion(
                proj5,
                264,
                LocalDate.of(2017, 10, 19),
                LocalDate.of(2017, 12, 28),
                researcher1
        );
        AssertionCollection<Assertion> proj5Assertion = new AssertionCollection<>();
        proj5Assertion.addAll(Arrays.asList(proj5A1));
        proj5.setAssertions(proj5Assertion);

        // proj6
        Requirement proj6 = new Requirement(
                "proj06",
                324,
                LocalDate.of(2017, 11, 1),
                LocalDate.of(2018, 11, 30),
                Arrays.asList(researcher1, researcher2)
        );
        Assertion proj6A1 = new Assertion(
                proj6,
                231,
                LocalDate.of(2017, 11, 15),
                LocalDate.of(2018, 11, 13),
                researcher1
        );
        Assertion proj6A2 = new Assertion(
                proj6,
                93,
                LocalDate.of(2018, 1, 8),
                LocalDate.of(2018, 3, 13),
                researcher2
        );
        AssertionCollection<Assertion> proj6Assertion = new AssertionCollection<>();
        proj6Assertion.addAll(Arrays.asList(proj6A1, proj6A2));
        proj6.setAssertions(proj6Assertion);

        // proj7
        Requirement proj7 = new Requirement(
                "proj07",
                499,
                LocalDate.of(2017, 12, 1),
                LocalDate.of(2018, 3, 31),
                Arrays.asList(researcher1, researcher3)
        );
        Assertion proj7A1 = new Assertion(
                proj7,
                158,
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 3, 20),
                researcher1
        );
        Assertion proj7A3 = new Assertion(
                proj7,
                341,
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 3, 29),
                researcher3
        );
        AssertionCollection<Assertion> proj7Assertion = new AssertionCollection<>();
        proj7Assertion.addAll(Arrays.asList(proj7A1, proj7A3));
        proj7.setAssertions(proj7Assertion);

        // proj8
        Requirement proj8 = new Requirement(
                "proj08",
                377,
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 5, 31),
                Arrays.asList(researcher1, researcher3)
        );
        Assertion proj8A1 = new Assertion(
                proj8,
                203,
                LocalDate.of(2018, 1, 25),
                LocalDate.of(2018, 3, 20),
                researcher1
        );
        Assertion proj8A3 = new Assertion(
                proj8,
                174,
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 5, 4),
                researcher3
        );
        AssertionCollection<Assertion> proj8Assertion = new AssertionCollection<>();
        proj8Assertion.addAll(Arrays.asList(proj8A1, proj8A3));
        proj8.setAssertions(proj8Assertion);

        // proj9
        Requirement proj9 = new Requirement(
                "proj09",
                268,
                LocalDate.of(2018, 3, 1),
                LocalDate.of(2018, 7, 31),
                Arrays.asList(researcher1, researcher3)
        );
        Assertion proj9A1 = new Assertion(
                proj9,
                215,
                LocalDate.of(2018, 3, 20),
                LocalDate.of(2018, 7, 1),
                researcher1
        );
        Assertion proj9A3 = new Assertion(
                proj9,
                53,
                LocalDate.of(2018, 4, 15),
                LocalDate.of(2018, 5, 17),
                researcher3
        );
        AssertionCollection<Assertion> proj9Assertion = new AssertionCollection<>();
        proj9Assertion.addAll(Arrays.asList(proj9A1, proj9A3));
        proj9.setAssertions(proj9Assertion);

        // proj10
        Requirement proj10 = new Requirement(
                "proj10",
                840,
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 11, 30),
                Arrays.asList(researcher1, researcher2, researcher3)
        );
        Assertion proj10A1 = new Assertion(
                proj10,
                99,
                LocalDate.of(2018, 5, 12),
                LocalDate.of(2018, 7, 9),
                researcher1
        );
        Assertion proj10A2 = new Assertion(
                proj10,
                224,
                LocalDate.of(2018, 1, 8),
                LocalDate.of(2018, 2, 15),
                researcher2
        );
        Assertion proj10A3 = new Assertion(
                proj10,
                517,
                LocalDate.of(2018, 5, 4),
                LocalDate.of(2018, 11, 2),
                researcher3
        );
        AssertionCollection<Assertion> proj10Assertion = new AssertionCollection<>();
        proj10Assertion.addAll(Arrays.asList(proj10A1, proj10A2, proj10A3));
        proj10.setAssertions(proj10Assertion);

        // proj11
        Requirement proj11 = new Requirement(
                "proj11",
                453,
                LocalDate.of(2018, 6, 1),
                LocalDate.of(2018, 11, 30),
                Arrays.asList(researcher1, researcher3)
        );
        Assertion proj11A1 = new Assertion(
                proj11,
                106,
                LocalDate.of(2018, 6, 15),
                LocalDate.of(2018, 8, 30),
                researcher1
        );
        Assertion proj11A3 = new Assertion(
                proj11,
                347,
                LocalDate.of(2018, 6, 27),
                LocalDate.of(2018, 11, 2),
                researcher3
        );
        AssertionCollection<Assertion> proj11Assertion = new AssertionCollection<>();
        proj11Assertion.addAll(Arrays.asList(proj11A1, proj11A3));
        proj11.setAssertions(proj11Assertion);

        // proj12
        Requirement proj12 = new Requirement(
                "proj12",
                282,
                LocalDate.of(2017, 3, 1),
                LocalDate.of(2017, 8, 31),
                Arrays.asList(researcher2)
        );
        Assertion proj12A2 = new Assertion(
                proj12,
                282,
                LocalDate.of(2017, 3, 2),
                LocalDate.of(2017, 8, 17),
                researcher2
        );
        AssertionCollection<Assertion> proj12Assertion = new AssertionCollection<>();
        proj12Assertion.addAll(Arrays.asList(proj12A2));
        proj12.setAssertions(proj12Assertion);

        // proj13
        Requirement proj13 = new Requirement(
                "proj13",
                372,
                LocalDate.of(2018, 7, 1),
                LocalDate.of(2018, 12, 31),
                Arrays.asList(researcher1, researcher3)
        );
        Assertion proj13A1 = new Assertion(
                proj13,
                145,
                LocalDate.of(2018, 7, 9),
                LocalDate.of(2018, 12, 31),
                researcher1
        );
        Assertion proj13A3 = new Assertion(
                proj13,
                227,
                LocalDate.of(2018, 10, 13),
                LocalDate.of(2018, 12, 29),
                researcher3
        );
        AssertionCollection<Assertion> proj13Assertion = new AssertionCollection<>();
        proj13Assertion.addAll(Arrays.asList(proj13A1, proj13A3));
        proj13.setAssertions(proj13Assertion);

        // proj14
        Requirement proj14 = new Requirement(
                "proj14",
                437,
                LocalDate.of(2017, 8, 1),
                LocalDate.of(2018, 2, 28),
                Arrays.asList(researcher2, researcher3)
        );
        Assertion proj14A2 = new Assertion(
                proj14,
                274,
                LocalDate.of(2017, 8, 10),
                LocalDate.of(2017, 11, 30),
                researcher2
        );
        Assertion proj14A3 = new Assertion(
                proj14,
                163,
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 2, 26),
                researcher3
        );
        AssertionCollection<Assertion> proj14Assertion = new AssertionCollection<>();
        proj14Assertion.addAll(Arrays.asList(proj14A2, proj14A3));
        proj14.setAssertions(proj14Assertion);

        // proj15
        Requirement proj15 = new Requirement(
                "proj15",
                173,
                LocalDate.of(2018, 8, 1),
                LocalDate.of(2018, 10, 31),
                Arrays.asList(researcher1)
        );
        Assertion proj15A1 = new Assertion(
                proj15,
                173,
                LocalDate.of(2018, 8, 30),
                LocalDate.of(2018, 10, 30),
                researcher1
        );
        AssertionCollection<Assertion> proj15Assertion = new AssertionCollection<>();
        proj15Assertion.addAll(Arrays.asList(proj15A1));
        proj15.setAssertions(proj15Assertion);

        // proj16
        Requirement proj16 = new Requirement(
                "proj16",
                16,
                LocalDate.of(2018, 9, 10),
                LocalDate.of(2018, 9, 20),
                Arrays.asList(researcher1)
        );
        Assertion proj16A1 = new Assertion(
                proj16,
                16,
                LocalDate.of(2018, 9, 13),
                LocalDate.of(2018, 9, 16),
                researcher1
        );
        AssertionCollection<Assertion> proj16Assertion = new AssertionCollection<>();
        proj16Assertion.addAll(Arrays.asList(proj16A1));
        proj16.setAssertions(proj16Assertion);

        // proj17
        Requirement proj17 = new Requirement(
                "proj17",
                114,
                LocalDate.of(2018, 7, 1),
                LocalDate.of(2018, 12, 31),
                Arrays.asList(researcher1)
        );
        Assertion proj17A1 = new Assertion(
                proj17,
                114,
                LocalDate.of(2018, 7, 30),
                LocalDate.of(2018, 12, 21),
                researcher1
        );
        AssertionCollection<Assertion> proj17Assertion = new AssertionCollection<>();
        proj17Assertion.addAll(Arrays.asList(proj17A1));
        proj17.setAssertions(proj17Assertion);

        return new HashSet<>(Arrays.asList(proj1, proj2, proj3, proj4, proj5, proj6, proj7, proj8, proj9, proj10, proj11, proj12, proj13, proj14, proj15, proj16, proj17));
    }

}

