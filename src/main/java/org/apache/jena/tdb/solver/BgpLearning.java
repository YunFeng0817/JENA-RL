package org.apache.jena.tdb.solver;

import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.Learning;
import org.deeplearning4j.rl4j.learning.sync.Transition;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense.Configuration;
import org.deeplearning4j.rl4j.observation.Observation;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.Encodable;
import org.deeplearning4j.rl4j.util.IDataManager;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;

public class BgpLearning<O extends Encodable> extends QLearningDiscreteDense<O> {

    private int lastAction;
    private double accuReward = 0;

    public BgpLearning(MDP<O, Integer, DiscreteSpace> mdp, Configuration netConf, QLConfiguration conf,
            IDataManager dataManager) {
        super(mdp, netConf, conf, dataManager);
    }

    @Override
    public void preEpoch() {
        lastAction = 0;
        accuReward = 0;
    }

    /**
     * Single step of training
     * 
     * @param obs last obs
     * @return relevant info for next step
     */
    @Override
    protected QLStepReturn<Observation> trainStep(Observation obs) {

        Integer action;
        boolean isHistoryProcessor = getHistoryProcessor() != null;

        int skipFrame = isHistoryProcessor ? getHistoryProcessor().getConf().getSkipFrame() : 1;
        int historyLength = isHistoryProcessor ? getHistoryProcessor().getConf().getHistoryLength() : 1;
        int updateStart = getConfiguration().getUpdateStart()
                + ((getConfiguration().getBatchSize() + historyLength) * skipFrame);

        Double maxQ = Double.NaN; // ignore if Nan for stats

        // if step of training, just repeat lastAction
        // if (getStepCounter() % skipFrame != 0) {
        // action = lastAction;
        // } else {

        // removed the "skip frame" code temporarily
        INDArray qs = getQNetwork().output(obs);
        int maxAction = Learning.getMaxAction(qs);
        maxQ = qs.getDouble(maxAction);

        action = getLegacyMDPWrapper().getActionSpace().randomAction();
        // }

        lastAction = action;

        StepReply<Observation> stepReply = getLegacyMDPWrapper().step(action);

        Observation nextObservation = stepReply.getObservation();

        accuReward += stepReply.getReward() * getConfiguration().getRewardFactor();

        // if it's not a skipped frame, you can do a step of training
        if (getStepCounter() % skipFrame == 0 || stepReply.isDone()) {

            Transition<Integer> trans = new Transition(obs, action, accuReward, stepReply.isDone(), nextObservation);
            getExpReplay().store(trans);

            if (getStepCounter() > updateStart) {
                DataSet targets = setTarget(getExpReplay().getBatch());
                getQNetwork().fit(targets.getFeatures(), targets.getLabels());
            }

            accuReward = 0;
        }

        return new QLStepReturn<Observation>(maxQ, getQNetwork().getLatestScore(), stepReply);
    }

}