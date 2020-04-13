package org.apache.jena.tdb.solver;

import java.util.*;

import org.apache.jena.graph.Node;
import org.apache.jena.tdb.solver.stats.StatsResults;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

public class BgpMDP<O, A, AS extends ActionSpace<A>> implements MDP<O, A, AS> {

    private DiscreteSpace actionSpace;
    private ObservationSpace<O> observationSpace;
    private boolean done = false;
    private StatsResults statsResults;
    private final static String statisticsFile = "Statistics.object";

    BgpMDP() {

    }

    @Override
    public ObservationSpace<O> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public AS getActionSpace() {
        return (AS) actionSpace;
    }

    @Override
    public O reset() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public StepReply<O> step(A action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public MDP<O, A, AS> newInstance() {
        // TODO Auto-generated method stub
        return null;
    }

}