package de.gaalop.gapp.importing;

import de.gaalop.dfg.MultivectorComponent;
import de.gaalop.gapp.GAPP;
import de.gaalop.gapp.Selector;
import de.gaalop.gapp.Selectorset;
import de.gaalop.gapp.Variableset;
import de.gaalop.gapp.importing.parallelObjects.Constant;
import de.gaalop.gapp.importing.parallelObjects.DotProduct;
import de.gaalop.gapp.importing.parallelObjects.ExtCalculation;
import de.gaalop.gapp.importing.parallelObjects.MvComponent;
import de.gaalop.gapp.importing.parallelObjects.ParallelObject;
import de.gaalop.gapp.importing.parallelObjects.ParallelObjectType;
import de.gaalop.gapp.importing.parallelObjects.ParallelObjectVisitor;
import de.gaalop.gapp.importing.parallelObjects.Product;
import de.gaalop.gapp.importing.parallelObjects.Sum;
import de.gaalop.gapp.instructionSet.GAPPAssignMv;
import de.gaalop.gapp.instructionSet.GAPPCalculateMv;
import de.gaalop.gapp.instructionSet.GAPPDotVectors;
import de.gaalop.gapp.instructionSet.GAPPResetMv;
import de.gaalop.gapp.instructionSet.GAPPSetMv;
import de.gaalop.gapp.instructionSet.GAPPSetVector;
import de.gaalop.gapp.variables.GAPPConstant;
import de.gaalop.gapp.variables.GAPPMultivector;
import de.gaalop.gapp.variables.GAPPMultivectorComponent;
import de.gaalop.gapp.variables.GAPPScalarVariable;
import de.gaalop.gapp.variables.GAPPValueHolder;
import de.gaalop.gapp.variables.GAPPVector;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Creates GAPP code from the ParallelObject data structure
 * @author Christian Steinmetz
 */
public class GAPPCreator implements ParallelObjectVisitor {
    // arg: GAPPMultivectorComponent: destination of this operation, otherwise null
    // return: GAPPMultivectorComponent if a new one is created, otherwise null

    public GAPP gapp;
    
    private int curTmp = -1;

    private final String PREFIX_MV = "mv";
    private final String PREFIX_VE = "ve";

    private HashSet<String> variables;

    public GAPPCreator(HashSet<String> variables) {
        this.variables = variables;
    }

    public void setGapp(GAPP gapp) {
        this.gapp = gapp;
    }

    /**
     * Creates a new temp variable name with a prefix
     * @param prefix The prefix that should be used
     * @return The temp variable name
     */
    private String createTMP(String prefix) {
        curTmp++;

        while (variables.contains(prefix+curTmp))
            curTmp++;

        return prefix+curTmp;
    }

    /**
     * Creates a new GAPPMultivector and inserts a resetMv command in GAPP
     * @param The number of entries in the new multivector
     * @return The new GAPPMultivector
     */
    public GAPPMultivector createMv(int size) {
        GAPPMultivector mv = new GAPPMultivector(createTMP(PREFIX_MV));
        gapp.addInstruction(new GAPPResetMv(mv, size));
        return mv;
    }

    /**
     * Creates a multivector with one entry and returns the according GAPPMultivectorComponent
     * @return The GAPPMultivectorComponent
     */
    public GAPPMultivectorComponent createMvComp() {
        GAPPMultivector mv = new GAPPMultivector(createTMP(PREFIX_MV));
        gapp.addInstruction(new GAPPResetMv(mv, 1));
        return new GAPPMultivectorComponent(mv.getName(), 0);
    }

    /**
     * Creates a new GAPPVector
     * @return The new GAPPVector
     */
    private GAPPVector createVe() {
        return new GAPPVector(createTMP(PREFIX_VE));
    }

    @Override
    public Object visitProduct(Product product, Object arg) {
        DotProduct dp = new DotProduct();
        dp.setNegated(product.isNegated());
        int factorNo = 0;
        for (ParallelObject obj: product.getFactors()) {
            dp.set(0, factorNo, obj);
            factorNo++;
        }
        return dp.accept(this, arg);
    }

    @Override
    public Object visitExtCalculation(ExtCalculation extCalculation, Object arg) {
        //arg must be filled!

        GAPPMultivectorComponent destination = (GAPPMultivectorComponent) arg;

        GAPPMultivectorComponent altDestination = destination;
        if (extCalculation.isNegated())
            destination = createMvComp();

        if (!extCalculation.getOperand1().isTerminal()) {
            GAPPMultivector mvTmp1 = createMv(1);
            GAPPMultivectorComponent gMvC1 = new GAPPMultivectorComponent(mvTmp1.getName(), 0);
            extCalculation.getOperand1().accept(this, gMvC1);
            extCalculation.setOperand1(new MvComponent(new MultivectorComponent(mvTmp1.getName(), 0)));
        }

        if (extCalculation.getOperand2() != null) {
            if (!extCalculation.getOperand2().isTerminal()) {
                GAPPMultivector mvTmp2 = createMv(1);
                GAPPMultivectorComponent gMvC2 = new GAPPMultivectorComponent(mvTmp2.getName(), 0);
                extCalculation.getOperand2().accept(this, gMvC2);
                extCalculation.setOperand2(new MvComponent(new MultivectorComponent(mvTmp2.getName(), 0)));
            }
        }


        gapp.addInstruction(new GAPPCalculateMv(extCalculation.getType(),
                new GAPPMultivector(destination.getName()),
                createMultivectorFromParallelObjectTerminal(extCalculation.getOperand1()),
                (extCalculation.getOperand2() == null)
                    ? null
                    : createMultivectorFromParallelObjectTerminal(extCalculation.getOperand2())
                ));

        if (extCalculation.isNegated()) {

            Selectorset selDest = new Selectorset();
            selDest.add(new Selector(altDestination.getBladeIndex(), (byte) 1));

            Selectorset selSrc = new Selectorset();
            selSrc.add(new Selector(destination.getBladeIndex(), (byte) -1));

            GAPPSetMv setMv = new GAPPSetMv(
                    new GAPPMultivector(altDestination.getName()),
                    new GAPPMultivector(destination.getName()),
                    selDest, selSrc);

            gapp.addInstruction(setMv);
        }

        return null;
    }

    /**
     * Creates a new GAPPMultivector from ParallelObject which is a terminal
     * @param object The ParallelObject which is a terminal
     * @return The new GAPPMultivector
     */
    private GAPPMultivector createMultivectorFromParallelObjectTerminal(ParallelObject object) {
         GAPPMultivectorCreator creator = new GAPPMultivectorCreator(this);
         return (GAPPMultivector) object.accept(creator, null);
    }

    @Override
    public Object visitConstant(Constant constant, Object arg) {
        //arg must be filled!
        GAPPMultivectorComponent destination = (GAPPMultivectorComponent) arg;

        Selectorset selSet = new Selectorset();
        selSet.add(new Selector(
                destination.getBladeIndex(),
                (constant.isNegated()) ? (byte) -1 : (byte) 1)
                );

        Variableset varSet = new Variableset();
        varSet.add(new GAPPConstant(constant.getValue()));

        gapp.addInstruction(new GAPPAssignMv(new GAPPMultivector(destination.getName()), selSet, varSet));

        return null;
    }

    @Override
    public Object visitMvComponent(MvComponent mvComponent, Object arg) {
        //arg must be filled!
        GAPPMultivectorComponent destination = (GAPPMultivectorComponent) arg;

        Selectorset selDestSet = new Selectorset();
        selDestSet.add(new Selector(
                destination.getBladeIndex(),
                (mvComponent.isNegated()) ? (byte) -1 : (byte) 1)
                );

        Selectorset selSrcSet = new Selectorset();
        selSrcSet.add(new Selector(
                mvComponent.getMultivectorComponent().getBladeIndex(),
                (byte) 1)
                );

        gapp.addInstruction(new GAPPSetMv(
                new GAPPMultivector(destination.getName()),
                new GAPPMultivector(mvComponent.getMultivectorComponent().getName()),
                selDestSet, selSrcSet));

        return null;
    }

    @Override
    public Object visitDotProduct(DotProduct dotProduct, Object arg) {

        //make all inner elements to terminals
        for (int row=0;row<dotProduct.getHeight();row++)
            for (int col=0;col<dotProduct.getWidth();col++) {
                if (!dotProduct.get(row, col).isTerminal()) {
                    ParallelObject nonTerminalObj = dotProduct.get(row, col);
                    GAPPMultivector mvTmp = createMv(1);
                    GAPPMultivectorComponent gMvC = new GAPPMultivectorComponent(mvTmp.getName(),0);
                    nonTerminalObj.accept(this, gMvC);
                    dotProduct.set(row, col, new MvComponent(new MultivectorComponent(mvTmp.getName(), 0)));
                }
            }

        //now all inner elements are terminals
        //transformation is now easier
        
        //optimize order in every row
        optimizeOrder(dotProduct);

        GAPPMultivectorComponent destination = (arg == null)
                ? createMvComp()
                : (GAPPMultivectorComponent) arg;


        GAPPMultivectorComponent altDestination = destination;
        if (dotProduct.isNegated())
            destination = createMvComp();

        //create vectors for GAPPDotProduct
        LinkedList<GAPPVector> parts = new LinkedList<GAPPVector>();
        GAPPDotVectors dotVectors = new GAPPDotVectors(destination, parts);

        for (ParallelVector vector: dotProduct.getFactors())
            parts.add(createVectorFromParallelVector(vector));

        gapp.addInstruction(dotVectors);

        if (dotProduct.isNegated()) {

            Selectorset selDest = new Selectorset();
            selDest.add(new Selector(altDestination.getBladeIndex(), (byte) 1));

            Selectorset selSrc = new Selectorset();
            selSrc.add(new Selector(destination.getBladeIndex(), (byte) -1));

            GAPPSetMv setMv = new GAPPSetMv(
                    new GAPPMultivector(altDestination.getName()),
                    new GAPPMultivector(destination.getName()),
                    selDest, selSrc);

            gapp.addInstruction(setMv);
        }


        
        return (arg == null) ? destination: null;
    }

    /**
     * Optimizes the order per row in a dot product.
     * The order is optimal if the number of generated GAPP instructions is minimal
     * @param dotProduct The dot product
     */
    private void optimizeOrder(DotProduct dotProduct) {
        //TODO chs optimize order, so that less as possible multivectors are in one column
    }

    /**
     * Creates a GAPPVector from a ParallelVector
     * @param vector The ParallelVector
     * @return The new GAPPVector
     */
    private GAPPVector createVectorFromParallelVector(ParallelVector vector) {
        GAPPVector destination = createVe();
        //It may occour:
        // - MvComponent
        // - Constant

        // distinguish four cases:
        //1. only MvComponents from one multivector (using setVector (one operation))
        //2. only Constants (using assignMv and then setVector (three operations))
        //3. only MvComponents from more than one multivector (using setMv and then setVector (> three operations))
        //4. a mix of all two types (using assignMv,setMv and then setVector (> three operations))

        int numberOfMvComponents = countTypeInParallelVector(vector, ParallelObjectType.mvComponent);
        
        if (numberOfMvComponents == 0) 
            case2(destination, vector);
        else {
            int numberOfConstants = countTypeInParallelVector(vector, ParallelObjectType.constant);

            HashSet<String> multivectors = getMultivectors(vector);

            if (numberOfConstants == 0) {
                // case 1 or 3
                
                if (multivectors.size() == 1) 
                    case1(destination, vector);
                else
                    case3(destination, vector, multivectors);
                
            } else 
                case4(destination, vector, multivectors);
        }

        return destination;
    }

    /**
     * Handles case 1 in creation of a GAPPVector from a ParallelVector.
     * Case 1: Only MvComponents from one multivector (using setVector (one operation))
     * @param destination The destination GAPPVector
     * @param vector The ParallelVector
     */
    private void case1(GAPPVector destination, ParallelVector vector) {
        Selectorset selSet = new Selectorset();
        GAPPMultivector source = null;
        int slotNo = 0;
        for (ParallelObject slot: vector.getSlots()) {
            MultivectorComponent mvC = ((MvComponent) slot).getMultivectorComponent();
            if (slotNo == 0)
                source = new GAPPMultivector(mvC.getName());
            selSet.add(new Selector(mvC.getBladeIndex(), slot.isNegated() ? (byte) -1 : (byte) 1));
            slotNo++;
        }

        gapp.addInstruction(new GAPPSetVector(destination, source, selSet));
    }

    /**
     * Handles case 2 in creation of a GAPPVector from a ParallelVector.
     * Case 2: Only Constants (using assignMv and then setVector (three operations))
     * @param destination The destination GAPPVector
     * @param vector The ParallelVector
     */
    private void case2(GAPPVector destination, ParallelVector vector) {
        GAPPMultivector mvTmp = createMv(vector.getSlots().size());
        Variableset varMvDestSet = new Variableset();

        Selectorset selSet = new Selectorset();
        int slotNo = 0;
        for (ParallelObject obj: vector.getSlots()) {
            GAPPValueHolder valueHolder = null;
            switch (ParallelObjectType.getType(obj)) {
                case constant:
                    valueHolder = new GAPPConstant(((Constant) obj).getValue());
                    break;
            }
            varMvDestSet.add(valueHolder);
            selSet.add(new Selector(slotNo, obj.isNegated() ? (byte) -1: (byte) 1));
            slotNo++;
        }


        gapp.addInstruction(new GAPPAssignMv(
                    mvTmp,
                    selSet,
                    varMvDestSet
                ));

        gapp.addInstruction(new GAPPSetVector(
                    destination,
                    mvTmp,
                    createIncreasingSelectorset(vector.getSlots().size())
                ));
    }

    /**
     * Creates a Selectorset which consists of an increasing series of slots.
     * The sign of all slots is +1
     * @param numberOfSlots The number of slots that should be created
     * @return The Selectorset
     */
    private Selectorset createIncreasingSelectorset(int numberOfSlots) {
        Selectorset result = new Selectorset();
        for (int slot = 0;slot<numberOfSlots;slot++)
            result.add(new Selector(slot, (byte) 1));
        return result;
    }

    /**
     * Handles case 3 in creation of a GAPPVector from a ParallelVector.
     * Case 3: Only MvComponents from more than one multivector (using setMv and then setVector (> three operations))
     * #GAPPInstructions: multivectors.size() + 2
     * 
     * @param destination The destination GAPPVector
     * @param vector The ParallelVector
     * @param multivectors The multivector names in the vector
     */
    private void case3(GAPPVector destination, ParallelVector vector, HashSet<String> multivectors) {
        GAPPMultivector mvTmp = createMv(vector.getSlots().size());

        copyFromManyMultivectors(mvTmp, vector, multivectors);

        gapp.addInstruction(new GAPPSetVector(
                    destination,
                    mvTmp,
                    createIncreasingSelectorset(vector.getSlots().size())
                ));
    }

    /**
     * Copies the mvComponents from a ParallelVector to a GAPPMultivector
     * @param mvDest The destination GAPPMultivector
     * @param vector The ParallelVector
     * @param multivectors The names of the multivectors in the vector
     */
    private void copyFromManyMultivectors(GAPPMultivector mvDest, ParallelVector vector, HashSet<String> multivectors) {
        for (String mv: multivectors) {
            Selectorset selSetDest = new Selectorset();
            Selectorset selSetSrc = new Selectorset();

            int slotNo = 0;
            for (ParallelObject object: vector.getSlots()) {
                if (ParallelObjectType.getType(object) == ParallelObjectType.mvComponent) {
                    MultivectorComponent mvC = ((MvComponent) object).getMultivectorComponent();
                    if (mvC.getName().equals(mv)) {
                        selSetSrc.add(new Selector(mvC.getBladeIndex(), (byte) 1));
                        selSetDest.add(new Selector(slotNo, object.isNegated() ? (byte) -1 : (byte) 1));
                    }
                }

                slotNo++;
            }


            gapp.addInstruction(new GAPPSetMv(mvDest, new GAPPMultivector(mv), selSetDest, selSetSrc));
        }
    }

    /**
     * Handles case 4 in creation of a GAPPVector from a ParallelVector.
     * Case 4: A mix of all two types (using assignMv,setMv and then setVector (> three operations))
     * #GAPPInstructions: multivectors.size() + 3
     *
     * @param destination The destination GAPPVector
     * @param vector The ParallelVector
     * @param multivectors The multivector names in the vector
     */
    private void case4(GAPPVector destination, ParallelVector vector, HashSet<String> multivectors) {
        GAPPMultivector mvTmp = createMv(vector.getSlots().size());
        Variableset varMvDestSet = new Variableset();
        Selectorset selSet = new Selectorset();

        int slotNo = 0;
        for (ParallelObject obj: vector.getSlots()) {
            GAPPValueHolder valueHolder = null;
            switch (ParallelObjectType.getType(obj)) {
                case constant:
                    valueHolder = new GAPPConstant(((Constant) obj).getValue());
                    selSet.add(new Selector(slotNo, obj.isNegated() ? (byte) -1 : (byte) 1));
                    varMvDestSet.add(valueHolder);
                    break;
            }
            slotNo++;
        }

        copyFromManyMultivectors(mvTmp, vector, multivectors);

        
        gapp.addInstruction(new GAPPAssignMv(
                    mvTmp,
                    selSet,
                    varMvDestSet
                ));

        gapp.addInstruction(new GAPPSetVector(
                    destination,
                    mvTmp,
                    createIncreasingSelectorset(vector.getSlots().size())
                ));
    }

    //TODO chs Think about problem with size of multivectors!
    //Is it possible to annotate every multivector with an arbitrary size (i.e > bladecount)?

    /**
     * Collects all names of multivectors in the MvComponents of a ParallelVector
     * @param vector The ParallelVector
     * @return The set of all names of multivectors
     */
    private HashSet<String> getMultivectors(ParallelVector vector) {
        HashSet<String> mvs = new HashSet<String>();
        for (ParallelObject obj: vector.getSlots())
            if (ParallelObjectType.getType(obj) == ParallelObjectType.mvComponent)
                mvs.add(((MvComponent) obj).getMultivectorComponent().getName());
        return mvs;
    }

    /**
     * Computes the number of a type in a ParallelVector
     * @param vector The ParallelVector
     * @param type The type
     * @return The number of the type in the vector
     */
    private int countTypeInParallelVector(ParallelVector vector, ParallelObjectType type) {
        int count = 0;
        for (ParallelObject obj: vector.getSlots())
            if (ParallelObjectType.getType(obj) == type)
                count++;
        return count;
    }



    // ========================= Illegal methods ===============================

    @Override
    public Object visitSum(Sum sum, Object arg) {
        throw new IllegalStateException("Sums should have been removed by DotProductCreator&Finder");
    }



}
