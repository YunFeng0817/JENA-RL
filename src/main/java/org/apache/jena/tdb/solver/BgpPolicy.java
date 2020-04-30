package org.apache.jena.tdb.solver;

import org.deeplearning4j.rl4j.network.dqn.IDQN;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.space.Box;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.List;

import org.deeplearning4j.rl4j.network.dqn.DQN;

public class BgpPolicy extends DQNPolicy<Box> {

    private BgpMDP<Box, Integer, BgpActionSpace> mdp;

    public BgpPolicy(IDQN dqn) {
        super(dqn);
    }

    public static BgpPolicy load(String path) throws IOException {
        return new BgpPolicy(DQN.load(path));
    }

    @Override
    public Integer nextAction(INDArray input) {
        INDArray output = getNeuralNet().output(input);
        List<Integer> possibleAction = mdp.getActionSpace().possibleActionsFromState();
        double[] QValues = output.data().asDouble();
        double max = Double.NEGATIVE_INFINITY;
        int action = -1;
        for (Integer index : possibleAction) {
            if (QValues[index] > max) {
                action = index;
                max = QValues[index];
            }
        }
        return action;
    }

    /**
     * @param mdp the mdp to set
     */
    public void setMdp(BgpMDP<Box, Integer, BgpActionSpace> mdp) {
        this.mdp = mdp;
    }

}