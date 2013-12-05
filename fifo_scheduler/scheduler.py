#!/usr/bin/python3
from random import randint, random
from operator import itemgetter

minlen = 1
maxlen = 15
tasks = 1000
for intensivity in [0.1]:
# for intensivity in [i/1000 for i in range(1, 100, 2)]:
  # print('INTENSIVITY = {}'.format(intensivity), end=' => ') ## DEBUG
  time = 0
  waiting_sum = 0
  tasks_left = tasks
  q = []
  task_doing = 0
  time_useful = 0
  while tasks_left:
    waiting_sum += len(q)
    for pr in q[1:]: pr[1] += 1
    gen_task = random() < intensivity and tasks_left
    if gen_task:
      # print(waiting_sum, end=' ') ## DEBUG
      tasklen = randint(minlen, maxlen)
      q.append([tasklen, 0])
    task_finished = len(q) != 0 and q[0][0] == task_doing
    if task_finished:
      # print(q[0][1]) ## TASK 3: queue size over time dependency
      q.pop(0)
      task_doing = 0
      tasks_left -= 1
    if q:
      task_doing += 1
      time_useful += 1

    time += 1
  # print('RESULT: avg waiting  = {}'.format(waiting_sum / tasks)) ## DEBUG
  # print() ## DEBUG
  # print(waiting_sum / tasks) ## TASK 1: avg waiting time
  # print(1 - time_useful / time) ## TASK 2: idle time

