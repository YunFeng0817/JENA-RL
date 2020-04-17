from collections import deque
import random
import numpy as np
from tensorflow.keras import models, layers, optimizers

import gym

class DQN:
    def __init__(self):
        self.step = 0
        self.update_freq = 200
        self.replay_size = 2000
        self.replay_queue = deque(maxlen = self.replay_size)
        self.model = self.create_model()
        self.target_model = self.create_model()
    def create_model(self):
        STATE_DIM, ACTION_DIM = 2, 3
        model = models.Sequential([
            layers.Dense(100, input_dim = STATE_DIM, activation = 'relu'),
            layers.Dense(ACTION_DIM, activation = 'linear')
        ])
        model.compile(loss = 'mean_squared_error',
                     optimizer = optimizers.Adam(0.001))
        return model
    def remember(self, s, a, next_s, r):
        self.replay_queue.append((s, a, next_s, r))
        return 
    def epsilon_greedy(self, s, epsilon = 0.1):
        # 步数越大时随机性越少
        if np.random.uniform() < epsilon - self.step * 0.0002:
            return np.random.choice([0, 1, 2])
        return np.argmax(self.model.predict(np.array([s]))[0])
    def greedy(self, s):
        return np.argmax(self.model.predict(np.array([s]))[0])
    def train(self, batch_size = 64, gamma = 0.95):
        if len(self.replay_queue) < self.replay_size:
            return
        self.step += 1
        if self.step % self.update_freq == 0:
            
            self.target_model.set_weights(self.model.get_weights())
        
        replay_batch = random.sample(self.replay_queue, batch_size)
        s_batch = np.array([replay[0] for replay in replay_batch])
        next_s_batch = np.array([replay[2] for replay in replay_batch])
        
        Q = self.model.predict(s_batch)
        Q_next = self.target_model.predict(next_s_batch)
        
        for i, replay in enumerate(replay_batch):
            _, a, _, reward = replay
            Q[i][a] = reward + gamma * np.amax(Q_next[i])
        
        self.model.fit(s_batch, Q)
        


env = gym.make('MountainCar-v0')
# env = ENV(init_tables)
episodes = 20
score_list = []
agent = DQN()

for episode in range(episodes):
    state = env.reset()
    t = False
    step = 0
    while False == t and step < 200:
        a = agent.epsilon_greedy(s = state)
        s, r, t ,_ = env.step(a)  # gym中Env类step函数返回一个四元组: 下一个状态、奖励信息、是否Episode终止，以及一些额外的信息
        agent.remember(state, a, s, r)
        agent.train()
        state = s
        step += 1

#----------------------------------------------
# 将表格集合嵌入到向量
def table2vector(tables):
	# state = ...
	return state

# 仿照gym中的Env类，定义step，reset函数
# 用**包围的操作需要与数据库交互，共3处
class ENV:
	def __init__(self, init_tables):
		# 初始化环境
		init_state = table2vector(init_tables)
		self.init_state = init_state
		self.state = init_state
		return 

	def step(self, action):
		# 在环境中执行action，返回四元组（下一个状态，奖励信息，是否Episode终止，一些额外的信息）
		# **do this action at data tables**
		# ...
		# **get result tables**, compute state of result tables, and update state of env
		# ...
		return self.state, reward, False, ''
	def reset(self):
		# 重新初始化环境 
		self.state = self.init_state
		# **init tables in database**
		# ...
		return 