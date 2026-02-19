package protect.card_locker.coverage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The CoverageTool class provides functionality to track which branches have been covered
 * (branch coverage) during the execution of specific functions in the application.
 */
public class CoverageTool {

    static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        outputCoverageStatistics();
    }));
}

    private static final String[] FUNCTIONS = {
            "onResume in LoyaltyCardViewActivity",
            "onClick in ChooseCardImage",
            "importLoyaltyCard in CatimaImporter",
            "importJSON in VoucherVaultImporter",
            "onResume in LoyaltyCardEditActivity"
    };

    private static final boolean[][] functionFlags = new boolean[5][100];

    /**
     * Sets the flag for the specified branch that was taken in function 1.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc1Flag(int index) {
        functionFlags[0][index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 2.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc2Flag(int index) {
        functionFlags[1][index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 3.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc3Flag(int index) {
        functionFlags[2][index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 4.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc4Flag(int index) {
        functionFlags[3][index] = true;
    }

    /**
     * Sets the flag for the specified branch that was taken in function 5.
     *
     * @param index the index of the branch that has been taken. Must be less than 100.
     */
    public static void setFunc5Flag(int index) {
        functionFlags[4][index] = true;
    }

    /**
     * Outputs coverage statistics for the tracked functions. This method calculates and
     * prints the percentage of branch coverage for each each function.
     */
    public static void outputCoverageStatistics() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < functionFlags.length; i++) {
            sb.append(reportFunction(i, functionFlags[i]));
        }

        File file = new File("build/reports/manual-coverage/report.txt");
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();

        try (FileWriter w = new FileWriter(file)) {
            w.write(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write manual coverage report to " + file.getAbsolutePath(), e);
        }
    }

    private static String reportFunction(int functionNumber, boolean[] flags) {
        int numberOfBranches = getNumberOfBranches(flags); // see bug notes below
        if (numberOfBranches <= 0) {
            return "Function " + (functionNumber + 1) + " (" + FUNCTIONS[functionNumber] + "): no branches declared\n\n";
        }
        int taken = getTaken(flags, numberOfBranches);
        double cov = (double) taken / numberOfBranches;

        StringBuilder sb = new StringBuilder();
        sb.append("Function ").append(functionNumber + 1).append(" (").append(FUNCTIONS[functionNumber]).append("):\n");
        sb.append("Declared branches: ").append(numberOfBranches).append("\n");
        sb.append("Taken branches: ").append(taken).append("\n");
        sb.append(String.format("Coverage: %.1f%%\n", cov * 100.0));
        sb.append("Branches report:\n");
        for (int i = 0; i < numberOfBranches; i++) {
            sb.append("Branch ").append(i).append(": ").append(flags[i] ? "Taken" : "Not taken").append("\n");
        }
        return sb.append("\n").toString();
    }

    private static int getNumberOfBranches(boolean[] flags) {
        for (int i = flags.length - 1; 0 <= i; i--) {
            if (flags[i]) {
                return i;
            }
        }
        return -1;
    }

    private static int getTaken(boolean[] flags, int numberOfBranches) {
        int taken = 0;
        for (int i = 0; i < numberOfBranches; i++) {
            if (flags[i]) taken++;
        }
        return taken;
    }
}
