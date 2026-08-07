import math

class Particle:
    def __init__(self, x: float, y: float, r: float) -> None:
        self.x = x
        self.y = y
        self.r = r

    def __eq__(self, other):
        # Ensure the comparison is with the correct object type
        if not isinstance(other, Particle):
            return NotImplemented
        # Two objects are equal if their unique attributes are equal
        return math.isclose(self.x, other.x) and math.isclose(self.y, other.y)

    def __hash__(self):
        # Generate the hash value using the same identifying attributes
        return hash((self.x, self.y))