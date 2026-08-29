import re
from dataclasses import dataclass
from typing import List, Union
import pandas as pd
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class ParticleDataRecord:
    run_id: str
    time: float
    particle_id: int
    x: float
    y: float
    vx: float
    vy: float
    theta: float
    radius: float
    neighbors_raw: str
    neighbors_list: List[int]
    neighbor_count: int


class ParticleDataParser(BaseCsvParser[ParticleDataRecord]):

    def _parse_records(self) -> List[ParticleDataRecord]:
        records: List[ParticleDataRecord] = []
        for _, row in self.df.iterrows():
            raw_neighbors = str(row["neighbors"])
            n_list = self._parse_neighbors(raw_neighbors)
            record = ParticleDataRecord(
                run_id=str(row["run_id"]),
                time=float(row["time"]),
                particle_id=int(row["particle_id"]),
                x=float(row["x"]),
                y=float(row["y"]),
                vx=float(row["vx"]),
                vy=float(row["vy"]),
                theta=float(row["theta"]),
                radius=float(row["radius"]),
                neighbors_raw=raw_neighbors,
                neighbors_list=n_list,
                neighbor_count=len(n_list),
            )
            records.append(record)
        return records

    def get_particle_trajectory(self, particle_id: int) -> List[ParticleDataRecord]:
        """Filters entries for a specific particle across all timesteps (for animation)."""
        return [r for r in self.records if r.particle_id == particle_id]

    def get_state_at_time(self, time: float) -> List[ParticleDataRecord]:
        """Returns the full-system snapshot at a specific timestep (for cluster reconstruction)."""
        return [r for r in self.records if r.time == time]

    @staticmethod
    def _parse_neighbors(val: Union[str, float]) -> List[int]:
        """Parses bracket strings like '[0 2]' or 'none' to List[int]."""
        if pd.isna(val) or val == "none" or not str(val).strip():
            return []
        s = str(val).strip().lstrip("[").rstrip("]").strip()
        if not s:
            return []
        return [int(p) for p in re.split(r"[\s,]+", s) if p.isdigit()]