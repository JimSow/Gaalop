package de.gaalop.tba.cfgImport;

import de.gaalop.OptimizationException;
import de.gaalop.cfg.ControlFlowGraph;
import de.gaalop.tba.Plugin;
import de.gaalop.tba.UseAlgebra;
import de.gaalop.tba.cfgImport.optimization.OptConstantPropagation;
import de.gaalop.tba.cfgImport.optimization.OptMaxima;
import de.gaalop.tba.cfgImport.optimization.OptOneExpressionsRemoval;
import de.gaalop.tba.cfgImport.optimization.OptimizationStrategyWithModifyFlag;
import de.gaalop.tba.cfgImport.optimization.OptUnusedAssignmentsRemoval;
import java.util.LinkedList;

/**
 * This class provides a simple facade to transform the graph
 * with the table based approach
 *
 * @author Christian Steinmetz
 */
public class CFGImporterFacade {

    private UseAlgebra usedAlgebra;
    private LinkedList<OptimizationStrategyWithModifyFlag> optimizations;
    private Plugin plugin;

    public CFGImporterFacade(Plugin plugin) {
        this.plugin = plugin;

        //load desired algebra
        usedAlgebra = new UseAlgebra(plugin.getAlgebra());

        optimizations = new LinkedList<OptimizationStrategyWithModifyFlag>();

        if (plugin.isOptConstantPropagation()) {
            optimizations.add(new OptConstantPropagation());
        }
        if (plugin.isOptUnusedAssignments()) {
            optimizations.add(new OptUnusedAssignmentsRemoval());
        }
        if (plugin.isOptOneExpressionRemoval()) {
            optimizations.add(new OptOneExpressionsRemoval());
        }

    }

    /**
     * Transforms the graph and apply optionally optimizations.
     * Note, that the graph is changed in place.
     *
     * @param graph The graph to be transformed
     * @return The transformed graph
     * @throws OptimizationException
     */
    public ControlFlowGraph importGraph(ControlFlowGraph graph) throws OptimizationException {

        if (ContainsControlFlow.containsControlFlow(graph)) {
            throw new OptimizationException("Due to Control Flow Existence in Source, TBA isn't assigned on graph!", graph);
        }

        if (ContainsMultipleAssignments.containsMulipleAssignments(graph)) {
            throw new OptimizationException("Due to Existence of MultipleAssignments in Source, TBA isn't assigned on graph!", graph);
        }

        if (!usedAlgebra.isN3()) {
            BaseVectorChecker checker = new BaseVectorChecker(usedAlgebra.getAlgebra().getBase());
            graph.accept(checker);
        }

        if (!plugin.isInvertTransformation()) {
            DivisionRemover divisionRemover = new DivisionRemover();
            graph.accept(divisionRemover);
        }

        if (!plugin.isScalarFunctions()) {
            VariablesCollector collector = new VariablesCollector();
            graph.accept(collector);

            MathFunctionSeparator mathFunctionSeparator = new MathFunctionSeparator(collector.getVariables());
            graph.accept(mathFunctionSeparator);
        }

        CFGImporter builder = new CFGImporter(usedAlgebra, plugin.isScalarFunctions());
        graph.accept(builder);

        int count = 0;
        boolean repeat;
        do {
            repeat = false;
            for (OptimizationStrategyWithModifyFlag curOpt : optimizations) {
                repeat = repeat || curOpt.transform(graph, usedAlgebra);
            }
            count++;
        } while (repeat);

        //Use Maxima only once
        if (plugin.isOptMaxima()) {
            OptMaxima optMaxima = new OptMaxima(plugin.getMaximaCommand(), plugin);
            optMaxima.transform(graph, usedAlgebra);

            //repeat other optimizations
            count = 0;
            do {
                repeat = false;
                for (OptimizationStrategyWithModifyFlag curOpt : optimizations) {
                    repeat = repeat || curOpt.transform(graph, usedAlgebra);
                }
                count++;
            } while (repeat);
        }

        return graph;
    }

    public void setUsedAlgebra(UseAlgebra usedAlgebra) {
        this.usedAlgebra = usedAlgebra;
    }
}