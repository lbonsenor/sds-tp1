import math
from typing import List
from classes.particle import Particle

class Cell:
    def __init__(self) -> None:
        self.particles = set()

    def add_particle(self, particle: Particle) -> None:
        self.particles.add(particle)

class Grid:
    def __init__(self, particles: List[Particle], long: float, r: float, m = None) -> None:
        # Si P=(0,0), esta en C=(0,0)
        # Si P=(0,r), esta en C=(0,1) Y en C=(0,0)
        if m is None:
            self.m: int = math.ceil(long/r)
        else:
            self.m: int = m

        self.cell_size = self.m/long

        self.matrix: List[List[Cell]] = [
            [Cell() for _ in range(self.m)] for _ in range(self.m)
        ]

        for particle in particles:
            x = particle.x/r
            y = particle.y/r
        pass

    def add_particle(self, particle: "Particle") -> None:
        col_min = int((particle.x - particle.r) // self.cell_size)
        col_max = int((particle.x + particle.r) // self.cell_size)
        row_min = int((particle.y - particle.r) // self.cell_size)
        row_max = int((particle.y + particle.r) // self.cell_size)

        col_min = max(0, min(self.m - 1, col_min))
        col_max = max(0, min(self.m - 1, col_max))
        row_min = max(0, min(self.m - 1, row_min))
        row_max = max(0, min(self.m - 1, row_max))

        for r in range(row_min, row_max + 1):
            for c in range(col_min, col_max + 1):
                self.matrix[r][c].add_particle(particle)