from dataclasses import dataclass
from pathlib import Path
from typing import List, Union
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class TrajectoryRecord:
    run_id: str
    time: float
    particle_id: int
    x: float
    y: float
    vx: float
    vy: float
    theta: float


class TrajectoryParser(BaseCsvParser[TrajectoryRecord]):

    def _parse_records(self) -> List[TrajectoryRecord]:
        records: List[TrajectoryRecord] = []
        for _, row in self.df.iterrows():
            record = TrajectoryRecord(
                run_id=str(row["run_id"]),
                time=float(row["time"]),
                particle_id=int(row["particle_id"]),
                x=float(row["x"]),
                y=float(row["y"]),
                vx=float(row["vx"]),
                vy=float(row["vy"]),
                theta=float(row["theta"]),
            )
            records.append(record)
        return records

    def get_particle_trajectory(self, particle_id: int) -> List[TrajectoryRecord]:
        """Filters trajectory entries for a specific particle."""
        return [r for r in self.records if r.particle_id == particle_id]