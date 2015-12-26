import pstats
p = pstats.Stats('statistics')
p.strip_dirs().sort_stats('cumulative').print_stats(60)
