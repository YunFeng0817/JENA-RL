package org.apache.jena.tdb.solver;

import java.util.*;

import org.deeplearning4j.rl4j.space.DiscreteSpace;

class BgpActionSpace extends DiscreteSpace {
    protected final Random rand = new Random();
    private double[] state;
    private List<Integer> tripleIndexes;

    public BgpActionSpace(int size) {
        super(size);
    }

    /**
     * @param state the state to set
     */
    public void setState(double[] state) {
        this.state = state;
    }

    /**
     * @param tripleIndexes the tripleIndexes to set
     */
    public void setTripleIndexes(List<Integer> tripleIndexes) {
        this.tripleIndexes = tripleIndexes;
    }

    @Override
    public Object encode(Integer action) {
        return action;
    }

    @Override
    public Integer noOp() {
        return 0;
    }

    @Override
    public Integer randomAction() {
        List<Integer> actionsFromCurrentState = possibleActionsFromState();
        int randIndex = rand.nextInt(actionsFromCurrentState.size());
        return actionsFromCurrentState.get(randIndex);
    }

    /**
     * get all possible action at the current state
     * 
     * @return all possible action stored in a list
     */
    List<Integer> possibleActionsFromState() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < tripleIndexes.size(); i++) {
            if (state[tripleIndexes.get(i)] == 0)
                result.add(tripleIndexes.get(i));
        }
        return result;
    }

}