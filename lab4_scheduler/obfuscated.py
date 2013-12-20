import random
def s(M, c1, c2): if c1 != c2: for r in M: r[c1], r[c2] = r[c2], r[c1]
def q(M):
 for d in range(len(M)):
  l = min(M[d:], key=lambda line: (1 not in line[d:], line[d:].count(1)))
  if 1 not in l[d:]: break
  M[d], M[M[d:].index(l) + d] = M[M[d:].index(l) + d], M[d]; s(M, d, l[d:].index(1) + d)
 return M
for p in (nx / 1000 for nx in range(0, 1000, 3)): print('{}\t{}'.format(p, sum( q([[int(random.random() < p) for x in range(20)] for y in range(20)])[-1][-1] for _ in range(100)) / 100))