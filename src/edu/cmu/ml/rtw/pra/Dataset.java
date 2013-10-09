package edu.cmu.ml.rtw.pra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.rtw.users.matt.util.CollectionsUtil;
import edu.cmu.ml.rtw.util.Pair;

/**
 * A collection of positive and negative (source, target) pairs.  Generally, it's assumed that
 * positive instances are specified before negative instances, and sources are specified before
 * targets.  So, for instance, if we check positiveTargets and it's null, we don't bother checking
 * negativeTargets.
 */
public class Dataset {
    private final List<Integer> positiveSources;
    private final List<Integer> positiveTargets;

    private final List<Integer> negativeSources;
    private final List<Integer> negativeTargets;

    public static Dataset readFromFile(String file) throws IOException {
        return readFromReader(new BufferedReader(new FileReader(file)));
    }

    public static Dataset readFromFile(File file) throws IOException {
        return readFromReader(new BufferedReader(new FileReader(file)));
    }

    /**
     * Reads a Dataset from a file.  The format is assumed to be TSV with the following columns:
     *
     * source [tab] target
     *
     * where all examples are assumed to be positive, or
     *
     * source [tab] target [tab] {1,-1}
     *
     * where a 1 in the last column indicates a positive example and a -1 indicates a negative
     * example.
     */
    public static Dataset readFromReader(BufferedReader reader) throws IOException {
        List<Integer> positiveSources = new ArrayList<Integer>();
        List<Integer> positiveTargets = new ArrayList<Integer>();
        List<Integer> negativeSources = new ArrayList<Integer>();
        List<Integer> negativeTargets = new ArrayList<Integer>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            boolean positive;
            if (fields.length == 2) {
                positive = true;
            } else {
                positive = fields[2].equals("1");
            }
            int source = Integer.parseInt(fields[0]);
            int target = Integer.parseInt(fields[1]);
            if (positive) {
                positiveSources.add(source);
                positiveTargets.add(target);
            } else {
                negativeSources.add(source);
                negativeTargets.add(target);
            }
        }
        reader.close();
        if (negativeSources.size() == 0) {
            negativeSources = null;
        }
        if (negativeTargets.size() == 0) {
            negativeTargets = null;
        }
        return new Dataset.Builder()
                .setPositiveSources(positiveSources)
                .setPositiveTargets(positiveTargets)
                .setNegativeSources(negativeSources)
                .setNegativeTargets(negativeTargets)
                .build();
    }

    /**
     * Takes the examples in this dataset and splits it into two datasets, where the first has
     * percent of the data.  Modulo rounding errors, the ratio of positive to negative examples
     * will be the same in both resultant datasets as it is in the original dataset.
     */
    public Pair<Dataset, Dataset> splitData(double percent) {
        List<Pair<Integer, Integer>> positiveInstances = getPositiveInstances();
        int numTraining = (int) (percent * positiveInstances.size());
        Collections.shuffle(positiveInstances);
        List<Pair<Integer, Integer>> trainingPositive = new ArrayList<Pair<Integer, Integer>>();
        List<Pair<Integer, Integer>> testingPositive = new ArrayList<Pair<Integer, Integer>>();
        for (int i=0; i<positiveInstances.size(); i++) {
            if (i < numTraining) {
                trainingPositive.add(positiveInstances.get(i));
            } else {
                testingPositive.add(positiveInstances.get(i));
            }
        }
        List<Pair<Integer, Integer>> negativeInstances = getNegativeInstances();
        numTraining = (int) (percent * negativeInstances.size());
        Collections.shuffle(negativeInstances);
        List<Pair<Integer, Integer>> trainingNegative = new ArrayList<Pair<Integer, Integer>>();
        List<Pair<Integer, Integer>> testingNegative = new ArrayList<Pair<Integer, Integer>>();
        for (int i=0; i<negativeInstances.size(); i++) {
            if (i < numTraining) {
                trainingNegative.add(negativeInstances.get(i));
            } else {
                testingNegative.add(negativeInstances.get(i));
            }
        }
        Dataset trainingData = new Dataset.Builder()
                .setPositiveSources(CollectionsUtil.unzipLeft(trainingPositive))
                .setPositiveTargets(CollectionsUtil.unzipRight(trainingPositive))
                .setNegativeSources(CollectionsUtil.unzipLeft(trainingNegative))
                .setNegativeTargets(CollectionsUtil.unzipRight(trainingNegative))
                .build();
        Dataset testingData = new Dataset.Builder()
                .setPositiveSources(CollectionsUtil.unzipLeft(testingPositive))
                .setPositiveTargets(CollectionsUtil.unzipRight(testingPositive))
                .setNegativeSources(CollectionsUtil.unzipLeft(testingNegative))
                .setNegativeTargets(CollectionsUtil.unzipRight(testingNegative))
                .build();
        return new Pair<Dataset, Dataset>(trainingData, testingData);
    }

    public List<Pair<Integer, Integer>> getPositiveInstances() {
        return CollectionsUtil.zipLists(positiveSources, positiveTargets);
    }

    public List<Pair<Integer, Integer>> getNegativeInstances() {
        if (negativeSources == null) {
            return null;
        }
        return CollectionsUtil.zipLists(negativeSources, negativeTargets);
    }

    // These "asStrings" methods are for quickly testing membership in the set.
    public Set<String> getPositiveInstancesAsStrings() {
        return getInstancesAsStrings(getPositiveInstances());
    }

    public Set<String> getNegativeInstancesAsStrings() {
        return getInstancesAsStrings(getNegativeInstances());
    }

    private Set<String> getInstancesAsStrings(List<Pair<Integer, Integer>> instances) {
        Set<String> instanceStrings = new HashSet<String>();
        if (instances == null) {
            return instanceStrings;
        }
        for (Pair<Integer, Integer> instance : instances) {
            instanceStrings.add(instance.getLeft() + " " + instance.getRight());
        }
        return instanceStrings;
    }

    public List<Integer> getPositiveSources() {
        return positiveSources;
    }

    public List<Integer> getPositiveTargets() {
        return positiveTargets;
    }

    public List<Integer> getNegativeSources() {
        return negativeSources;
    }

    public List<Integer> getNegativeTargets() {
        return negativeTargets;
    }

    public List<Integer> getAllSources() {
        List<Integer> allSources = new ArrayList<Integer>();
        allSources.addAll(positiveSources);
        if (negativeSources != null) {
            allSources.addAll(negativeSources);
        }
        return allSources;
    }

    public List<Integer> getAllTargets() {
        if (positiveTargets == null) {
            return null;
        }
        List<Integer> allTargets = new ArrayList<Integer>();
        allTargets.addAll(positiveTargets);
        if (negativeTargets != null) {
            allTargets.addAll(negativeTargets);
        }
        return allTargets;
    }

    public Map<Integer, Set<Integer>> getPositiveSourceMap() {
        return getSourceMap(positiveSources, positiveTargets);
    }

    public Map<Integer, Set<Integer>> getNegativeSourceMap() {
        return getSourceMap(negativeSources, negativeTargets);
    }

    public Map<Integer, Set<Integer>> getCombinedSourceMap() {
        return getSourceMap(getAllSources(), getAllTargets());
    }

    private Map<Integer, Set<Integer>> getSourceMap(List<Integer> sources, List<Integer> targets) {
        return CollectionsUtil.groupByKey(sources, targets);
    }

    private Dataset(Builder builder) {
        positiveSources = builder.positiveSources;
        positiveTargets = builder.positiveTargets;
        negativeSources = builder.negativeSources;
        negativeTargets = builder.negativeTargets;
    }

    public static class Builder {
        private List<Integer> positiveSources;
        private List<Integer> positiveTargets;
        private List<Integer> negativeSources;
        private List<Integer> negativeTargets;

        public Builder() { }

        public Dataset build() {
            // TODO(matt): verify that all necessary options have been set, and that sizes match,
            // and whatnot.
            return new Dataset(this);
        }
        public Builder setPositiveSources(List<Integer> x) {this.positiveSources = x; return this;}
        public Builder setPositiveTargets(List<Integer> x) {this.positiveTargets = x; return this;}
        public Builder setNegativeSources(List<Integer> x) {this.negativeSources = x; return this;}
        public Builder setNegativeTargets(List<Integer> x) {this.negativeTargets = x; return this;}
    }
}
