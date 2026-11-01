package net.mooctest;

import java.util.*;

public class ReportGenerator {
    public String generate(Project project) {
        if (project == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Project:").append(project.getName()).append("\n");
        Map<Task.Status, Long> counts = project.statusCounts();
        for (Task.Status s : Task.Status.values()) {
            sb.append("Status ").append(s.name()).append(":").append(counts.get(s)).append("\n");
        }
        sb.append("CriticalPath:").append(project.criticalPathDuration()).append("\n");
        sb.append("BudgetCost:").append(project.getBudget().totalCost()).append("\n");
        sb.append("BudgetValue:").append(project.getBudget().totalValue()).append("\n");
        RiskAnalyzer.SimulationResult r = project.analyzeRisk(100);
        sb.append("RiskMean:").append(r.getMeanImpact()).append("\n");
        sb.append("RiskP90:").append(r.getP90Impact()).append("\n");
        sb.append("RiskWorst:").append(r.getWorstCaseImpact()).append("\n");
        return sb.toString();
    }
}
