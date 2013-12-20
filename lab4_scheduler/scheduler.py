#!/usr/bin/python3
from random import random

def generate_random_matrix(size, p_one):
  ''' generate random `size` X `size` matrix where each cell has
      a probability `p_one` of being equal to 1, else 0 '''
  return [[int(random() < p_one) for x in range(size)] for y in range(size)]

def print_matrix(matrix):
  for line in matrix:
    print(*line)

def swap_rows(M, row1, row2):
  if row1 != row2:
    M[row1], M[row2] = M[row2], M[row1]

def swap_columns(M, col1, col2):
  if col1 != col2:
    for row in M:
      row[col1], row[col2] = row[col2], row[col1]

def reschedule(M):
  size = len(M)
  for d in range(size):
    least_ones = min(M[d:], key=lambda line: (1 not in line[d:], line[d:].count(1)))
    if 1 not in least_ones[d:]: break
    swap_rows(M, d, M[d:].index(least_ones) + d)
    swap_columns(M, d, least_ones[d:].index(1) + d)

def perfect_schedulable(M):
  reschedule(M)
  return M[-1][-1] == 1

size = 20 # MATRIX SIZE.
for p_one in (nx / 1000 for nx in range(0, 1000, 3)):
  experiments = 100
  successfull = sum(
      perfect_schedulable(generate_random_matrix(size, p_one))
      for _ in range(experiments))
  print('{}\t{}'.format(p_one, successfull / experiments))

