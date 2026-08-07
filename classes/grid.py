import math
from typing import List
from classes.particle import Particle

class Cell:
    def __init__(self) -> None:
        self.particles = set()

    def add_particle(self, particle: Particle) -> None:
        self.particles.add(particle)

class Grid:
    def __init__(self, particles: List[Particle], long: float, range: float) -> None:
        # Si P=(0,0), esta en C=(0,0)
        # Si P=(0,r), esta en C=(0,1)
        self.m: int = math.ceil(long/range)
        self.matrix: List[List[Cell]] = [
            [Cell() for _ in range(self.m)] for _ in range(self.m)
        ]
        pass